package com.enonic.app.guillotine.headless;

import java.util.Map;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;

interface HeadlessMapper
{
    static HeadlessDataFetchingEnvironment map( DataFetchingEnvironment source )
    {
        if ( source == null )
        {
            return null;
        }

        HeadlessDataFetchingEnvironment result = new HeadlessDataFetchingEnvironment();

        result.setSource( source.getSource() );
        result.setArguments( source.getArguments() );
        result.setContext( source.getRoot() );

        return result;
    }

    static GraphQLFieldDefinition map( HeadlessFieldDefinition source, String parentTypeName,
                                       GraphQLCodeRegistry.Builder codeRegistryBuilder )
    {
        GraphQLFieldDefinition.Builder fieldBuilder = GraphQLFieldDefinition.newFieldDefinition();

        fieldBuilder.name( source.getName() );
        fieldBuilder.description( source.getDescription() );
        fieldBuilder.type( Scalars.GraphQLString ); // TODO

        setArguments( fieldBuilder, source.getArguments() );

        codeRegistryBuilder.dataFetcher( FieldCoordinates.coordinates( parentTypeName, source.getName() ),
                                         (DataFetcher<Object>) environment -> {
                                             if ( source.getResolveFunction() != null )
                                             {
                                                 source.getResolveFunction().apply( map( environment ) );
                                             }
                                             return null;
                                         } );

        return fieldBuilder.build();
    }

    static HeadlessObject map( GraphQLObjectType source )
    {
        if ( source == null )
        {
            return null;
        }

        HeadlessObject result = new HeadlessObject();

        result.setName( source.getName() );
        result.setDescription( source.getDescription() );

        return result;
    }

    static HeadlessSchema map( String identifier )
    {
        final HeadlessSchema result = new HeadlessSchema();
        result.setIdentifier( identifier );
        return result;
    }

    private static void setArguments( GraphQLFieldDefinition.Builder fieldBuilder, Map<String, Object> arguments )
    {
        arguments.entrySet().stream().map( argument -> GraphQLArgument.newArgument().name( argument.getKey() ).type(
            (GraphQLInputType) argument.getValue() ).build() ).forEach( fieldBuilder::argument );
    }
}
