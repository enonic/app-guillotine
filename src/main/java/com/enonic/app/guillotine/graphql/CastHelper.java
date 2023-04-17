package com.enonic.app.guillotine.graphql;

import java.util.Map;

@SuppressWarnings("unchecked")
public class CastHelper
{

    public static <T> T cast( Map<String, Object> objectAsMap, String key )
    {
        return (T) objectAsMap.get( key );
    }

    public static <T> T cast( Object value )
    {
        return (T) value;
    }

    public static Map<String, Object> castToMap( Object value )
    {
        return (Map<String, Object>) value;
    }
}
