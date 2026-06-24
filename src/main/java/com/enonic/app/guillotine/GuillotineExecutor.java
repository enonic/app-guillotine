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

@Component(service = Function.class, property = {"function=guillotine.execute"})
public class GuillotineExecutor
    implements Function<Map<String, Object>, Map<String, Object>>
{
    private static final String QUERY_KEY = "query";

    private static final String VARIABLES_KEY = "variables";

    private final GraphQLApi graphQLApi;

    private volatile GraphQLSchema schema;

    @Activate
    public GuillotineExecutor( @Reference final ServiceFacade serviceFacade, @Reference final ApplicationService applicationService,
                               @Reference final ExtensionsExtractorService extensionsExtractorService,
                               @Reference final GuillotineConfigService guillotineConfigService )
    {
        this.graphQLApi = new GraphQLApi();
        this.graphQLApi.initialize( () -> serviceFacade, () -> applicationService, () -> extensionsExtractorService,
                                    () -> guillotineConfigService );
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

    public void invalidate()
    {
        this.schema = null;
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
