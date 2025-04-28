package com.enonic.app.guillotine.graphql.helper;

import java.util.List;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import com.enonic.xp.content.Content;

public final class SchemaAwareContentExtractor
{
    public List<Content> extract( final Object jsApiResult, final DataFetchingEnvironment environment )
    {
        final GraphQLOutputType type = environment.getFieldType();

        final ExtractionStrategy strategy = resolveStrategy( type );

        return strategy.extract( jsApiResult );
    }

    private ExtractionStrategy resolveStrategy( final GraphQLType type )
    {
        if ( type instanceof GraphQLNonNull )
        {
            return resolveStrategy( ( (GraphQLNonNull) type ).getWrappedType() );
        }
        else if ( type instanceof GraphQLList )
        {
            final GraphQLType wrappedType = ( (GraphQLList) type ).getWrappedType();
            if ( GraphQLTypeChecker.isConnection( wrappedType ) )
            {
                return new ConnectionExtractionStrategy();
            }
            return new ListExtractionStrategy();
        }
        else
        {
            if ( GraphQLTypeChecker.isConnection( type ) )
            {
                return new ConnectionExtractionStrategy();
            }
            else
            {
                return new SingleExtractionStrategy();
            }
        }
    }
}
