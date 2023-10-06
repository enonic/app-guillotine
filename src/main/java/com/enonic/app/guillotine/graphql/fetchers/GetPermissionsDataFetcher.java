package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentService;

public class GetPermissionsDataFetcher
    extends BaseContentDataFetcher
{
    public GetPermissionsDataFetcher( final ContentService contentService )
    {
        super( contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        Map<String, Object> contentAsMap = getContent( environment, false );

        if ( contentAsMap != null )
        {
            return contentAsMap.get( "permissions" );
        }
        return null;
    }
}
