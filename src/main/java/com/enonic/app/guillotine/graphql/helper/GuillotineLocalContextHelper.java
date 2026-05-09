package com.enonic.app.guillotine.graphql.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;

public class GuillotineLocalContextHelper
{
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
}
