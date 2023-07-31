package com.enonic.app.guillotine.graphql.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayHelper
{
    public static List<Object> forceArray( Object value )
    {
        List<Object> result = new ArrayList<>();
        if ( value != null )
        {
            if ( value instanceof Collection )
            {
                result.addAll( (Collection<?>) value );
            }
            else
            {
                result.add( value );
            }
        }
        return result;
    }

    public static <T> List<T> slice( List<T> list, int offset, int first )
    {
        return list.stream().skip( offset ).limit( first ).collect( Collectors.toList() );
    }
}
