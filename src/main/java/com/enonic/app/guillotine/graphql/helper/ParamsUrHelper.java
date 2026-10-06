package com.enonic.app.guillotine.graphql.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParamsUrHelper
{
    private ParamsUrHelper()
    {
    }

    /**
     * @return the query params of the {@code params} argument of a URL field: each value as a string, a list of values for
     * a list, and no value - such as {@code ?key} - for {@code null}; {@code null} values in a list are left out
     */
    public static Map<String, Collection<String>> convertToMultimap( final Map<String, ?> originalMap )
    {
        final Map<String, Collection<String>> result = new LinkedHashMap<>();

        for ( Map.Entry<String, ?> entry : originalMap.entrySet() )
        {
            final List<String> values = new ArrayList<>();

            final Object value = entry.getValue();
            if ( value instanceof Iterable<?> iterable )
            {
                iterable.forEach( v -> {
                    if ( v != null )
                    {
                        values.add( v.toString() );
                    }
                } );
            }
            else if ( value != null )
            {
                values.add( value.toString() );
            }

            result.put( entry.getKey(), values );
        }

        return result;
    }
}
