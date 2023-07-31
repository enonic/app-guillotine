package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.CursorConnectionDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.EdgesDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetFieldAsJsonDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.PageInfoDataFetcher;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class ConnectionTypeFactory
{
    private final GuillotineContext context;

    public ConnectionTypeFactory( final GuillotineContext context )
    {
        this.context = context;
    }

    public void create()
    {
        createPageInfo();
    }

    public GraphQLObjectType createEdgeType( String typeName )
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "node", new GraphQLNonNull( GraphQLTypeReference.typeRef( typeName ) ) ) );
        fields.add( outputField( "cursor", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( typeName + "Edge" ), typeName + "Edge.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "cursor", new CursorConnectionDataFetcher( "cursor" ) );

        return objectType;
    }

    public GraphQLObjectType createConnectionType( String typeName )
    {
        return createConnectionType( typeName, createEdgeType( typeName ), null );
    }

    public GraphQLObjectType createConnectionType( String typeName, GraphQLObjectType edgeType,
                                                   List<GraphQLFieldDefinition> additionalFields )
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "totalCount", new GraphQLNonNull( Scalars.GraphQLInt ) ) );
        fields.add( outputField( "edges", new GraphQLList( edgeType ) ) );
        fields.add( outputField( "pageInfo", GraphQLTypeReference.typeRef( "PageInfo" ) ) );
        if ( additionalFields != null )
        {
            fields.addAll( additionalFields );
        }

        GraphQLObjectType objectType = newObject( context.uniqueName( typeName + "Connection" ), typeName + "Connection.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "totalCount", new GetFieldAsJsonDataFetcher( "total" ) );
        context.registerDataFetcher( objectType.getName(), "edges", new EdgesDataFetcher() );
        context.registerDataFetcher( objectType.getName(), "pageInfo", new PageInfoDataFetcher() );

        return objectType;
    }

    private void createPageInfo()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "startCursor", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( outputField( "endCursor", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( outputField( "hasNext", new GraphQLNonNull( Scalars.GraphQLBoolean ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( "PageInfo" ), "PageInfo", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "startCursor", new CursorConnectionDataFetcher( "startCursor" ) );
        context.registerDataFetcher( objectType.getName(), "endCursor", new CursorConnectionDataFetcher( "endCursor" ) );
        context.registerDataFetcher( objectType.getName(), "hasNext", new GetFieldAsJsonDataFetcher( "hasNext" ) );
    }

}
