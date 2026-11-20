package com.enonic.app.guillotine.mcp;

import java.util.List;
import java.util.Map;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

/**
 * Tool arguments as XP's MCP endpoint hands them to a controller: a map serialized into a plain script object.
 */
record McpArguments(Map<String, ?> map)
    implements MapSerializable
{
    @Override
    public void serialize( final MapGenerator generator )
    {
        entries( generator, map );
    }

    private static void entries( final MapGenerator generator, final Map<?, ?> entries )
    {
        entries.forEach( ( key, value ) -> {
            final String name = String.valueOf( key );
            if ( value instanceof Map<?, ?> nested )
            {
                generator.map( name );
                entries( generator, nested );
                generator.end();
            }
            else if ( value instanceof List<?> items )
            {
                generator.array( name );
                items( generator, items );
                generator.end();
            }
            else
            {
                generator.rawValue( name, value );
            }
        } );
    }

    private static void items( final MapGenerator generator, final List<?> items )
    {
        for ( final Object item : items )
        {
            if ( item instanceof Map<?, ?> nested )
            {
                generator.map();
                entries( generator, nested );
                generator.end();
            }
            else if ( item instanceof List<?> inner )
            {
                generator.array();
                items( generator, inner );
                generator.end();
            }
            else
            {
                generator.rawValue( item );
            }
        }
    }
}
