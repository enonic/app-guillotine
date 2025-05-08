package com.enonic.app.guillotine.graphql.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ParamsUrHelper
{
    private ParamsUrHelper()
    {
    }

    public static Map<String, Collection<String>> convertToMultimap( final Map<String, ?> originalMap )
    {
        final Map<String, Collection<String>> result = new LinkedHashMap<>();

        for ( Map.Entry<String, ?> entry : originalMap.entrySet() )
        {
            final List<String> values = new ArrayList<>();

            final Object value = entry.getValue();
            if ( value instanceof Iterable )
            {
                ( (Iterable<?>) value ).forEach( v -> values.add( Objects.toString( v, null ) ) );
            }
            else
            {
                values.add( Objects.toString( value, null ) );
            }

            result.put( entry.getKey(), values );
        }

        return result;
    }
}
