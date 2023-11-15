package com.enonic.app.guillotine.mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.scalars.CustomScalars;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class GraphQLMapper
    implements MapSerializable
{
    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.rawValue( "GraphQLString", Scalars.GraphQLString );
        gen.rawValue( "GraphQLInt", Scalars.GraphQLInt );
        gen.rawValue( "GraphQLID", Scalars.GraphQLID );
        gen.rawValue( "GraphQLBoolean", Scalars.GraphQLBoolean );
        gen.rawValue( "GraphQLFloat", Scalars.GraphQLFloat );
        gen.rawValue( "Json", ExtendedScalars.Json );
        gen.rawValue( "DateTime", ExtendedScalars.DateTime );
        gen.rawValue( "Date", ExtendedScalars.Date );
        gen.rawValue( "LocalTime", CustomScalars.LocalTime );
        gen.rawValue( "LocalDateTime", CustomScalars.LocalDateTime );
        gen.rawValue( "list", (Function<GraphQLType, GraphQLType>) GraphQLList::new );
        gen.rawValue( "nonNull", (Function<GraphQLType, GraphQLType>) GraphQLNonNull::new );
        gen.rawValue( "reference", (Function<String, GraphQLTypeReference>) GraphQLTypeReference::new );
        gen.rawValue( "createDataFetcherResult", createDataFetcherResult() );
    }

    private static Function<Map<String, Object>, DataFetcherResult<Object>> createDataFetcherResult()
    {
        return params -> DataFetcherResult.newResult().data( resolveData( params.get( "data" ) ) ).localContext(
            resolveLocalContext( params.get( "localContext" ), params.get( "parentLocalContext" ) ) ).build();
    }

    private static Object resolveData( final Object data )
    {
        if ( data instanceof ScriptValue )
        {
            return convertScriptValue( (ScriptValue) data );
        }
        else
        {
            checkType( data );
            return data;
        }
    }

    private static Object resolveLocalContext( Object localContext, Object parentLocalContext )
    {
        if ( localContext instanceof Map )
        {
            ( (Map<String, Object>) localContext ).values().forEach( GraphQLMapper::checkType );

            Map<String, Object> mergedLocalContext = new HashMap<>();
            if ( parentLocalContext instanceof Map )
            {
                ( (Map<String, Object>) parentLocalContext ).values().forEach( GraphQLMapper::checkType );
                mergedLocalContext.putAll( skipNullableValueFromMap( (Map<String, Object>) parentLocalContext ) );
            }
            mergedLocalContext.putAll( skipNullableValueFromMap( (Map<String, Object>) localContext ) );
            return Collections.unmodifiableMap( mergedLocalContext );
        }
        else
        {
            checkType( localContext );
            return localContext;
        }
    }

    private static Map<String, Object> skipNullableValueFromMap( final Map<String, Object> map )
    {
        return map.entrySet().stream().filter( entry -> entry.getValue() != null ).collect(
            Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }

    private static Object convertScriptValue( final ScriptValue scriptValue )
    {
        if ( scriptValue.isObject() )
        {
            return scriptValue.getMap();
        }
        else if ( scriptValue.isArray() )
        {
            return scriptValue.getList();
        }
        else if ( scriptValue.isValue() )
        {
            return scriptValue.getValue();
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ScriptValue type: " + scriptValue.getClass().getName() );
        }
    }

    private static void checkType( final Object value )
    {
        if ( !( value instanceof String || value instanceof Double || value instanceof Integer || value instanceof Boolean ||
            value == null ) )
        {
            throw new IllegalArgumentException(
                String.format( "Unsupported type \"%s\". Type of value must be String, Double, Integer or Boolean.",
                               value.getClass().getName() ) );
        }
    }
}
