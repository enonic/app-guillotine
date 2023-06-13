package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;

public class CursorConnectionDataFetcher
    implements DataFetcher<Object>
{
    private final String fieldName;

    public CursorConnectionDataFetcher( final String fieldName )
    {
        this.fieldName = fieldName;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();
        return ConnectionHelper.encodeCursor( sourceAsMap.get( fieldName ).toString() );
    }
}
