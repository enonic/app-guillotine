package com.enonic.app.guillotine.helper;

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
}
