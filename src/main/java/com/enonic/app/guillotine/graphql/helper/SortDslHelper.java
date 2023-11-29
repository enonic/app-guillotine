package com.enonic.app.guillotine.graphql.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortDslHelper
{
    public static Object createDslSort( final Object sort )
    {
        List<Map<String, Object>> result = new ArrayList<>();

        ArrayHelper.forceArray( sort ).forEach( sortItem -> {
            Map<String, Object> sortItemAsMap = CastHelper.cast( sortItem );

            Map<String, Object> sortAsMap = new HashMap<>();

            sortAsMap.put( "field", sortItemAsMap.get( "field" ) );
            sortAsMap.computeIfAbsent( "direction", k -> sortItemAsMap.get( "direction" ) );
            sortAsMap.computeIfAbsent( "unit", k -> sortItemAsMap.get( "unit" ) );

            if ( sortItemAsMap.get( "location" ) != null )
            {
                Map<String, Object> locationAsMap = CastHelper.cast( sortItemAsMap.get( "location" ) );
                sortAsMap.put( "location", Map.of( "lat", locationAsMap.get( "lat" ), "lon", locationAsMap.get( "lon" ) ) );
            }

            result.add( sortAsMap );
        } );

        return result;
    }
}
