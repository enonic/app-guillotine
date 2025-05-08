package com.enonic.app.guillotine.graphql.helper;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;

public final class GraphQLTypeUnwrapper
{
    private GraphQLTypeUnwrapper()
    {
        // do nothing
    }

    public static GraphQLNamedType unwrapType( final GraphQLType type )
    {
        if ( type instanceof GraphQLNonNull )
        {
            return unwrapType( ( (GraphQLNonNull) type ).getWrappedType() );
        }
        if ( type instanceof GraphQLList )
        {
            return unwrapType( ( (GraphQLList) type ).getWrappedType() );
        }
        if ( type instanceof GraphQLNamedType )
        {
            return (GraphQLNamedType) type;
        }
        throw new IllegalArgumentException( "Unsupported GraphQLType: " + type );
    }
}
