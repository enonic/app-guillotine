package com.enonic.app.guillotine.headless;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = HeadlessService.class)
public class GraphQLServiceImpl
    implements HeadlessService
{
    private final GraphQLSchemaProvider schemaProvider;

    private final HeadlessGraphQLHelper graphQLHelper = new HeadlessGraphQLHelper();

    @Activate
    public GraphQLServiceImpl( final @Reference GraphQLSchemaProvider schemaProvider )
    {
        this.schemaProvider = schemaProvider;
    }

    @Override
    public HeadlessSchema createSchema( final HeadlessSchemaParams params )
    {
        return graphQLHelper.createSchema( params, schemaProvider );
    }

    @Override
    public HeadlessObject createOutputObject( final HeadlessObjectParams params )
    {
        return graphQLHelper.createOutputObject( params );
    }
}
