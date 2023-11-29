package com.enonic.app.guillotine.graphql.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighlightHelper
{
    public static Map<String, Object> createHighlight( final Map<String, Object> inputHighlight )
    {
        Map<String, Object> result = new HashMap<>();

        inputHighlight.keySet().forEach( key -> {
            if ( inputHighlight.get( key ) != null )
            {
                result.put( key, inputHighlight.get( key ) );
            }
        } );
        result.put( "properties", createHighlightProperties( inputHighlight ) );

        return result;
    }

    private static Map<String, Object> createHighlightProperties( final Map<String, Object> inputHighlight )
    {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> properties = CastHelper.cast( inputHighlight.get( "properties" ) );

        properties.forEach( highlightProperty -> {
            Map<String, Object> propertyData = new HashMap<>();
            highlightProperty.keySet().stream().filter( prop -> !"propertyName".equals( prop ) ).forEach( prop -> {
                if ( highlightProperty.get( prop ) != null )
                {
                    propertyData.put( prop, highlightProperty.get( prop ) );
                }
            } );
            result.put( highlightProperty.get( "propertyName" ).toString(), propertyData );
        } );

        return result;
    }
}
