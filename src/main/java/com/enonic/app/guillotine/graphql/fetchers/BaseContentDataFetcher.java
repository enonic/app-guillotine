package com.enonic.app.guillotine.graphql.fetchers;

import java.util.function.Function;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.PortalScope;
import com.enonic.xp.portal.url.PortalUrlService;

public abstract class BaseContentDataFetcher
    implements DataFetcher<Object>
{
    private final static String SITE_PATTERN = "${site}";

    private final static String SCOPE_PATTERN = "${scope}";

    protected final ContentService contentService;

    private final PortalUrlService portalUrlService;

    public BaseContentDataFetcher( final ContentService contentService, final PortalUrlService portalUrlService )
    {
        this.contentService = contentService;
        this.portalUrlService = portalUrlService;
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
            final String sitePath = scopePath( environment, scopeKey, PortalScope::getSitePath );
            return pathOrRoot( sitePath + argumentKey.substring( SITE_PATTERN.length() ) );
        }

        if ( argumentKey.startsWith( SCOPE_PATTERN ) )
        {
            final String scopePath = scopePath( environment, scopeKey, PortalScope::getPath );
            return pathOrRoot( scopePath + argumentKey.substring( SCOPE_PATTERN.length() ) );
        }

        if ( argumentKey.isEmpty() )
        {
            return "/".equals( scopeKey ) ? "/" : null;
        }

        return argumentKey;
    }

    private String scopePath( final DataFetchingEnvironment environment, final String scopeKey,
                              final Function<PortalScope, ContentPath> path )
    {
        if ( scopeKey == null || "/".equals( scopeKey ) )
        {
            return "";
        }

        final ContentPath contentPath = path.apply( GuillotineLocalContextHelper.getPortalScope( environment, portalUrlService ) );
        return contentPath.isRoot() ? "" : contentPath.toString();
    }

    private static String pathOrRoot( final String path )
    {
        return path.isEmpty() ? "/" : path;
    }
}
