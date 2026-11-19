package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Nulls
{
    private Nulls()
    {
    }

    /**
     * Removes null-valued object fields at any depth. Array items are kept as is, nulls included, so positions stay meaningful.
     */
    public static Map<String, Object> clean( final Map<?, ?> map )
    {
        return copy( map, false );
    }

    /**
     * A deep, mutable copy of {@code map} that keeps null values.
     */
    public static Map<String, Object> copy( final Map<?, ?> map )
    {
        return copy( map, true );
    }

    private static Map<String, Object> copy( final Map<?, ?> map, final boolean keepNulls )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        map.forEach( ( key, value ) -> {
            if ( value != null || keepNulls )
            {
                result.put( String.valueOf( key ), copyValue( value, keepNulls ) );
            }
        } );
        return result;
    }

    private static Object copyValue( final Object value, final boolean keepNulls )
    {
        if ( value instanceof Map<?, ?> map )
        {
            return copy( map, keepNulls );
        }
        if ( value instanceof List<?> list )
        {
            final List<Object> result = new ArrayList<>( list.size() );
            list.forEach( item -> result.add( copyValue( item, keepNulls ) ) );
            return result;
        }
        return value;
    }
}
