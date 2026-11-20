package com.enonic.app.guillotine.mcp.tools;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.mcp.shape.ToolError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentKeysTest
{
    @Test
    public void testKeysAreNormalizedOrRejectedWithAHint()
    {
        assertEquals( "/articles/a-1", ContentKeys.key( "  /articles/a-1/ ", "key" ) );
        assertEquals( "b9f3cf5c-1882-467e-b6c5-6dd2a48806e7", ContentKeys.key( "B9F3CF5C-1882-467E-B6C5-6DD2A48806E7", "key" ) );
        assertEquals( "custom-id", ContentKeys.key( "custom-id", "key" ) );
        for ( final String key : List.of( "/a//b", "/a/./b", "/a/../b", "/a/ b", "/a/*", "/" ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> ContentKeys.key( key, "key" ), key );
            assertTrue( error.userMessage().contains( "is not a content path" ), error.userMessage() );
        }
        assertTrue( assertThrows( ToolError.class, () -> ContentKeys.key( "a/b", "key" ) ).userMessage().contains( "relative path" ) );
        assertThrows( ToolError.class, () -> ContentKeys.key( "  ", "key" ) );
        assertThrows( ToolError.class, () -> ContentKeys.key( 42, "key" ) );
        assertThrows( ToolError.class, () -> ContentKeys.key( "/" + "a".repeat( ContentKeys.MAX_KEY_LENGTH ), "key" ) );
    }

    @Test
    public void testScopePathsFollowTheKeyRule()
    {
        assertNull( ContentKeys.scopePath( null ) );
        assertNull( ContentKeys.scopePath( " " ) );
        assertNull( ContentKeys.scopePath( "/" ) );
        assertEquals( "/articles", ContentKeys.scopePath( "/articles/" ) );
        for ( final String path : List.of( "articles", "/a/../b", "/a/*" ) )
        {
            assertTrue( assertThrows( ToolError.class, () -> ContentKeys.scopePath( path ), path ).userMessage()
                            .startsWith( "Invalid path: " ), path );
        }
    }
}
