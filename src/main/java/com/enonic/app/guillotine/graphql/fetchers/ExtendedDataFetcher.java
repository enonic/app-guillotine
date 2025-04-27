package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.graphql.helper.GraphQLTypeUnwrapper;

public class ExtendedDataFetcher
    implements DataFetcher<Object>
{
    private final DataFetcher<Object> delegate;

    public ExtendedDataFetcher( final DataFetcher<Object> delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Object result = delegate.get( environment );

        final GraphQLOutputType fieldType = environment.getFieldType();
        if ( isType( fieldType, "Content" ) )
        {
            return result;
        }

        return result;
    }

    private boolean isType( final GraphQLOutputType type, final String typeName )
    {
        return GraphQLTypeUnwrapper.unwrapType( type ).getName().equals( typeName );
    }
}
