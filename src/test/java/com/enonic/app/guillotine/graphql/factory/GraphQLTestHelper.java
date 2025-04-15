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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class GraphQLTestHelper
{
    public static String getNameForGraphQLTypeReference( GraphQLType type )
    {
        assertInstanceOf( GraphQLTypeReference.class, type );
        return ( (GraphQLTypeReference) type ).getName();
    }

    public static GraphQLType getOriginalTypeFromGraphQLList( GraphQLInputObjectType type, String fieldName )
    {
        GraphQLInputType typeOfField = type.getField( fieldName ).getType();
        assertInstanceOf( GraphQLList.class, typeOfField );

        return ( (GraphQLList) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLList( GraphQLObjectType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertInstanceOf( GraphQLList.class, typeOfField );

        return ( (GraphQLList) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLList( GraphQLInterfaceType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertInstanceOf( GraphQLList.class, typeOfField );

        return ( (GraphQLList) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLNonNull( GraphQLInputObjectType type, String fieldName )
    {
        GraphQLInputType typeOfField = type.getField( fieldName ).getType();
        assertInstanceOf( GraphQLNonNull.class, typeOfField );

        return ( (GraphQLNonNull) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLNonNull( GraphQLObjectType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertInstanceOf( GraphQLNonNull.class, typeOfField );

        return ( (GraphQLNonNull) typeOfField ).getOriginalWrappedType();
    }

    public static GraphQLType getOriginalTypeFromGraphQLNonNull( GraphQLInterfaceType type, String fieldName )
    {
        GraphQLOutputType typeOfField = type.getField( fieldName ).getType();
        assertInstanceOf( GraphQLNonNull.class, typeOfField );

        return ( (GraphQLNonNull) typeOfField ).getOriginalWrappedType();
    }
}
