package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.site.Site;

public class GetSiteDataFetcher
    implements DataFetcher<Object>
{
    private final ContentService contentService;

    private final PortalUrlService portalUrlService;

    public GetSiteDataFetcher( final ContentService contentService, final PortalUrlService portalUrlService )
    {
        this.contentService = contentService;
        this.portalUrlService = portalUrlService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        final String scopeKey = GuillotineLocalContextHelper.getScopeKey( environment );
        if ( scopeKey == null || "/".equals( scopeKey ) )
        {
            return null;
        }

        final ContentPath sitePath = GuillotineLocalContextHelper.getPortalScope( environment, portalUrlService ).getSitePath();
        if ( sitePath.isRoot() )
        {
            return null;
        }

        try
        {
            final Content content = contentService.getByPath( sitePath );
            return content instanceof Site site ? GuillotineSerializer.serialize( site ) : null;
        }
        catch ( ContentNotFoundException e )
        {
            return null;
        }
    }
}
