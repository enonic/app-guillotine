package com.enonic.app.guillotine.graphql;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.transformer.ExtensionsExtractorService;
import com.enonic.xp.app.ApplicationService;

@Component(immediate = true, service = SchemaProvider.class)
public class SchemaProvider
{
    private final GraphQLApi graphQLApi;

    private volatile GraphQLSchema schema;

    @Activate
    public SchemaProvider( @Reference final ServiceFacade serviceFacade, @Reference final ApplicationService applicationService,
                           @Reference final ExtensionsExtractorService extensionsExtractorService,
                           @Reference final GuillotineConfigService guillotineConfigService )
    {
        this.graphQLApi = new GraphQLApi();
        this.graphQLApi.initialize( () -> serviceFacade, () -> applicationService, () -> extensionsExtractorService,
                                    () -> guillotineConfigService );
    }

    public Object execute( final String query, final Map<String, Object> variables )
    {
        return graphQLApi.execute( getSchema(), query, variables );
    }

    public Map<String, Object> executeToSpecification( final String query, final Map<String, Object> variables )
    {
        return graphQLApi.executeToSpecification( getSchema(), query, variables );
    }

    public GraphQLSchema getSchema()
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

    public void invalidate()
    {
        this.schema = null;
    }
}
