package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.regex.Pattern;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.SecurityHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.site.Site;

public abstract class BaseContentDataFetcher
    implements DataFetcher<Object>
{
    private final static Pattern SITE_KEY_PATTERN = Pattern.compile( "\\$\\{site\\}" );

    protected final ContentService contentService;

    public BaseContentDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    protected Map<String, Object> getContent( DataFetchingEnvironment environment, boolean returnRootContent )
    {
        String siteKey = GuillotineLocalContextHelper.getSiteKey( environment );

        String argumentKey = environment.getArgument( "key" );

        if ( argumentKey != null )
        {
            String key = argumentKey;
            if ( !siteKey.isEmpty() )
            {
                Site site = getSiteByKey( siteKey );
                if ( site != null )
                {
                    key = argumentKey.replaceAll( SITE_KEY_PATTERN.pattern(), site.getPath().toString() );
                }
            }
            return getContentByKey( key, returnRootContent, environment );
        }
        else
        {
            if ( !siteKey.isEmpty() )
            {
                return getContentByKey( siteKey, returnRootContent, environment );
            }
            if ( returnRootContent )
            {
                return ContextBuilder.from( ContextAccessor.current() ).build().callWith(
                    () -> new GetContentCommand( contentService ).execute( "/", environment ) );
            }
        }
        return null;
    }

    private Site getSiteByKey( String siteKey )
    {
        return siteKey.startsWith( "/" )
            ? contentService.findNearestSiteByPath( ContentPath.from( siteKey ) )
            : contentService.getNearestSite( ContentId.from( siteKey ) );
    }

    private Map<String, Object> getContentByKey( String key, boolean returnRootContent, DataFetchingEnvironment environment )
    {
        Map<String, Object> contentAsMap = new GetContentCommand( contentService ).execute( key, environment );

        if ( contentAsMap != null && "/".equals( contentAsMap.get( "_path" ) ) && !returnRootContent )
        {
            return null;
        }

        return SecurityHelper.filterForbiddenContent( contentAsMap );
    }
}
