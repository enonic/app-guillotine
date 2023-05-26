package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;

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
        fields.add( outputField( "node", new GraphQLNonNull( new GraphQLTypeReference( typeName ) ) ) );
        fields.add( outputField( "cursor", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( typeName + "Edge" ), typeName + "Edge.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "cursor", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return ConnectionHelper.encodeCursor( sourceAsMap.get( "cursor" ).toString() );
        } );

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
        fields.add( outputField( "pageInfo", context.getOutputType( "PageInfo" ) ) );
        if ( additionalFields != null )
        {
            fields.addAll( additionalFields );
        }

        GraphQLObjectType objectType = newObject( context.uniqueName( typeName + "Connection" ), typeName + "Connection.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "totalCount", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "total" );
        } );
        context.registerDataFetcher( objectType.getName(), "edges", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();

            List<Map<String, Object>> hits = CastHelper.cast( sourceAsMap.get( "hits" ) );

            List<Map<String, Object>> edges = new ArrayList<>();

            for ( int i = 0; i < hits.size(); i++ )
            {
                Map<String, Object> edge = new HashMap<>();

                edge.put( "node", hits.get( i ) );
                edge.put( "cursor", (int) sourceAsMap.get( "start" ) + i );

                edges.add( edge );
            }

            return edges;
        } );
        context.registerDataFetcher( objectType.getName(), "pageInfo", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();

            int count = ( (List<?>) sourceAsMap.get( "hits" ) ).size();
            int start = (int) sourceAsMap.get( "start" );
            long total = (long) sourceAsMap.get( "total" );

            Map<String, Object> result = new HashMap<>();

            result.put( "startCursor", start );
            result.put( "endCursor", start + ( count == 0 ? 0 : count - 1 ) );
            result.put( "hasNext", start + count < total );

            return result;
        } );

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

        context.registerDataFetcher( objectType.getName(), "startCursor", environment -> {
            Map<String, Object> source = environment.getSource();
            return ConnectionHelper.encodeCursor( source.get( "startCursor" ).toString() );
        } );
        context.registerDataFetcher( objectType.getName(), "endCursor", environment -> {
            Map<String, Object> source = environment.getSource();
            return ConnectionHelper.encodeCursor( source.get( "endCursor" ).toString() );
        } );
        context.registerDataFetcher( objectType.getName(), "hasNext", environment -> {
            Map<String, Object> source = environment.getSource();
            return source.get( "hasNext" );
        } );
    }

}
