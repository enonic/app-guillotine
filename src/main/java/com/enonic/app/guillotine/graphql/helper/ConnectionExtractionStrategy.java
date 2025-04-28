package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.enonic.xp.content.Content;

final class ConnectionExtractionStrategy
    implements ExtractionStrategy
{
    @Override
    @SuppressWarnings("unchecked")
    public List<Content> extract( final Object jsApiResult )
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

        final List<Map<String, Object>> contentsAsMap = edges.stream().map( edge -> (Map<String, Object>) edge.get( "node" ) ).toList();

        if ( contentsAsMap.isEmpty() )
        {
            return Collections.emptyList();
        }

        return contentsAsMap.stream().map( ContentDeserializer::convert ).collect( Collectors.toList() );
    }
}
