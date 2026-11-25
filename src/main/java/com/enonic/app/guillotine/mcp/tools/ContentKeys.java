package com.enonic.app.guillotine.mcp.tools;

import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.ContentPath;

/**
 * Scope paths as tools take them: a path relative to the project's content root such as {@code /articles}, as {@code _path} reads in
 * results.
 */
final class ContentKeys
{
    private static final String PATH_HINT =
        "Pass a path that starts with /, such as /articles/article-01 (as _path reads in results).";

    private ContentKeys()
    {
    }

    /**
     * The scope path of {@code path}, without trailing slash, or {@code null} for the whole project ({@code /} or blank).
     */
    static String scopePath( final String path )
    {
        if ( path == null || path.isBlank() || "/".equals( path.strip() ) )
        {
            return null;
        }
        if ( !path.strip().startsWith( "/" ) )
        {
            throw invalidPath( path );
        }
        return normalized( path.strip() );
    }

    private static String normalized( final String key )
    {
        final String path = key.length() > 1 && key.endsWith( "/" ) ? key.substring( 0, key.length() - 1 ) : key;
        for ( final String element : path.substring( 1 ).split( "/", -1 ) )
        {
            if ( element.isEmpty() || ".".equals( element ) || "..".equals( element ) || !element.equals( element.strip() ) ||
                element.contains( "*" ) || element.contains( "?" ) )
            {
                throw invalidPath( key );
            }
        }
        try
        {
            ContentPath.from( path );
        }
        catch ( IllegalArgumentException e )
        {
            throw invalidPath( key );
        }
        return path;
    }

    private static ToolError invalidPath( final String path )
    {
        return new ToolError( "Invalid path: " + McpJson.preview( path ) + " is not a content path.",
                              PATH_HINT + " Wildcards and . or .. elements are not allowed." );
    }
}
