package com.enonic.app.guillotine.mcp.shape;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Paging arguments. A cursor is an opaque, URL-safe encoding of the offset of the next item; it is not bound to the query that issued
 * it, so only its range is checked.
 */
public final class Cursors
{
    public static final String CURSOR_ARG = "cursor";

    public static final String COUNT_ARG = "count";

    /**
     * The deepest result a listing can page to; the search index rejects windows beyond it.
     */
    public static final int MAX_WINDOW = 10000;

    static final String RESTART_HINT = "Repeat the call without cursor to start from the first page.";

    private static final String PREFIX = "offset:";

    private Cursors()
    {
    }

    /**
     * Reads {@code count} and {@code cursor}. The input schema bounds {@code count}; {@code defaultCount} applies when it is absent.
     * A page that reaches past {@link #MAX_WINDOW} is shortened to end there.
     */
    public static Page page( final Map<String, ?> args, final int defaultCount )
    {
        final int count = args.get( COUNT_ARG ) instanceof Number number ? number.intValue() : defaultCount;
        final int offset = decode( args.get( CURSOR_ARG ) );
        if ( offset >= MAX_WINDOW )
        {
            throw new ToolError( "Invalid cursor: a listing can page through at most " + MAX_WINDOW + " items.",
                                 "Repeat the call without cursor and narrow it, for example with path or types." );
        }
        return new Page( offset, Math.min( count, MAX_WINDOW - offset ) );
    }

    public static String encode( final int offset )
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString( ( PREFIX + offset ).getBytes( StandardCharsets.UTF_8 ) );
    }

    static int decode( final Object cursor )
    {
        if ( cursor == null || "".equals( cursor ) )
        {
            return 0;
        }
        try
        {
            final String decoded = new String( Base64.getUrlDecoder().decode( cursor.toString() ), StandardCharsets.UTF_8 );
            if ( decoded.startsWith( PREFIX ) )
            {
                final int offset = Integer.parseInt( decoded.substring( PREFIX.length() ) );
                if ( offset >= 0 )
                {
                    return offset;
                }
            }
        }
        catch ( IllegalArgumentException e )
        {
            // falls through to the same error as any other foreign value
        }
        throw new ToolError( "Invalid cursor: pass nextCursor from the previous result unchanged.", RESTART_HINT );
    }
}
