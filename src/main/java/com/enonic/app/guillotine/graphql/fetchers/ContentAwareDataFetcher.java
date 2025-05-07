package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;

public final class ContentAwareDataFetcher
    implements DataFetcher<Object>
{
    private final DataFetcher<?> delegate;

    public ContentAwareDataFetcher( final DataFetcher<?> delegate )
    {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Object rawValue = delegate.get( environment );

        final ExtractionStrategy<?> strategy = resolveStrategy( environment );

        if ( strategy instanceof SingleExtractionStrategy )
        {
            final Map<String, Object> contentAsMap = (Map<String, Object>) strategy.extract( rawValue );
            return createDataFetcherResult( environment, contentAsMap );
        }
        else
        {
            final List<Map<String, Object>> contentsAsMap = (List<Map<String, Object>>) strategy.extract( rawValue );
            return contentsAsMap.stream().map( contentAsMap -> createDataFetcherResult( environment, contentAsMap ) ).toList();
        }
    }

    private DataFetcherResult<Object> createDataFetcherResult( final DataFetchingEnvironment environment,
                                                               final Map<String, Object> contentAsMap )
    {
        final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );
        newLocalContext.put( Constants.CURRENT_CONTENT_FIELD, contentAsMap );

        return DataFetcherResult.newResult().data( contentAsMap ).localContext( Collections.unmodifiableMap( newLocalContext ) ).build();
    }

    private ExtractionStrategy<?> resolveStrategy( final DataFetchingEnvironment environment )
    {
        final GraphQLOutputType type = environment.getFieldType();
        return resolveStrategy( type );
    }

    private ExtractionStrategy<?> resolveStrategy( final GraphQLType type )
    {
        if ( type instanceof GraphQLNonNull )
        {
            return resolveStrategy( ( (GraphQLNonNull) type ).getWrappedType() );
        }
        else if ( type instanceof GraphQLList )
        {
            return new ListExtractionStrategy();
        }
        else
        {
            return new SingleExtractionStrategy();
        }
    }
}
