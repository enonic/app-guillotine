package com.enonic.app.guillotine.mcp.shape;

import java.util.Locale;

import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;

/**
 * Validates the content keys and scope paths tools take as arguments, so that the caller gets a fix hint instead of a parser error.
 */
public final class ContentKeys
{
    static final int MAX_KEY_LENGTH = 1000;

    private static final String PATH_HINT =
        "Paths start with / relative to the project root, e.g. /articles/article-01; or pass the _id returned by search.";

    private ContentKeys()
    {
    }

    /**
     * Normalizes a content key: trims it, lowercases a UUID and checks that a path is absolute, has no empty, relative or padded
     * elements and only valid content names, and that an id is a valid content id.
     */
    public static String key( final Object value )
    {
        final String key = value instanceof String text ? text.strip() : null;
        if ( key == null || key.isEmpty() || key.length() > MAX_KEY_LENGTH )
        {
            throw new ToolError( "Invalid key: expected a content id or a path such as /articles/my-article.",
                                 "Call search to find content and pass its _id or _path." );
        }
        if ( key.startsWith( "/" ) )
        {
            final String path = key.length() > 1 && key.endsWith( "/" ) ? key.substring( 0, key.length() - 1 ) : key;
            final String[] elements = path.substring( 1 ).split( "/", -1 );
            for ( final String element : elements )
            {
                if ( element.isEmpty() || ".".equals( element ) || "..".equals( element ) || !element.equals( element.strip() ) )
                {
                    throw invalidKeyPath( key );
                }
            }
            try
            {
                ContentPath.from( path );
            }
            catch ( IllegalArgumentException e )
            {
                throw invalidKeyPath( key );
            }
            return path;
        }
        if ( key.contains( "/" ) )
        {
            throw new ToolError( "Invalid key: " + McpJson.preview( key ) + " looks like a relative path.", PATH_HINT );
        }
        final String id = McpArgs.isUuid( key ) ? key.toLowerCase( Locale.ROOT ) : key;
        try
        {
            ContentId.from( id );
        }
        catch ( IllegalArgumentException e )
        {
            throw new ToolError( "Invalid key: " + McpJson.preview( key ) + " is neither a content id nor a path.", PATH_HINT );
        }
        return id;
    }

    /**
     * A scope path without its trailing slash, or {@code null} for the whole project. Paths follow the rule of {@link #key} paths:
     * absolute, without empty, relative or padded elements, with valid content names and without wildcards.
     */
    public static String scopePath( final String path )
    {
        if ( path == null || path.isBlank() || "/".equals( path ) )
        {
            return null;
        }
        if ( !path.startsWith( "/" ) || path.contains( "*" ) || path.contains( "?" ) )
        {
            throw invalidScopePath( path );
        }
        try
        {
            return key( path );
        }
        catch ( ToolError e )
        {
            throw invalidScopePath( path );
        }
    }

    private static ToolError invalidKeyPath( final String key )
    {
        return new ToolError( "Invalid key: " + McpJson.preview( key ) + " is not a valid content path.", PATH_HINT );
    }

    private static ToolError invalidScopePath( final String path )
    {
        return new ToolError( "Invalid path: " + McpJson.preview( path ) + ".",
                              "Pass a content path that starts with /, such as /landings (as returned in _path), without . or .. " +
                                  "elements." );
    }
}
