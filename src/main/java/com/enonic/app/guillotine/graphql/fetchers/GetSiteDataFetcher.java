package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.site.Site;

public class GetSiteDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private final GuillotineContext guillotineContext;

    private final ContentService contentService;

    public GetSiteDataFetcher( final GuillotineContext guillotineContext, final ContentService contentService )
    {
        this.guillotineContext = guillotineContext;
        this.contentService = contentService;
    }

    @Override
    public Map<String, Object> get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Map<String, Object> doGet( final DataFetchingEnvironment environment )
    {
        Site site = null;
        if ( guillotineContext.isGlobalMode() )
        {
            String siteKey = GuillotineLocalContextHelper.getSiteKey( environment );
            if ( siteKey != null && !siteKey.isEmpty() )
            {
                site = siteKey.startsWith( "/" )
                    ? contentService.findNearestSiteByPath( ContentPath.from( siteKey ) )
                    : contentService.getNearestSite( ContentId.from( siteKey ) );
            }
        }
        else
        {
            PortalRequest portalRequest = PortalRequestAccessor.get();
            site = portalRequest.getSite();
        }

        return ContentSerializer.serialize( site );
    }
}
