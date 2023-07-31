package com.enonic.app.guillotine.mapper;

import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public final class PropertyTreeMapper
    implements MapSerializable
{
    private final PropertyTree value;

    private final boolean useRawValue;

    private final String contentId;

    public PropertyTreeMapper( final PropertyTree value, final String contentId )
    {
        this.value = value;
        this.contentId = contentId;
        this.useRawValue = false;
    }

    public PropertyTreeMapper( final boolean useRawValue, final PropertyTree value, final String contentId )
    {
        this.useRawValue = useRawValue;
        this.value = value;
        this.contentId = contentId;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        serialize( gen, this.value );
    }

    private void serialize( final MapGenerator gen, final PropertyTree value )
    {
        final Map<String, Object> map = value.toMap();
        serializeMap( gen, map );
    }

    private void serializeMap( final MapGenerator gen, final Map<?, ?> map )
    {
        for ( final Map.Entry<?, ?> entry : map.entrySet() )
        {
            serializeKeyValue( gen, entry.getKey().toString(), entry.getValue() );
        }
    }

    private void serializeKeyValue( final MapGenerator gen, final String key, final Object value )
    {
        if ( value instanceof List )
        {
            serializeList( gen, key, (List<?>) value );
        }
        else if ( value instanceof Map )
        {
            serializeMap( gen, key, (Map<?, ?>) value );
        }
        else
        {
            if ( this.useRawValue )
            {
                gen.rawValue( key, value );
            }
            else
            {
                gen.value( key, value );
            }
        }
    }

    private void serializeList( final MapGenerator gen, final String key, final List<?> values )
    {
        if ( values.isEmpty() )
        {
            serializeKeyValue( gen, key, null );
            return;
        }

        if ( values.size() == 1 )
        {
            serializeKeyValue( gen, key, values.get( 0 ) );
            return;
        }

        gen.array( key );

        for ( final Object value : values )
        {
            serializeValue( gen, value );
        }
        gen.end();
    }

    private void serializeMap( final MapGenerator gen, final String key, final Map<?, ?> map )
    {
        gen.map( key );
        if ( contentId != null )
        {
            gen.value( Constants.CONTENT_ID_FIELD, contentId );
        }
        serializeMap( gen, map );
        gen.end();
    }

    private void serializeValue( final MapGenerator gen, final Object value )
    {
        if ( value instanceof Map )
        {
            gen.map();
            serializeMap( gen, (Map<?, ?>) value );
            gen.end();
        }
        else
        {
            if ( this.useRawValue )
            {
                gen.rawValue( value );
            }
            else
            {
                gen.value( value );
            }
        }
    }
}
