package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;

public class EdgesDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();

        List<Map<String, Object>> hits = CastHelper.cast( sourceAsMap.get( "hits" ) );

        List<Map<String, Object>> edges = new ArrayList<>();

        for ( int i = 0; i < hits.size(); i++ )
        {
            Map<String, Object> edge = new HashMap<>();

            edge.put( "node", hits.get( i ) );
            edge.put( "cursor", (int) sourceAsMap.get( "start" ) + i );

            edges.add( edge );
        }

        return edges;
    }
}
