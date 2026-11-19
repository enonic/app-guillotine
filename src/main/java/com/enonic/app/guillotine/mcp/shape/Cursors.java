package com.enonic.app.guillotine.mcp.shape;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Cursors
{
    public static final String CURSOR_ARG = "cursor";

    public static final String COUNT_ARG = "count";

    public static final int DEFAULT_COUNT = 10;

    public static final int MAX_COUNT = 50;

    /**
     * The deepest result a listing can page to; the search index rejects windows beyond it.
     */
    public static final int MAX_WINDOW = 10000;

    private static final String RESTART_HINT = "Repeat the call without cursor to start from the first page.";

    private Cursors()
    {
    }

    /**
     * Reads {@code count} and {@code cursor} from the arguments of {@code tool}. The tool and all other arguments, with
     * {@code target} in place of the project and branch passed, form the query the cursor is bound to. Rejects a page that reaches
     * past {@link #MAX_WINDOW}. {@code count} may be 0, which {@code zeroCountReturns} describes for the caller, e.g. "only the total".
     */
    public static Page page( final String tool, final Map<String, Object> args, final ProjectBranch target,
                             final String zeroCountReturns )
    {
        return page( tool, args, target, DEFAULT_COUNT, 0, zeroCountReturns );
    }

    /**
     * Same as {@link #page(String, Map, ProjectBranch, String)} with {@code defaultCount} items per page when {@code count} is not
     * passed, and at least {@code minCount}, which must be positive, when it is.
     */
    public static Page page( final String tool, final Map<String, Object> args, final ProjectBranch target, final int defaultCount,
                             final int minCount )
    {
        if ( minCount < 1 )
        {
            throw new IllegalArgumentException( "minCount must be positive; pass what count 0 returns instead" );
        }
        return page( tool, args, target, defaultCount, minCount, null );
    }

    private static Page page( final String tool, final Map<String, Object> args, final ProjectBranch target, final int defaultCount,
                              final int minCount, final String zeroCountReturns )
    {
        final Map<String, Object> query = target.putInto( new LinkedHashMap<>( args ) );
        final int count = count( query.get( COUNT_ARG ), defaultCount, minCount, zeroCountReturns );
        final String queryHash = queryHash( tool, query );
        final Page page = new Page( decode( query.get( CURSOR_ARG ), queryHash ), count, queryHash );
        if ( (long) page.offset() + page.count() > MAX_WINDOW )
        {
            throw new ToolError( "Invalid cursor: this listing can page through at most " + MAX_WINDOW + " items.",
                                 "Repeat the call without cursor and narrow it with path or types." );
        }
        return page;
    }

    private static int count( final Object value, final int defaultCount, final int minCount, final String zeroCountReturns )
    {
        if ( value == null )
        {
            return defaultCount;
        }
        final Integer count = McpArgs.toInt( value );
        if ( count == null || count < minCount || count > MAX_COUNT )
        {
            throw new ToolError( "Invalid count: " + McpJson.write( value ) + ".",
                                 "Use an integer from " + minCount + " to " + MAX_COUNT + " (default " + defaultCount + ")" +
                                     ( minCount == 0 ? "; 0 returns " + zeroCountReturns + "." : "." ) );
        }
        return count;
    }

    public static String queryHash( final String tool, final Map<String, ?> args )
    {
        final Map<String, Object> query = new LinkedHashMap<>( Nulls.clean( args ) );
        query.remove( CURSOR_ARG );
        query.remove( COUNT_ARG );
        try
        {
            final byte[] digest = MessageDigest.getInstance( "SHA-256" )
                .digest( ( tool + '\n' + McpJson.writeSorted( query ) ).getBytes( StandardCharsets.UTF_8 ) );
            return HexFormat.of().formatHex( digest, 0, 8 );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new IllegalStateException( e );
        }
    }

    public static String encode( final int offset, final String queryHash )
    {
        final Map<String, Object> cursor = new LinkedHashMap<>();
        cursor.put( "offset", offset );
        cursor.put( "queryHash", queryHash );
        return Base64.getUrlEncoder().withoutPadding().encodeToString( McpJson.write( cursor ).getBytes( StandardCharsets.UTF_8 ) );
    }

    static int decode( final Object cursor, final String queryHash )
    {
        if ( cursor == null || "".equals( cursor ) )
        {
            return 0;
        }
        if ( !( cursor instanceof String value ) )
        {
            throw new ToolError( "Invalid cursor: expected the nextCursor string from a previous result.", RESTART_HINT );
        }

        final Map<String, Object> decoded;
        try
        {
            decoded = McpJson.readObject( new String( Base64.getUrlDecoder().decode( value ), StandardCharsets.UTF_8 ) );
        }
        catch ( IllegalArgumentException e )
        {
            throw new ToolError( "Invalid cursor: it cannot be decoded.", RESTART_HINT );
        }

        final Integer offset = McpArgs.toInt( decoded.get( "offset" ) );
        if ( offset == null || offset < 0 || !( decoded.get( "queryHash" ) instanceof String ) )
        {
            throw new ToolError( "Invalid cursor: it cannot be decoded.", RESTART_HINT );
        }
        if ( !queryHash.equals( decoded.get( "queryHash" ) ) )
        {
            throw new ToolError( "Invalid cursor: cursor does not match this query; repeat the call without cursor.",
                                 "Pass nextCursor only together with the same arguments as the call that returned it." );
        }
        return offset;
    }
}
