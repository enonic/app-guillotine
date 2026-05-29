package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;

public class GetPermissionsDataFetcher
    extends BaseContentDataFetcher
{
    public GetPermissionsDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        super( context, contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return doGet( environment );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        Content content = getContent( environment );
        return GuillotineSerializer.serializePermissions( content );
    }
}
