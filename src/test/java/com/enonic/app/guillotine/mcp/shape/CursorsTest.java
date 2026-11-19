package com.enonic.app.guillotine.mcp.shape;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CursorsTest
{
    private static final ProjectBranch TARGET = new ProjectBranch( "blog", "master" );

    private static final Map<String, Object> ARGS = Map.of( "query", "news" );

    private static final String ZERO_COUNT = "only the total";

    private static final Map<String, Object> QUERY = Map.of( "project", "blog", "branch", "master", "query", "news" );

    @Test
    public void testCountDefaultsAndBounds()
    {
        assertEquals( 10, page( ARGS ).count() );
        assertEquals( 0, count( 0 ) );
        assertEquals( 50, count( 50 ) );
        assertEquals( 7, count( 7L ) );
        assertEquals( 7, count( 7.0 ) );
        assertEquals( 5, Cursors.page( ARGS, TARGET, 5, 1 ).count() );
    }

    @Test
    public void testCountOutOfRangeIsToolError()
    {
        for ( final Object count : new Object[]{51, -1, 2.5, "5", Long.MAX_VALUE, Double.NaN, true} )
        {
            final ToolError error = assertThrows( ToolError.class, () -> count( count ), String.valueOf( count ) );
            assertEquals( "Invalid count: " + McpJson.write( count ) + ". Use an integer from 0 to 50 (default 10); 0 returns only " +
                              "the total.", error.userMessage() );
        }
        assertEquals( "Invalid count: 0. Use an integer from 1 to 50 (default 5).", assertThrows( ToolError.class, () -> Cursors.page(
            with( ARGS, "count", 0 ), TARGET, 5, 1 ) ).userMessage() );
        assertThrows( IllegalArgumentException.class, () -> Cursors.page( ARGS, TARGET, 10, 0 ) );
    }

    @Test
    public void testFirstPageWithoutCursor()
    {
        final Page page = page( ARGS );

        assertEquals( 0, page.offset() );
        assertEquals( 10, page.count() );
        assertEquals( Cursors.queryHash( QUERY ), page.queryHash() );
        assertEquals( 0, page( with( ARGS, "cursor", "" ) ).offset() );
    }

    @Test
    public void testNextCursorRoundTrip()
    {
        final Page first = page( with( ARGS, "count", 20 ) );
        final String next = first.nextCursor( 45, 20 );

        final Page second = page( with( with( ARGS, "count", 20 ), "cursor", next ) );
        assertEquals( 20, second.offset() );
        assertEquals( 20, second.count() );

        final Page third = page( with( with( ARGS, "count", 20 ), "cursor", second.nextCursor( 45, 20 ) ) );
        assertEquals( 40, third.offset() );
        assertNull( third.nextCursor( 45, 20 ) );
    }

    @Test
    public void testCountMayChangeBetweenPages()
    {
        final String next = page( with( ARGS, "count", 5 ) ).nextCursor( 100, 5 );

        assertEquals( 5, page( with( with( ARGS, "count", 50 ), "cursor", next ) ).offset() );
    }

    @Test
    public void testNextCursorOnlyWhenMoreResultsExist()
    {
        final Page page = new Page( 0, 10, "h" );

        assertNull( page.nextCursor( 10, 10 ) );
        assertNull( page.nextCursor( 0, 10 ) );
        assertEquals( Cursors.encode( 10, "h" ), page.nextCursor( 11, 10 ) );
        assertNull( new Page( 0, 0, "h" ).nextCursor( 100, 0 ) );
        assertEquals( Cursors.encode( 3, "h" ), page.nextCursor( 11, 3 ) );
        assertNull( page.nextCursor( 11, 0 ) );
        assertNull( new Page( Integer.MAX_VALUE, 10, "h" ).nextCursor( Long.MAX_VALUE, 10 ) );
    }

    @Test
    public void testCursorPastTheEndIsRejected()
    {
        new Page( 0, 10, "h" ).requireWithin( 0 );
        new Page( 20, 10, "h" ).requireWithin( 21 );

        final ToolError error = assertThrows( ToolError.class, () -> new Page( 20, 10, "h" ).requireWithin( 20 ) );
        assertEquals( "Invalid cursor: cursor is past the end of the results; repeat the call without cursor.", error.userMessage() );
        assertThrows( ToolError.class, () -> new Page( 20, 10, "h" ).requireWithin( 5 ) );
    }

    @Test
    public void testCursorFromDifferentQueryIsRejected()
    {
        final String foreign = page( Map.of( "query", "sport" ) ).nextCursor( 100, 10 );
        final String otherBranch = Cursors.page( ARGS, new ProjectBranch( "blog", "draft" ), ZERO_COUNT ).nextCursor( 100, 10 );

        assertThrows( ToolError.class, () -> page( with( ARGS, "cursor", otherBranch ) ) );
        final ToolError error = assertThrows( ToolError.class, () -> page( with( ARGS, "cursor", foreign ) ) );
        assertTrue( error.userMessage().contains( "cursor does not match this query; repeat the call without cursor" ),
                    error.userMessage() );
    }

    @Test
    public void testGarbageCursorIsRejected()
    {
        final Object[] cursors =
            {"!!!not-base64", encode( "not json" ), encode( "[1]" ), encode( "{\"offset\":-5,\"queryHash\":\"x\"}" ),
                encode( "{\"offset\":\"1\",\"queryHash\":\"x\"}" ), encode( "{\"offset\":1}" ), 42};
        for ( final Object cursor : cursors )
        {
            final ToolError error =
                assertThrows( ToolError.class, () -> page( with( ARGS, "cursor", cursor ) ), cursor.toString() );
            assertTrue( error.userMessage().startsWith( "Invalid cursor" ), error.userMessage() );
            assertTrue( error.userMessage().contains( "without cursor" ), error.userMessage() );
        }
    }

    @Test
    public void testTamperedQueryHashIsRejected()
    {
        final String tampered = Cursors.encode( 10, "0000000000000000" );

        assertThrows( ToolError.class, () -> page( with( ARGS, "cursor", tampered ) ) );
    }

    @Test
    public void testQueryHashIsStable()
    {
        final Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put( "query", "news" );
        reordered.put( "branch", "master" );
        reordered.put( "project", "blog" );
        reordered.put( "count", 30 );
        reordered.put( "cursor", "abc" );
        reordered.put( "sort", null );

        assertEquals( Cursors.queryHash( QUERY ), Cursors.queryHash( reordered ) );
        assertEquals( 16, Cursors.queryHash( QUERY ).length() );
        assertNotEquals( Cursors.queryHash( QUERY ), Cursors.queryHash( with( QUERY, "query", "sport" ) ) );
        assertEquals( Cursors.queryHash( Map.of( "filter", Map.of( "a", 1, "b", 2 ) ) ),
                      Cursors.queryHash( Map.of( "filter", new LinkedHashMap<>( Map.of( "b", 2, "a", 1 ) ) ) ) );
    }

    @Test
    public void testCursorIsUrlSafe()
    {
        final String cursor = Cursors.encode( 123456, Cursors.queryHash( QUERY ) );

        assertTrue( cursor.matches( "[A-Za-z0-9_-]+" ), cursor );
    }

    @Test
    public void testPagePastTheSearchWindowIsRejected()
    {
        final String deep = Cursors.encode( Cursors.MAX_WINDOW - 5, Cursors.queryHash( QUERY ) );

        assertEquals( Cursors.MAX_WINDOW - 5, page( with( with( ARGS, "count", 5 ), "cursor", deep ) ).offset() );
        final ToolError error =
            assertThrows( ToolError.class, () -> page( with( with( ARGS, "count", 6 ), "cursor", deep ) ) );
        assertEquals( "Invalid cursor: this listing can page through at most 10000 items. Repeat the call without cursor and narrow it " +
                          "with path or types.", error.userMessage() );
    }

    private static Page page( final Map<String, Object> args )
    {
        return Cursors.page( args, TARGET, ZERO_COUNT );
    }

    private static int count( final Object count )
    {
        return page( with( ARGS, "count", count ) ).count();
    }

    private static String encode( final String json )
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString( json.getBytes( StandardCharsets.UTF_8 ) );
    }

    private static Map<String, Object> with( final Map<String, Object> args, final String key, final Object value )
    {
        final Map<String, Object> result = new HashMap<>( args );
        result.put( key, value );
        return result;
    }
}
