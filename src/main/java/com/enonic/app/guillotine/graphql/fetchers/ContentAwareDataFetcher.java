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
import com.enonic.xp.content.Content;

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
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Object rawValue = delegate.get( environment );

        if ( GraphQLTypeChecker.isContentType( environment.getFieldType() ) )
        {
            final List<Content> contents = contentExtractor.extract( rawValue, environment );

            final Map<String, Content> contentsWithAttachments = new HashMap<>();
            contents.forEach( content -> {
                if ( !content.getAttachments().isEmpty() )
                {
                    contentsWithAttachments.put( content.getId().toString(), content );
                }
            } );

            final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );
            if ( !contentsWithAttachments.isEmpty() )
            {
                newLocalContext.put( Constants.CONTENTS_WITH_ATTACHMENTS_FIELD, contentsWithAttachments );
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
