package com.enonic.app.guillotine.graphql.helper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;

public class GuillotineLocalContextHelper
{
    public static <T> T executeInContext( final DataFetchingEnvironment environment, Callable<T> callable )
    {
        return ContextBuilder.from( ContextAccessor.current() ).build().callWith( callable );
    }

    public static String getSiteKey( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        return Objects.toString( localContext.get( Constants.SITE_ARG ), "/" );
    }
}
