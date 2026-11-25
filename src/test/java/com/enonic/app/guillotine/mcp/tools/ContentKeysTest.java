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
    public void testScopePathsAreNormalizedOrRejectedWithAHint()
    {
        assertNull( ContentKeys.scopePath( null ) );
        assertNull( ContentKeys.scopePath( " " ) );
        assertNull( ContentKeys.scopePath( "/" ) );
        assertEquals( "/articles", ContentKeys.scopePath( "/articles/" ) );
        for ( final String path : List.of( "articles", "a/b", "/a//b", "/a/./b", "/a/../b", "/a/ b", "/a/*" ) )
        {
            assertTrue( assertThrows( ToolError.class, () -> ContentKeys.scopePath( path ), path ).userMessage()
                            .startsWith( "Invalid path: " ), path );
        }
    }
}
