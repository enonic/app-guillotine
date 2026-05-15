package com.enonic.app.guillotine.graphql.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;

public class GuillotineLocalContextHelper
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>()
    {
    };

    public static String getSiteKey( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> localContext = getLocalContext( environment );
        return Objects.toString( localContext.get( Constants.SITE_ARG ), "/" );
    }

    public static Map<String, Object> newLocalContext( final DataFetchingEnvironment environment )
    {
        return new HashMap<>( getLocalContext( environment ) );
    }

    public static Map<String, Object> getLocalContext( final DataFetchingEnvironment environment )
    {
        if ( environment.getLocalContext() == null )
        {
            return new HashMap<>();
        }
        return environment.getLocalContext();
    }

    public static String mapToJson( final Map<String, Object> map )
    {
        try
        {
            return MAPPER.writeValueAsString( map );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalStateException( "Failed to serialize Map to JSON string", e );
        }
    }

    public static void putCurrentContent( final Map<String, Object> localContext, final Map<String, Object> content )
    {
        if ( content == null )
        {
            localContext.remove( Constants.CURRENT_CONTENT );
            return;
        }
        localContext.put( Constants.CURRENT_CONTENT, mapToJson( content ) );
    }

    public static Map<String, Object> getCurrentContent( final DataFetchingEnvironment environment )
    {
        final Object value = getLocalContext( environment ).get( Constants.CURRENT_CONTENT );
        if ( value == null )
        {
            return null;
        }
        if ( value instanceof String )
        {
            try
            {
                return MAPPER.readValue( (String) value, MAP_TYPE );
            }
            catch ( JsonProcessingException e )
            {
                throw new IllegalStateException( "Failed to deserialize " + Constants.CURRENT_CONTENT + " from JSON", e );
            }
        }
        throw new IllegalStateException(
            "Unexpected type for " + Constants.CURRENT_CONTENT + " in localContext: " + value.getClass().getName() );
    }
}
