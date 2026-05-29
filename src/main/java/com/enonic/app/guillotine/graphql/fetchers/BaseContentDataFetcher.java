package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.site.Site;

public abstract class BaseContentDataFetcher
    implements DataFetcher<Object>
{
    private final static String SITE_PATTERN = "${site}";

    protected final GuillotineContext context;

    protected final ContentService contentService;

    public BaseContentDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        this.context = context;
        this.contentService = contentService;
    }

    protected Content getContent( DataFetchingEnvironment environment )
    {
        final String key = resolveKeyFromArgumentOrLocalContext( environment );

        if ( key == null || "/".equals( key ) )
        {
            return null;
        }

        final Content content = new GetContentCommand( contentService ).executeAndGetContent( key, environment );

        if ( content != null && "/".equals( content.getPath().toString() ) )
        {
            return null;
        }

        return content;
    }

    protected String resolveKeyFromArgumentOrLocalContext( DataFetchingEnvironment environment )
    {
        String siteKey = GuillotineLocalContextHelper.getSiteKey( environment );

        String argumentKey = environment.getArgument( "key" );

        if ( argumentKey == null )
        {
            return siteKey;
        }

        if ( argumentKey.startsWith( SITE_PATTERN ) )
        {
            final Site site = getSiteByKey( siteKey );
            final String replacement = site != null ? site.getPath().toString() : "";
            argumentKey = replacement + argumentKey.substring( SITE_PATTERN.length() );
        }

        if ( argumentKey.isEmpty() )
        {
            return null;
        }

        return argumentKey;
    }

    private Site getSiteByKey( String siteKey )
    {
        return siteKey.startsWith( "/" )
            ? contentService.findNearestSiteByPath( ContentPath.from( siteKey ) )
            : contentService.getNearestSite( ContentId.from( siteKey ) );
    }
}
