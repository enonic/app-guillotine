package com.enonic.app.guillotine.mcp.shape;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class McpJson
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ObjectMapper SORTED_MAPPER = new ObjectMapper().enable( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS );

    public static final int PREVIEW_CHARS = 200;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>()
    {
    };

    private McpJson()
    {
    }

    public static Map<String, Object> readObject( final String json )
    {
        if ( json == null || json.isBlank() )
        {
            return Map.of();
        }

        final JsonNode node;
        try
        {
            node = MAPPER.readTree( json );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException( "Malformed JSON", e );
        }

        if ( node == null || !node.isObject() )
        {
            throw new IllegalArgumentException( "Expected a JSON object" );
        }
        return MAPPER.convertValue( node, MAP_TYPE );
    }

    public static String write( final Object value )
    {
        return write( MAPPER, value );
    }

    public static String writeSorted( final Object value )
    {
        return write( SORTED_MAPPER, value );
    }

    /**
     * Compact JSON of {@code value}, cut to {@link #PREVIEW_CHARS}, for echoing caller input in messages.
     */
    public static String preview( final Object value )
    {
        return shorten( write( value ), PREVIEW_CHARS );
    }

    public static String shorten( final String value, final int maxChars )
    {
        if ( value == null || value.length() <= maxChars )
        {
            return value;
        }
        int end = Math.max( 0, maxChars - 1 );
        if ( end > 0 && Character.isHighSurrogate( value.charAt( end - 1 ) ) )
        {
            end--;
        }
        return value.substring( 0, end ) + "\u2026";
    }

    public static int length( final Object value )
    {
        return write( value ).length();
    }

    private static String write( final ObjectMapper mapper, final Object value )
    {
        try
        {
            return mapper.writeValueAsString( value );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalStateException( "Cannot serialize MCP result", e );
        }
    }
}
