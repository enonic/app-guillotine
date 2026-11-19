package com.enonic.app.guillotine.mcp.shape;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

import com.enonic.xp.context.ContextAccessor;

/**
 * Small helpers shared by the MCP tools for reading arguments and the caller.
 */
public final class McpArgs
{
    private static final Pattern UUID =
        Pattern.compile( "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" );

    private McpArgs()
    {
    }

    /**
     * Whether {@code value} is a UUID in any letter case, the form of generated content ids.
     */
    public static boolean isUuid( final String value )
    {
        return value != null && UUID.matcher( value ).matches();
    }

    /**
     * The exact int value of a JSON number, or {@code null} for anything else, including fractions and out-of-range values.
     */
    public static Integer toInt( final Object value )
    {
        if ( value instanceof Integer i )
        {
            return i;
        }
        if ( value instanceof Number number )
        {
            try
            {
                return new BigDecimal( number.toString() ).intValueExact();
            }
            catch ( ArithmeticException | NumberFormatException e )
            {
                return null;
            }
        }
        return null;
    }

    public static boolean isAuthenticated()
    {
        return ContextAccessor.current().getAuthInfo().isAuthenticated();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> castMap( final Map<?, ?> map )
    {
        return (Map<String, Object>) map;
    }
}
