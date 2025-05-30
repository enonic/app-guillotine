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
        String siteKey = GuillotineLocalContextHelper.getSiteKey( environment );
        if ( siteKey != null && !siteKey.isEmpty() )
        {
            Site site = siteKey.startsWith( "/" )
                ? contentService.findNearestSiteByPath( ContentPath.from( siteKey ) )
                : contentService.getNearestSite( ContentId.from( siteKey ) );

            return GuillotineSerializer.serialize( site );
        }

        return null;
    }
}
