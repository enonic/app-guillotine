package com.enonic.app.guillotine.graphql.helper;

import java.util.List;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;

public final class SchemaAwareContentExtractor
{
    private final ContentService contentService;

    public SchemaAwareContentExtractor( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    public List<Content> extract( final Object jsApiResult, final DataFetchingEnvironment environment )
    {
        final GraphQLOutputType type = environment.getFieldType();

        final ExtractionStrategy strategy = determineStrategy( type );

        return strategy.extract( jsApiResult );
    }

    private ExtractionStrategy determineStrategy( final GraphQLType type )
    {
        if ( type instanceof GraphQLNonNull )
        {
            return determineStrategy( ( (GraphQLNonNull) type ).getWrappedType() );
        }
        else if ( type instanceof GraphQLList )
        {
            final GraphQLType wrappedType = ( (GraphQLList) type ).getWrappedType();
            if ( isConnection( wrappedType ) )
            {
                return new ConnectionExtractionStrategy( contentService );
            }
            return new ListExtractionStrategy( contentService );
        }
        else
        {
            if ( isConnection( type ) )
            {
                return new ConnectionExtractionStrategy( contentService );
            }
            else
            {
                return new SingleExtractionStrategy( contentService );
            }
        }
    }

    private boolean isConnection( final GraphQLType type )
    {
        return GraphQLTypeUnwrapper.unwrapType( type ).getName().endsWith( "Connection" );
    }
}
