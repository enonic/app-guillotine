package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.GraphQLTypeChecker;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.SchemaAwareContentExtractor;

public final class ContentAwareDataFetcher
    implements DataFetcher<Object>
{
    private final SchemaAwareContentExtractor contentExtractor;

    private final DataFetcher<?> delegate;

    public ContentAwareDataFetcher( final SchemaAwareContentExtractor contentExtractor, final DataFetcher<?> delegate )
    {
        this.contentExtractor = contentExtractor;
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Object rawValue = delegate.get( environment );

        if ( GraphQLTypeChecker.isContentType( environment.getFieldType() ) )
        {
            final List<Map<String, Object>> extractedContents = contentExtractor.extract( rawValue, environment );

            final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );

            final Map<String, Object> parentLocalContext = GuillotineLocalContextHelper.getLocalContext( environment );
            if ( parentLocalContext != null )
            {
                final Map<String, Object> newCachedContents = new HashMap<>();

                final Map<String, Object> cachedContentsFromParent =
                    (Map<String, Object>) parentLocalContext.get( Constants.CONTENTS_FIELD );
                if ( cachedContentsFromParent != null )
                {
                    newCachedContents.putAll( cachedContentsFromParent );
                }
                extractedContents.forEach( content -> newCachedContents.put( content.get( "_id" ).toString(), content ) );

                newLocalContext.put( Constants.CONTENTS_FIELD, newCachedContents );
            }

            if ( rawValue instanceof DataFetcherResult<?> dataFetcherResult )
            {
                return dataFetcherResult.transform( result -> result.localContext( Collections.unmodifiableMap( newLocalContext ) ) );
            }
            else
            {
                return DataFetcherResult.newResult().data( rawValue ).localContext(
                    Collections.unmodifiableMap( newLocalContext ) ).build();
            }
        }
        else
        {
            return rawValue;
        }
    }
}
