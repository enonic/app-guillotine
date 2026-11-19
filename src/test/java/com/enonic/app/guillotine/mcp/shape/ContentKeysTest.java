package com.enonic.app.guillotine.mcp.shape;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentKeysTest
{
    @Test
    public void testKeysAreNormalizedOrRejectedWithHint()
    {
        assertEquals( "/articles/a-1", ContentKeys.key( "  /articles/a-1/ " ) );
        assertEquals( "b9f3cf5c-1882-467e-b6c5-6dd2a48806e7", ContentKeys.key( "B9F3CF5C-1882-467E-B6C5-6DD2A48806E7" ) );
        assertEquals( "custom-id", ContentKeys.key( "custom-id" ) );
        for ( final String key : List.of( "articles/article-01", "/../x", "/a//b", "/a/./b", "/a /b", "/" ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> ContentKeys.key( key ) );
            assertTrue( error.userMessage().contains( "Paths start with / relative to the project root" ), key );
        }
        assertThrows( ToolError.class, () -> ContentKeys.key( "  " ) );
        assertThrows( ToolError.class, () -> ContentKeys.key( "/" + "a".repeat( ContentKeys.MAX_KEY_LENGTH ) ) );
    }

    @Test
    public void testScopePathsFollowTheKeyRule()
    {
        assertNull( ContentKeys.scopePath( null ) );
        assertNull( ContentKeys.scopePath( " " ) );
        assertNull( ContentKeys.scopePath( "/" ) );
        assertEquals( "/articles", ContentKeys.scopePath( "/articles/" ) );
        for ( final String path : List.of( "articles", "/x/..", "/a//b", "/a*", "/a?", "/a /b" ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> ContentKeys.scopePath( path ), path );
            assertTrue( error.userMessage().startsWith( "Invalid path: " ), error.userMessage() );
            assertTrue( error.userMessage().endsWith( "without . or .. elements." ), error.userMessage() );
        }
    }
}
