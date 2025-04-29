package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ConnectionExtractionStrategy
    implements ExtractionStrategy
{
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extract( final Object jsApiResult )
    {
        if ( !( jsApiResult instanceof Map ) )
        {
            return Collections.emptyList();
        }

        final Map<String, Object> connectionAsMap = (Map<String, Object>) jsApiResult;

        final List<Map<String, Object>> edges = (List<Map<String, Object>>) connectionAsMap.get( "edges" );

        if ( edges == null )
        {
            return Collections.emptyList();
        }

        return edges.stream().map( edge -> (Map<String, Object>) edge.get( "node" ) ).toList();
    }
}
