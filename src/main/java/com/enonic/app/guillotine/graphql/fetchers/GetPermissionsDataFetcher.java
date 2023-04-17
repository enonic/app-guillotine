package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineContext;
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
        Map<String, Object> contentAsMap = getContent( environment, false );

        if ( contentAsMap != null )
        {
            return contentAsMap.get( "permissions" );
        }
        return null;
    }
}
