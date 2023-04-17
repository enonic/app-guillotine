package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.helper.SecurityHelper;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.site.Site;

public abstract class BaseContentDataFetcher
    implements DataFetcher<Object>
{
    protected final GuillotineContext context;

    protected final ContentService contentService;

    public BaseContentDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        this.context = context;
        this.contentService = contentService;
    }

    protected Map<String, Object> getContent( DataFetchingEnvironment environment, boolean returnRootContent )
    {
        PortalRequest portalRequest = PortalRequestAccessor.get();

        Map<String, Object> queryContext = environment.getRoot();

        String argumentKey = environment.getArgument( "key" );

        if ( argumentKey != null )
        {
            String key = argumentKey;

            Site site = portalRequest.getSite();

            if ( context.isGlobalMode() && queryContext != null && queryContext.get( "__siteKey" ) != null )
            {
                site = getSiteByKey( queryContext.get( "__siteKey" ).toString() );
            }

            if ( site != null )
            {
                key = argumentKey.replaceAll( "\\$\\{site\\}", site.getPath().toString() );
            }

            return getContentByKey( key, returnRootContent );
        }
        else
        {
            if ( context.isGlobalMode() )
            {
                if ( queryContext != null && queryContext.get( "__siteKey" ) != null )
                {
                    return getContentByKey( queryContext.get( "__siteKey" ).toString(), returnRootContent );
                }
                if ( returnRootContent )
                {
                    return ContextBuilder.from( ContextAccessor.current() ).build().callWith(
                        () -> new GetContentCommand( contentService ).execute( "/" ) );
                }
            }
            return ContentSerializer.serialize( portalRequest.getContent() );
        }
    }

    private Site getSiteByKey( String siteKey )
    {
        Site site = siteKey.startsWith( "/" )
            ? contentService.findNearestSiteByPath( ContentPath.from( siteKey ) )
            : contentService.getNearestSite( ContentId.from( siteKey ) );

        if ( site == null )
        {
            throw new IllegalArgumentException( "Site not found." );
        }

        return site;
    }

    private Map<String, Object> getContentByKey( String key, boolean returnRootContent )
    {
        Map<String, Object> contentAsMap = new GetContentCommand( contentService ).execute( key );

        if ( contentAsMap != null && "/".equals( contentAsMap.get( "_path" ) ) && !returnRootContent )
        {
            return null;
        }

        return SecurityHelper.filterForbiddenContent( contentAsMap, context );
    }
}
