package com.enonic.app.guillotine.graphql.helper;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Multimap;

public final class ParamsUrHelper
{
    private ParamsUrHelper()
    {
    }

    public static void resolveParams( final Multimap<String, String> holder, final Object originalParams )
    {
        if ( originalParams instanceof Map )
        {
            for ( Map.Entry<String, Object> entry : ( (Map<String, Object>) originalParams ).entrySet() )
            {
                applyParam( holder, entry.getKey(), entry.getValue() );
            }
        }
    }

    private static void applyParam( final Multimap<String, String> holder, final String key, final Object value )
    {
        if ( value instanceof Iterable )
        {
            applyParam( holder, key, (Iterable) value );
        }
        else
        {
            holder.put( key, Objects.toString( value, null ) );
        }
    }

    private static void applyParam( final Multimap<String, String> holder, final String key, final Iterable values )
    {
        for ( final Object value : values )
        {
            holder.put( key, Objects.toString( value, null ) );
        }
    }
}
