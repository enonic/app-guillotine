package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.enonic.app.guillotine.mcp.shape.McpJson;

/**
 * Values of data fields as {@code dataAsJson} returns them, keyed by the raw form item names. A field's values inside a repeated item
 * set are read as one list. Reference values, content ids, are replaced by {@code {_id, displayName, _path}} once labels are looked
 * up, or by {@code {_id, missing: true}} when the target is missing or not readable.
 */
final class DataValues
{
    private final DataFieldScope scope;

    private final Set<String> ids = new LinkedHashSet<>();

    DataValues( final DataFieldScope scope )
    {
        this.scope = scope;
    }

    /**
     * The values of {@code paths} (index paths under {@code data.}) in {@code data} of an item of {@code contentType}, keyed by the path
     * without {@code data.}; unset fields are left out. Reference ids found in them, also inside a requested set, are remembered for
     * {@link #referencedIds()}.
     */
    Map<String, Object> project( final Object data, final List<String> paths, final String contentType )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        for ( final String path : paths )
        {
            final String name = path.substring( DataFieldScope.DATA_PREFIX.length() );
            final Object value = valueAt( data, name.split( "\\." ), 0 );
            if ( value != null )
            {
                result.put( name, value instanceof Map<?, ?> || value instanceof List<?> ? McpJson.readObject(
                    McpJson.write( Map.of( "v", value ) ) ).get( "v" ) : value );
                below( result, name, scope.referencePaths( contentType ), item -> {
                    collect( item );
                    return item;
                } );
            }
        }
        return result;
    }

    /**
     * Remembers the reference ids anywhere in {@code data}, a whole {@code dataAsJson} object of an item of {@code contentType}.
     */
    void collectTree( final Map<String, Object> data, final String contentType )
    {
        for ( final String path : scope.referencePaths( contentType ) )
        {
            replaceAt( data, parts( path ), 0, value -> {
                collect( value );
                return value;
            } );
        }
    }

    Set<String> referencedIds()
    {
        return ids;
    }

    /**
     * Replaces the reference values of projected {@code data} maps under {@code hits}, by the type of each hit.
     */
    void label( final List<Map<String, Object>> hits, final Map<String, Map<String, Object>> labels, final Set<String> looked )
    {
        for ( final Map<String, Object> hit : hits )
        {
            if ( hit.get( "data" ) instanceof Map<?, ?> data )
            {
                @SuppressWarnings("unchecked") final Map<String, Object> values = (Map<String, Object>) data;
                labelProjected( values, String.valueOf( hit.get( "type" ) ), labels, looked );
            }
        }
    }

    void labelProjected( final Map<String, Object> values, final String contentType, final Map<String, Map<String, Object>> labels,
                         final Set<String> looked )
    {
        for ( final String name : List.copyOf( values.keySet() ) )
        {
            below( values, name, scope.referencePaths( contentType ), value -> labelled( value, labels, looked ) );
        }
    }

    /**
     * Replaces the reference values anywhere in {@code data}, a whole {@code dataAsJson} object of an item of {@code contentType}.
     */
    void labelTree( final Map<String, Object> data, final String contentType, final Map<String, Map<String, Object>> labels,
                    final Set<String> looked )
    {
        for ( final String path : scope.referencePaths( contentType ) )
        {
            replaceAt( data, parts( path ), 0, value -> labelled( value, labels, looked ) );
        }
    }

    /**
     * Applies {@code leaf} to the values of {@code fieldPaths} that the projected value under {@code name} holds: the value itself when
     * it is one of them, or the values below it when it is a set that contains them.
     */
    static void below( final Map<String, Object> projected, final String name, final Set<String> fieldPaths,
                       final UnaryOperator<Object> leaf )
    {
        final String requested = DataFieldScope.DATA_PREFIX + name;
        for ( final String path : fieldPaths )
        {
            if ( path.equals( requested ) )
            {
                projected.put( name, leaf.apply( projected.get( name ) ) );
            }
            else if ( path.startsWith( requested + "." ) )
            {
                replaceAt( projected.get( name ), path.substring( requested.length() + 1 ).split( "\\." ), 0, leaf );
            }
        }
    }

    /**
     * Applies {@code leaf} to each value at {@code parts} below {@code node}, walking into lists on the way.
     */
    @SuppressWarnings("unchecked")
    static void replaceAt( final Object node, final String[] parts, final int index, final UnaryOperator<Object> leaf )
    {
        if ( node instanceof List<?> list )
        {
            list.forEach( item -> replaceAt( item, parts, index, leaf ) );
        }
        else if ( node instanceof Map<?, ?> map && map.containsKey( parts[index] ) )
        {
            final Map<String, Object> object = (Map<String, Object>) map;
            if ( index < parts.length - 1 )
            {
                replaceAt( object.get( parts[index] ), parts, index + 1, leaf );
            }
            else if ( object.get( parts[index] ) != null )
            {
                object.put( parts[index], leaf.apply( object.get( parts[index] ) ) );
            }
        }
    }

    static String[] parts( final String path )
    {
        return path.substring( DataFieldScope.DATA_PREFIX.length() ).split( "\\." );
    }

    private void collect( final Object value )
    {
        if ( value instanceof String id && !id.isEmpty() )
        {
            ids.add( id );
        }
        else if ( value instanceof Collection<?> values )
        {
            values.forEach( this::collect );
        }
    }

    private static Object labelled( final Object value, final Map<String, Map<String, Object>> labels, final Set<String> looked )
    {
        if ( value instanceof List<?> values )
        {
            final List<Object> result = new ArrayList<>( values.size() );
            values.forEach( item -> result.add( labelled( item, labels, looked ) ) );
            return result;
        }
        if ( !( value instanceof String id ) || id.isEmpty() )
        {
            return value;
        }
        final Map<String, Object> reference = new LinkedHashMap<>();
        reference.put( ContentLookup.ID, id );
        if ( labels.containsKey( id ) )
        {
            reference.putAll( labels.get( id ) );
        }
        else if ( looked.contains( id ) )
        {
            reference.put( ContentLookup.MISSING, true );
        }
        return reference;
    }

    /**
     * The value at {@code path}; inside a repeated item set, the list of the values found in its occurrences.
     */
    private static Object valueAt( final Object node, final String[] path, final int index )
    {
        if ( index == path.length )
        {
            return node;
        }
        if ( node instanceof List<?> list )
        {
            final List<Object> values = new ArrayList<>();
            for ( final Object item : list )
            {
                final Object value = valueAt( item, path, index );
                if ( value instanceof List<?> nested )
                {
                    nested.stream().filter( Objects::nonNull ).forEach( values::add );
                }
                else if ( value != null )
                {
                    values.add( value );
                }
            }
            return values.isEmpty() ? null : values;
        }
        return node instanceof Map<?, ?> map ? valueAt( map.get( path[index] ), path, index + 1 ) : null;
    }
}
