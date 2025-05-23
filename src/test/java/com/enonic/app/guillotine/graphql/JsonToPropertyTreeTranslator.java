package com.enonic.app.guillotine.graphql;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;

public final class JsonToPropertyTreeTranslator
{
    public static PropertyTree translate( final JsonNode json )
    {
        final PropertyTree propertyTree = new PropertyTree();
        traverse( json, propertyTree.getRoot() );
        return propertyTree;
    }

    private static void traverse( final JsonNode json, final PropertySet parent )
    {
        final Iterator<Map.Entry<String, JsonNode>> fields = json.fields();

        while ( fields.hasNext() )
        {
            final Map.Entry<String, JsonNode> next = fields.next();
            addValue( parent, next.getKey(), next.getValue() );
        }
    }

    private static void addValue( final PropertySet parent, final String key, final JsonNode value )
    {
        if ( value.isArray() )
        {
            for ( final JsonNode objNode : value )
            {
                addValue( parent, key, objNode );
            }
        }
        else if ( value.isObject() )
        {
            final PropertySet parentSet = parent.addSet( key );
            value.fields().forEachRemaining( ( objectValue ) -> addValue( parentSet, objectValue.getKey(), objectValue.getValue() ) );
        }
        else
        {
            mapValue( parent, key, value );
        }
    }

    private static void mapValue( final PropertySet parent, final String key, final JsonNode value )
    {
        parent.addProperty( key, resolveCoreValue( value ) );
    }

    private static Value resolveCoreValue( final JsonNode value )
    {
        if ( value.isDouble() )
        {
            return ValueFactory.newDouble( value.doubleValue() );
        }

        if ( value.isTextual() )
        {
            return ValueFactory.newString( value.textValue() );
        }

        if ( value.isInt() )
        {
            return ValueFactory.newLong( (long) value.intValue() );
        }

        if ( value.isLong() )
        {
            return ValueFactory.newLong( value.longValue() );
        }

        if ( value.isBoolean() )
        {
            return ValueFactory.newBoolean( value.booleanValue() );
        }

        if ( value.isObject() )
        {
            return mapSet( value );
        }

        return ValueFactory.newString( value.toString() );
    }

    private static Value mapSet( final JsonNode value )
    {
        PropertyTree propertyTree = new PropertyTree();
        PropertySet propertySet = propertyTree.newSet();

        value.properties().forEach( field -> {
            if ( field.getValue().isArray() )
            {
                field.getValue().forEach( arrayNode -> propertySet.addProperty( field.getKey(), resolveCoreValue( arrayNode ) ) );
            }
            else
            {
                propertySet.addProperty( field.getKey(), resolveCoreValue( field.getValue() ) );
            }
        } );

        return ValueFactory.newPropertySet( propertySet );
    }
}
