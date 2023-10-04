package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
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
        return GuillotineLocalContextHelper.executeInContext( environment, () -> getContent( environment, false ) );
    }
}
