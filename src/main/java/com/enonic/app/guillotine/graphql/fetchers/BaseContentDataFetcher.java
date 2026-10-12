package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

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

    private final static String SCOPE_PATTERN = "${scope}";

    protected final ContentService contentService;

    public BaseContentDataFetcher( final ContentService contentService )
    {
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
        String scopeKey = GuillotineLocalContextHelper.getScopeKey( environment );

        String argumentKey = environment.getArgument( "key" );

        if ( argumentKey == null )
        {
            return scopeKey;
        }

        // without a scopeKey the project is the root, as with scopeKey "/"
        if ( argumentKey.startsWith( SITE_PATTERN ) )
        {
            return pathOrRoot( sitePath( scopeKey ) + argumentKey.substring( SITE_PATTERN.length() ) );
        }

        if ( argumentKey.startsWith( SCOPE_PATTERN ) )
        {
            return pathOrRoot( scopePath( scopeKey ) + argumentKey.substring( SCOPE_PATTERN.length() ) );
        }

        if ( argumentKey.isEmpty() )
        {
            return "/".equals( scopeKey ) ? "/" : null;
        }

        return argumentKey;
    }

    private String sitePath( final String scopeKey )
    {
        if ( scopeKey == null )
        {
            return "";
        }

        final Site site = scopeKey.startsWith( "/" )
            ? contentService.findNearestSiteByPath( ContentPath.from( scopeKey ) )
            : contentService.getNearestSite( ContentId.from( scopeKey ) );
        return site != null ? site.getPath().toString() : "";
    }

    private String scopePath( final String scopeKey )
    {
        if ( scopeKey == null || "/".equals( scopeKey ) )
        {
            return "";
        }

        return scopeKey.startsWith( "/" )
            ? ContentPath.from( scopeKey ).toString()
            : contentService.getById( ContentId.from( scopeKey ) ).getPath().toString();
    }

    private static String pathOrRoot( final String path )
    {
        return path.isEmpty() ? "/" : path;
    }
}
