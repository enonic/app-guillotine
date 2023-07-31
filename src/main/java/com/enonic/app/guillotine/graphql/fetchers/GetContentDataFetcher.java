package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.xp.content.ContentService;

public class GetContentDataFetcher
    extends BaseContentDataFetcher
{
    public GetContentDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        super( context, contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return getContent( environment, false );
    }
}
