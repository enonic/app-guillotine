package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GetFieldAsJsonDataFetcher
    implements DataFetcher<Object>
{
    private final String fieldName;

    public GetFieldAsJsonDataFetcher( final String fieldName )
    {
        this.fieldName = fieldName;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();
        return sourceAsMap.get( fieldName );
    }
}
