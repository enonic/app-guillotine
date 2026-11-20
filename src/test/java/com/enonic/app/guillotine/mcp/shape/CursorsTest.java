package com.enonic.app.guillotine.mcp.shape;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CursorsTest
{
    @Test
    public void testCountDefaultsAndPassesThrough()
    {
        assertEquals( new Page( 0, 10 ), Cursors.page( Map.of(), 10 ) );
        assertEquals( 0, Cursors.page( Map.of( "count", 0 ), 10 ).count() );
        assertEquals( 7, Cursors.page( Map.of( "count", 7.0 ), 10 ).count() );
        assertEquals( 0, Cursors.page( Map.of( "cursor", "" ), 10 ).offset() );
    }

    @Test
    public void testNextCursorRoundTrip()
    {
        final String next = Cursors.page( Map.of( "count", 20 ), 10 ).nextCursor( 45, 20 );
        final Page second = Cursors.page( Map.of( "count", 20, "cursor", next ), 10 );
        assertEquals( new Page( 20, 20 ), second );

        final Page third = Cursors.page( Map.of( "count", 20, "cursor", second.nextCursor( 45, 20 ) ), 10 );
        assertEquals( 40, third.offset() );
        assertNull( third.nextCursor( 45, 5 ) );
    }

    @Test
    public void testCursorIsNotBoundToTheQuery()
    {
        final String next = Cursors.page( Map.of( "text", "news", "count", 5 ), 10 ).nextCursor( 100, 5 );

        assertEquals( new Page( 5, 50 ), Cursors.page( Map.of( "text", "sport", "count", 50, "cursor", next ), 10 ) );
    }

    @Test
    public void testNextCursorFollowsTheLastItemReturned()
    {
        final Page page = new Page( 0, 10 );

        assertNull( page.nextCursor( 10, 10 ) );
        assertNull( page.nextCursor( 0, 10 ) );
        assertEquals( Cursors.encode( 10 ), page.nextCursor( 11, 10 ) );
        assertEquals( Cursors.encode( 3 ), page.nextCursor( 11, 3 ) );
        assertNull( page.nextCursor( 11, 0 ) );
        assertNull( new Page( Integer.MAX_VALUE, 10 ).nextCursor( Long.MAX_VALUE, 10 ) );
    }

    @Test
    public void testCursorPastTheEndIsRejected()
    {
        new Page( 0, 10 ).requireWithin( 0 );
        new Page( 20, 10 ).requireWithin( 21 );

        final ToolError error = assertThrows( ToolError.class, () -> new Page( 20, 10 ).requireWithin( 20 ) );
        assertEquals( "Invalid cursor: it points past the end of the results. Repeat the call without cursor to start from the first " +
                          "page.", error.userMessage() );
    }

    @Test
    public void testForeignCursorIsRejected()
    {
        for ( final Object cursor : new Object[]{"!!!not-base64", encode( "offset:x" ), encode( "offset:-5" ), encode( "{\"offset\":1}" ),
            encode( "offset:99999999999" ), 42} )
        {
            final ToolError error = assertThrows( ToolError.class, () -> Cursors.page( Map.of( "cursor", cursor ), 10 ), cursor::toString );
            assertEquals( "Invalid cursor: pass nextCursor from the previous result unchanged. Repeat the call without cursor to start " +
                              "from the first page.", error.userMessage() );
        }
    }

    @Test
    public void testCursorIsUrlSafe()
    {
        assertTrue( Cursors.encode( 123456 ).matches( "[A-Za-z0-9_-]+" ), Cursors.encode( 123456 ) );
    }

    @Test
    public void testPagingEndsAtTheSearchWindow()
    {
        Page page = Cursors.page( Map.of( "count", 30 ), 10 );
        int pages = 1;
        String next;
        while ( ( next = page.nextCursor( 50000, page.count() ) ) != null )
        {
            page = Cursors.page( Map.of( "count", 30, "cursor", next ), 10 );
            pages++;
        }

        assertEquals( new Page( 9990, 10 ), page );
        assertEquals( 334, pages );
        final ToolError error =
            assertThrows( ToolError.class, () -> Cursors.page( Map.of( "cursor", Cursors.encode( Cursors.MAX_WINDOW ) ), 10 ) );
        assertTrue( error.userMessage().startsWith( "Invalid cursor: a listing can page through at most 10000 items." ),
                    error.userMessage() );
    }

    private static String encode( final String text )
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString( text.getBytes( StandardCharsets.UTF_8 ) );
    }
}
