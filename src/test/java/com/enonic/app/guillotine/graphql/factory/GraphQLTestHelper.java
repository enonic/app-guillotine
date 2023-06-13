package com.enonic.app.guillotine.graphql.factory;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphQLTestHelper
{
    public static String getNameForGraphQLTypeReference( GraphQLType type )
    {
        assertTrue( type instanceof GraphQLTypeReference );
        return ( (GraphQLTypeReference) type ).getName();
    }

    public static GraphQLType getOriginalTypeFromGraphQLList( GraphQLInputObjectType type, String fieldName )
    {
        GraphQLInputType typeOfField = type.getField( fieldName ).getType();
        assertTrue( typeOfField instanceof GraphQLList );

        return ( (GraphQLList) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLList( GraphQLObjectType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertTrue( typeOfField instanceof GraphQLList );

        return ( (GraphQLList) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLList( GraphQLInterfaceType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertTrue( typeOfField instanceof GraphQLList );

        return ( (GraphQLList) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLNonNull( GraphQLInputObjectType type, String fieldName )
    {
        GraphQLInputType typeOfField = type.getField( fieldName ).getType();
        assertTrue( typeOfField instanceof GraphQLNonNull );

        return ( (GraphQLNonNull) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLNonNull( GraphQLObjectType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertTrue( typeOfField instanceof GraphQLNonNull );

        return ( (GraphQLNonNull) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLNonNull( GraphQLInterfaceType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertTrue( typeOfField instanceof GraphQLNonNull );

        return ( (GraphQLNonNull) typeOfField ).getOriginalWrappedType();
    }
}
