package com.enonic.app.guillotine.graphql.helper;

@SuppressWarnings("unchecked")
public class CastHelper
{
    public static <T> T cast( Object value )
    {
        return (T) value;
    }
}
