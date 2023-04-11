package com.enonic.app.guillotine.headless;

import java.util.UUID;

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

class HeadlessGraphQLHelper
{
    private final GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();

    HeadlessSchema createSchema( final HeadlessSchemaParams params, final GraphQLSchemaProvider schemaProvider )
    {
        GraphQLSchema.Builder graphQLSchema = GraphQLSchema.newSchema();

        graphQLSchema.query( createGraphQLObjectType( params.getQuery() ) );

        graphQLSchema.codeRegistry( codeRegistryBuilder.build() );

        GraphQLSchema schema = graphQLSchema.build();

        String identifier = UUID.randomUUID().toString();

        schemaProvider.registerSchema( identifier, schema );

        return HeadlessMapper.map( identifier );
    }

    HeadlessObject createOutputObject( final HeadlessObjectParams params )
    {
        return HeadlessMapper.map( createGraphQLObjectType( params ) );
    }

    private GraphQLObjectType createGraphQLObjectType( final HeadlessObjectParams params )
    {
        GraphQLObjectType.Builder objectType =
            GraphQLObjectType.newObject().name( params.getTypeName() ).description( params.getDescription() );

        params.getFields().forEach( field -> objectType.field( HeadlessMapper.map( field, params.getTypeName(), codeRegistryBuilder ) ) );

        return objectType.build();
    }
}
