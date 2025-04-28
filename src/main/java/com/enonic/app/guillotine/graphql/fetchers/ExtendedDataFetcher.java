package com.enonic.app.guillotine.graphql.fetchers;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.graphql.helper.GraphQLTypeUnwrapper;

public class ExtendedDataFetcher
    implements DataFetcher<Object>
{
    private final Supplier<Object> contentSupplier;

    private final DataFetcher<Object> delegate;

    public ExtendedDataFetcher( final Supplier<Object> contentSupplier, final DataFetcher<Object> delegate )
    {
        this.contentSupplier = Suppliers.memoize( () -> contentSupplier );
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
