package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class PageInfoDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();

        int count = ( (List<?>) sourceAsMap.get( "hits" ) ).size();
        int start = (int) sourceAsMap.get( "start" );
        int total = (int) sourceAsMap.get( "total" );

        Map<String, Object> result = new HashMap<>();

        result.put( "startCursor", start );
        result.put( "endCursor", start + ( count == 0 ? 0 : count - 1 ) );
        result.put( "hasNext", start + count < total );

        return result;
    }
}
