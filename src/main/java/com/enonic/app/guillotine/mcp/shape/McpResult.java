package com.enonic.app.guillotine.mcp.shape;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class McpResult
{
    public static final String ERRORS = "errors";

    private static final String TRUNCATED_MESSAGE = "\u2026 (message truncated)";

    private McpResult()
    {
    }

    /**
     * Unbudgeted result; tools whose output size depends on data use the {@link ResultBudget} overloads.
     */
    public static Map<String, Object> success( final String summary, final Map<String, ?> structuredContent )
    {
        return envelope( summary, Nulls.clean( structuredContent ) );
    }

    public static Map<String, Object> success( final String summary, final Map<String, ?> structuredContent, final ResultBudget budget )
    {
        return budget.apply( summary, structuredContent );
    }

    /**
     * Budgeted success for a paged list: see {@link ResultBudget#apply(Map, String, Page, long)}.
     */
    public static Map<String, Object> success( final String summary, final Map<String, ?> structuredContent, final ResultBudget budget,
                                               final String pageListKey, final Page page, final long total )
    {
        return budget.apply( summary, structuredContent, pageListKey, page, total );
    }

    /**
     * Budgeted success for a paged list whose summary is computed from the structured object as finally returned: see
     * {@link ResultBudget#apply(Function, Map, String, Page, long)}.
     */
    public static Map<String, Object> success( final Function<Map<String, Object>, String> summary, final Map<String, ?> structuredContent,
                                               final ResultBudget budget, final String pageListKey, final Page page, final long total )
    {
        return budget.apply( summary, structuredContent, pageListKey, page, total );
    }

    /**
     * Result of a GraphQL execution: partial errors go next to the data as {@code errors: [{message, path}]}; only a result without
     * any data (see {@link #hasData}) is an error. {@code data} is what the tool extracted from the response.
     */
    public static Map<String, Object> fromGraphQl( final String summary, final Map<String, ?> structuredContent, final Object data,
                                                   final List<?> graphQlErrors, final ResultBudget budget )
    {
        final List<Map<String, Object>> errors = GraphQlErrors.sanitize( graphQlErrors );
        if ( !hasData( data ) )
        {
            return errors.isEmpty()
                ? success( summary, structuredContent, budget )
                : error( GraphQlErrors.failure( errors ) );
        }

        final Map<String, Object> structured = new LinkedHashMap<>( structuredContent );
        if ( !errors.isEmpty() )
        {
            structured.put( ERRORS, errors );
        }
        return success( summary, structured, budget );
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

    private static Map<String, Object> errorEnvelope( final String message )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "content", List.of( text( message ) ) );
        result.put( "isError", true );
        return result;
    }

    public static Map<String, Object> error( final ToolError error )
    {
        return error( error.userMessage() );
    }

    static Map<String, Object> envelope( final String summary, final Map<String, Object> structured )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "content", List.of( text( summary + "\n" + McpJson.write( structured ) ) ) );
        result.put( "structuredContent", structured );
        return result;
    }

    /**
     * Whether a GraphQL {@code data} object holds a value: a root object such as {@code guillotine} counts only when one of its fields
     * is non-null, since GraphQL keeps it when every field below it failed.
     */
    public static boolean hasData( final Object data )
    {
        if ( data instanceof Map<?, ?> map )
        {
            return map.values()
                .stream()
                .anyMatch( value -> value instanceof Map<?, ?> object
                    ? object.values().stream().anyMatch( Objects::nonNull )
                    : value != null );
        }
        return data != null;
    }

    private static Map<String, Object> text( final String text )
    {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put( "type", "text" );
        content.put( "text", text );
        return content;
    }
}
