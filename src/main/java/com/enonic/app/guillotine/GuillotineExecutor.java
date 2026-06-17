package com.enonic.app.guillotine;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.GraphQLApi;
import com.enonic.app.guillotine.graphql.transformer.ExtensionsExtractorService;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.portal.PortalRequest;

/**
 * Exposes Guillotine GraphQL execution as an OSGi service so other applications can run queries without going over the
 * HTTP wire. The contract is deliberately a plain {@link Function} typed with JDK-only types (no graphql-java leaks):
 * <pre>
 *   input  = { "query": String, "variables": Map&lt;String,Object&gt; }
 *   output = { "data": ..., "errors": ... }   // GraphQL-spec response, JSON-shaped maps/lists/scalars
 * </pre>
 * Because the service interface is {@code java.util.function.Function} (from {@code java.base}), any bundle can consume
 * it without importing a guillotine-specific package. Consumers select it with the service property
 * {@code (function=guillotine.execute)}.
 * <p>
 * The query executes in the caller's current XP context, so the caller targets project/branch/principals by wrapping the
 * call in {@code contextLib.run({...}, ...)} — exactly as the HTTP endpoint does.
 */
@Component(service = Function.class, property = {"function=guillotine.execute"})
public class GuillotineExecutor
    implements Function<Map<String, Object>, Map<String, Object>>
{
    private static final String QUERY_KEY = "query";

    private static final String VARIABLES_KEY = "variables";

    private final GraphQLApi graphQLApi;

    // The site-header lookup in GuillotineDataFetcher dereferences the PortalRequest when no siteKey arg is supplied.
    // Outside a portal request there is none, so we feed an empty request (empty headers -> no site resolution).
    private final PortalRequest emptyPortalRequest = new PortalRequest();

    private volatile GraphQLSchema schema;

    @Activate
    public GuillotineExecutor( @Reference final ServiceFacade serviceFacade, @Reference final ApplicationService applicationService,
                               @Reference final ExtensionsExtractorService extensionsExtractorService,
                               @Reference final GuillotineConfigService guillotineConfigService )
    {
        this.graphQLApi = new GraphQLApi();
        this.graphQLApi.initialize( () -> serviceFacade, () -> applicationService, () -> extensionsExtractorService,
                                    () -> emptyPortalRequest, () -> guillotineConfigService );
    }

    @Override
    public Map<String, Object> apply( final Map<String, Object> input )
    {
        final Object query = input.get( QUERY_KEY );
        if ( !( query instanceof String ) )
        {
            throw new IllegalArgumentException( "'query' is required and must be a String" );
        }

        final Object variables = input.get( VARIABLES_KEY );
        final Map<String, Object> variablesMap = variables instanceof Map ? castToMap( variables ) : Map.of();

        return graphQLApi.executeToSpecification( getSchema(), (String) query, variablesMap );
    }

    /**
     * Drops the cached schema. The schema is shared and context-independent; it should be invalidated on application
     * lifecycle changes (the same trigger the HTTP path uses via the {@code com.enonic.app.guillotine-schemaChanged} event).
     */
    public void invalidate()
    {
        this.schema = null;
        this.graphQLApi.invalidateCache();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMap( final Object value )
    {
        return (Map<String, Object>) value;
    }

    private GraphQLSchema getSchema()
    {
        GraphQLSchema result = this.schema;
        if ( result == null )
        {
            synchronized ( this )
            {
                result = this.schema;
                if ( result == null )
                {
                    result = this.graphQLApi.createSchema();
                    this.schema = result;
                }
            }
        }
        return result;
    }
}
