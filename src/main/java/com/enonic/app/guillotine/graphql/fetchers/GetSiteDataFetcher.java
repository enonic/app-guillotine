package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.CastHelper;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.SiteMapper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.PortalRequest;
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
        Site site = null;
        if ( guillotineContext.isGlobalMode() )
        {
            Map<String, Object> queryContext = environment.getRoot();
            if ( queryContext != null && queryContext.get( "__siteKey" ) != null )
            {
                String siteKey = queryContext.get( "__siteKey" ).toString();
                site = siteKey.startsWith( "/" )
                    ? contentService.findNearestSiteByPath( ContentPath.from( siteKey ) )
                    : contentService.getNearestSite( ContentId.from( siteKey ) );
                return site != null ? map( site ) : null;
            }
        }
        else
        {
            PortalRequest portalRequest = environment.getLocalContext();
            site = portalRequest.getSite();
        }

        return site != null ? map( site ) : null;
    }

    private Map<String, Object> map( Site site )
    {
        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new SiteMapper( site ).serialize( generator );
        Map<String, Object> result = CastHelper.cast( generator.getRoot() );
        if ( result.get( "data" ) != null )
        {
            CastHelper.castToMap( result.get( "data" ) ).remove( "siteConfig" );
        }
        return result;
    }
}
