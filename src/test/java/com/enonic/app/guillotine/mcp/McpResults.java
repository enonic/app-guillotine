package com.enonic.app.guillotine.mcp;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Readers for MCP tool results ({@code content}, {@code structuredContent}, {@code isError}) shared by the MCP tests.
 */
public final class McpResults
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    /**
     * The structured content of a success result, checked to be what its text says, as it reads after a JSON round trip.
     */
    public static Map<String, Object> structured( final Map<String, Object> result )
    {
        assertFalse( Boolean.TRUE.equals( result.get( "isError" ) ), () -> text( result ) );
        final Map<String, Object> structured = read( write( result.get( "structuredContent" ) ) );
        assertEquals( structured, read( text( result ) ) );
        return structured;
    }

    public static String write( final Object value )
    {
        try
        {
            return MAPPER.writeValueAsString( value );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalStateException( e );
        }
    }

    public static Map<String, Object> read( final String json )
    {
        try
        {
            return MAPPER.readValue( json, new TypeReference<>()
            {
            } );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalStateException( e );
        }
    }
}
