package com.enonic.app.guillotine.mcp.tools;

import java.util.Locale;
import java.util.regex.Pattern;

import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;

/**
 * Content keys and scope paths as tools take them: a content id, or a path relative to the project's content root such as
 * {@code /articles/article-01}, as {@code _path} reads in results.
 */
final class ContentKeys
{
    static final int MAX_KEY_LENGTH = 1000;

    private static final Pattern UUID =
        Pattern.compile( "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" );

    private static final String PATH_HINT =
        "Pass a path that starts with /, such as /articles/article-01 (as _path reads in results), or an _id.";

    private ContentKeys()
    {
    }

    /**
     * Whether {@code value} is a UUID in any letter case, the form of generated content ids.
     */
    static boolean isUuid( final String value )
    {
        return value != null && UUID.matcher( value ).matches();
    }

    static boolean isPath( final String key )
    {
        return key.startsWith( "/" );
    }

    /**
     * {@code value} as a content key: a path without trailing slash, or an id, a UUID lowercased. {@code name} is the argument it
     * came from, for the message.
     */
    static String key( final Object value, final String name )
    {
        final String key = value instanceof String text ? text.strip() : null;
        if ( key == null || key.isEmpty() || key.length() > MAX_KEY_LENGTH )
        {
            throw new ToolError( "Invalid " + name + ": expected a content id or a path such as /articles/article-01.",
                                 "Call search to find content and pass its _id or _path." );
        }
        if ( isPath( key ) )
        {
            return path( key, name );
        }
        if ( key.contains( "/" ) )
        {
            throw new ToolError( "Invalid " + name + ": " + McpJson.preview( key ) + " looks like a relative path.", PATH_HINT );
        }
        final String id = isUuid( key ) ? key.toLowerCase( Locale.ROOT ) : key;
        try
        {
            ContentId.from( id );
        }
        catch ( IllegalArgumentException e )
        {
            throw new ToolError( "Invalid " + name + ": " + McpJson.preview( key ) + " is neither a content id nor a path.", PATH_HINT );
        }
        return id;
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
        if ( !isPath( path.strip() ) )
        {
            throw invalidPath( path, "path" );
        }
        return path( path.strip(), "path" );
    }

    private static String path( final String key, final String name )
    {
        final String path = key.length() > 1 && key.endsWith( "/" ) ? key.substring( 0, key.length() - 1 ) : key;
        for ( final String element : path.substring( 1 ).split( "/", -1 ) )
        {
            if ( element.isEmpty() || ".".equals( element ) || "..".equals( element ) || !element.equals( element.strip() ) ||
                element.contains( "*" ) || element.contains( "?" ) )
            {
                throw invalidPath( key, name );
            }
        }
        try
        {
            ContentPath.from( path );
        }
        catch ( IllegalArgumentException e )
        {
            throw invalidPath( key, name );
        }
        return path;
    }

    private static ToolError invalidPath( final String path, final String name )
    {
        return new ToolError( "Invalid " + name + ": " + McpJson.preview( path ) + " is not a content path.",
                              PATH_HINT + " Wildcards and . or .. elements are not allowed." );
    }
}
