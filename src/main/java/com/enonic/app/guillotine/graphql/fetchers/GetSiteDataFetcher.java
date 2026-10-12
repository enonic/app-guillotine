package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.site.Site;

public class GetSiteDataFetcher
    implements DataFetcher<Object>
{
    private final ContentService contentService;

    public GetSiteDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        String scopeKey = GuillotineLocalContextHelper.getScopeKey( environment );
        if ( scopeKey != null && !scopeKey.isEmpty() )
        {
            Site site = scopeKey.startsWith( "/" )
                ? contentService.findNearestSiteByPath( ContentPath.from( scopeKey ) )
                : contentService.getNearestSite( ContentId.from( scopeKey ) );

            return GuillotineSerializer.serialize( site );
        }

        return null;
    }
}
