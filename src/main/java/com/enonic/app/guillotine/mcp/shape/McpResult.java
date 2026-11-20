package com.enonic.app.guillotine.mcp.shape;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tool results. A success carries {@code structuredContent} and one text content whose text is the same object as compact JSON,
 * so a client that shows the model only one of them loses nothing. An error carries only a text and {@code isError: true}.
 */
public final class McpResult
{
    public static final String ERRORS = "errors";

    private static final String TRUNCATED_MESSAGE = "… (message truncated)";

    private McpResult()
    {
    }

    /**
     * Result for a structured object whose size does not depend on data; others go through {@link ResultBudget}.
     */
    public static Map<String, Object> success( final Map<String, ?> structured )
    {
        return envelope( Nulls.clean( structured ) );
    }

    public static Map<String, Object> error( final ToolError error )
    {
        return error( error.userMessage() );
    }

    /**
     * Error result whose compact JSON never exceeds {@link ResultBudget#MIN_RESULT_CHARS}; a longer message is cut.
     */
    public static Map<String, Object> error( final String message )
    {
        final String text = message == null ? "" : message;
        final Map<String, Object> result = errorEnvelope( text );
        if ( McpJson.length( result ) <= ResultBudget.MIN_RESULT_CHARS )
        {
            return result;
        }
        int low = 0;
        int high = text.length();
        while ( low < high )
        {
            final int middle = ( low + high + 1 ) >>> 1;
            if ( McpJson.length( errorEnvelope( text.substring( 0, middle ) + TRUNCATED_MESSAGE ) ) <= ResultBudget.MIN_RESULT_CHARS )
            {
                low = middle;
            }
            else
            {
                high = middle - 1;
            }
        }
        if ( low > 0 && Character.isHighSurrogate( text.charAt( low - 1 ) ) )
        {
            low--;
        }
        return errorEnvelope( text.substring( 0, low ) + TRUNCATED_MESSAGE );
    }

    static Map<String, Object> envelope( final Map<String, Object> structured )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "content", List.of( text( McpJson.write( structured ) ) ) );
        result.put( "structuredContent", structured );
        return result;
    }

    private static Map<String, Object> errorEnvelope( final String message )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "content", List.of( text( message ) ) );
        result.put( "isError", true );
        return result;
    }

    private static Map<String, Object> text( final String text )
    {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put( "type", "text" );
        content.put( "text", text );
        return content;
    }
}
