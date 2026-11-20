package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deep copies of structured results without {@code null} array items, which XP drops on the way to the client so that
 * {@code structuredContent} would no longer match the text.
 */
public final class Nulls
{
    private Nulls()
    {
    }

    /**
     * A deep, mutable copy without null object fields and without null array items.
     */
    public static Map<String, Object> clean( final Map<?, ?> map )
    {
        return copy( map, false );
    }

    /**
     * A deep, mutable copy that keeps null object fields, for free-form data such as a GraphQL response, and drops null array items.
     */
    public static Map<String, Object> keepingNullFields( final Map<?, ?> map )
    {
        return copy( map, true );
    }

    private static Map<String, Object> copy( final Map<?, ?> map, final boolean keepNullFields )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        map.forEach( ( key, value ) -> {
            if ( value != null || keepNullFields )
            {
                result.put( String.valueOf( key ), copyValue( value, keepNullFields ) );
            }
        } );
        return result;
    }

    private static Object copyValue( final Object value, final boolean keepNullFields )
    {
        if ( value instanceof Map<?, ?> map )
        {
            return copy( map, keepNullFields );
        }
        if ( value instanceof List<?> list )
        {
            final List<Object> result = new ArrayList<>( list.size() );
            for ( final Object item : list )
            {
                if ( item != null )
                {
                    result.add( copyValue( item, keepNullFields ) );
                }
            }
            return result;
        }
        return value;
    }
}
