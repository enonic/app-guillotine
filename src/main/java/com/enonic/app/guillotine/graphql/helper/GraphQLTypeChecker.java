package com.enonic.app.guillotine.graphql.helper;

import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

public final class GraphQLTypeChecker
{
    private GraphQLTypeChecker()
    {
    }

    public static boolean isContentType( final GraphQLType type )
    {
        final GraphQLNamedType graphQLNamedType = GraphQLTypeUnwrapper.unwrapType( type );
        return graphQLNamedType.getName().equals( "Content" ) || ( type instanceof GraphQLObjectType &&
            ( (GraphQLObjectType) type ).getInterfaces().stream().anyMatch(
                interfaceType -> interfaceType.getName().equals( "Content" ) ) );
    }

    public static boolean isHeadlessCmsType( final GraphQLType type )
    {
        return GraphQLTypeUnwrapper.unwrapType( type ).getName().equals( "HeadlessCms" );
    }

    public static boolean isConnection( final GraphQLType type )
    {
        return GraphQLTypeUnwrapper.unwrapType( type ).getName().endsWith( "Connection" );
    }
}
