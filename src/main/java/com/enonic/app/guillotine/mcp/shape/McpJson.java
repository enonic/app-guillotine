package com.enonic.app.guillotine.mcp.shape;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class McpJson
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final int PREVIEW_CHARS = 200;

    private McpJson()
    {
    }

    public static String write( final Object value )
    {
        try
        {
            return MAPPER.writeValueAsString( value );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalStateException( "Cannot serialize MCP result", e );
        }
    }

    /**
     * The JSON object in {@code json}; {@link IllegalArgumentException} when it is not one.
     */
    public static Map<String, Object> readObject( final String json )
    {
        final Map<String, Object> value;
        try
        {
            value = MAPPER.readValue( json, new TypeReference<>()
            {
            } );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException( "Not a JSON object", e );
        }
        if ( value == null )
        {
            throw new IllegalArgumentException( "Not a JSON object" );
        }
        return value;
    }

    public static int length( final Object value )
    {
        return write( value ).length();
    }

    /**
     * Compact JSON of {@code value}, cut to {@link #PREVIEW_CHARS}, for echoing caller input in messages.
     */
    public static String preview( final Object value )
    {
        return shorten( write( value ), PREVIEW_CHARS );
    }

    /**
     * {@code value} cut to at most {@code maxChars} characters, ending with an ellipsis when cut, never inside a surrogate pair.
     */
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
        return value.substring( 0, end ) + "…";
    }
}
