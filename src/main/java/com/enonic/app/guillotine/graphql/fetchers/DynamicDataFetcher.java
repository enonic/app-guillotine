package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.app.guillotine.mapper.DataFetchingEnvironmentMapper;
import com.enonic.xp.script.ScriptValue;

public class DynamicDataFetcher
    implements DataFetcher<Object>
{
    private final ScriptValue resolveFunction;

    public DynamicDataFetcher( final ScriptValue resolveFunction )
    {
        this.resolveFunction = resolveFunction;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return ContentSerializer.serialize( resolveFunction.call( new DataFetchingEnvironmentMapper( environment ) ) );
    }
}
