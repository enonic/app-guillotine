package com.enonic.app.guillotine.graphql.fetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

import com.enonic.app.guillotine.mapper.MapMapper;
import com.enonic.xp.script.ScriptValue;

public class DynamicTypeResolver
    implements TypeResolver
{
    private final ScriptValue typeResolver;

    public DynamicTypeResolver( final ScriptValue typeResolver )
    {
        this.typeResolver = typeResolver;
    }

    @Override
    public GraphQLObjectType getType( final TypeResolutionEnvironment env )
    {
        ScriptValue value = typeResolver.call( new MapMapper( env.getObject() ) );
        if ( value != null && value.isValue() )
        {
            return (GraphQLObjectType) env.getSchema().getType( value.getValue( String.class ) );
        }
        return null;
    }
}
