package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;

public class GetContentDataFetcher
    extends BaseContentDataFetcher
{
    public GetContentDataFetcher( final ContentService contentService )
    {
        super( contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> {
            final Content content = getContent( environment, false );

            if ( content == null )
            {
                return null;
            }

            final Map<String, Object> data = GuillotineSerializer.serialize( content );

            if ( content.getAttachments().isEmpty() )
            {
                return data;
            }
            else
            {
                final Map<String, Object> parentLocalContext = environment.getLocalContext();

                final ConcurrentHashMap<String, Content> contentsWithAttachments = new ConcurrentHashMap<>();
                contentsWithAttachments.put( content.getId().toString(), content );

                final Map<String, Object> newLocalContext = new HashMap<>();
                if ( parentLocalContext != null )
                {
                    newLocalContext.putAll( parentLocalContext );
                }
                newLocalContext.put( Constants.CONTENTS_FIELD, contentsWithAttachments );

                return DataFetcherResult.newResult().localContext( Collections.unmodifiableMap( newLocalContext ) ).data( data ).build();
            }
        } );
    }
}
