package com.enonic.app.guillotine.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.mcp.shape.McpJson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Readers for MCP tool results ({@code content}, {@code structuredContent}, {@code isError}) shared by the MCP tests.
 */
public final class McpResults
{
    private McpResults()
    {
    }

    @SuppressWarnings("unchecked")
    public static String text( final Map<String, Object> result )
    {
        final List<Map<String, Object>> content = (List<Map<String, Object>>) result.get( "content" );
        assertEquals( 1, content.size(), content::toString );
        assertEquals( "text", content.get( 0 ).get( "type" ) );
        return (String) content.get( 0 ).get( "text" );
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> structured( final Map<String, Object> result )
    {
        return (Map<String, Object>) result.get( "structuredContent" );
    }

    /**
     * The structured content of a result that is not an error, as it reads after a JSON round trip.
     */
    public static Map<String, Object> successStructured( final Map<String, Object> result )
    {
        assertFalse( Boolean.TRUE.equals( result.get( "isError" ) ), () -> String.valueOf( result.get( "content" ) ) );
        return McpJson.readObject( McpJson.write( result.get( "structuredContent" ) ) );
    }

    public static Map<String, Object> roundTrip( final Map<String, Object> result )
    {
        return McpJson.readObject( McpJson.write( result ) );
    }

    public static Map<String, Object> args( final Object... keyValues )
    {
        final Map<String, Object> args = new LinkedHashMap<>();
        for ( int i = 0; i < keyValues.length; i += 2 )
        {
            args.put( (String) keyValues[i], keyValues[i + 1] );
        }
        return args;
    }
}
