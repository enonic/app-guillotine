package com.enonic.app.guillotine.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.enonic.xp.script.serializer.MapGeneratorBase;

public class GuillotineMapGenerator
    extends MapGeneratorBase
{
    public GuillotineMapGenerator()
    {
        initRoot();
    }

    @Override
    protected Object newMap()
    {
        return new HashMap<String, Object>();
    }

    @Override
    protected Object newArray()
    {
        return new ArrayList<>();
    }

    @Override
    protected Object newFunction( final Function<?, ?> function )
    {
        return function;
    }

    @Override
    protected boolean isMap( final Object value )
    {
        return value instanceof Map;
    }

    @Override
    protected boolean isArray( final Object value )
    {
        return value instanceof Collection;
    }

    @Override
    protected void putInMap( final Object map, final String key, final Object value )
    {
        if ( map instanceof Map )
        {
            ( (Map<String, Object>) map ).put( key, value );
        }
    }

    @Override
    protected void putRawValueInMap( final Object map, final String key, final Object value )
    {
        putInMap( map, key, value );
    }

    @Override
    protected void addToArray( final Object array, final Object value )
    {
        if ( array instanceof Collection )
        {
            ( (Collection) array ).add( value );
        }
    }

    @Override
    protected MapGeneratorBase newGenerator()
    {
        return new GuillotineMapGenerator();
    }
}
