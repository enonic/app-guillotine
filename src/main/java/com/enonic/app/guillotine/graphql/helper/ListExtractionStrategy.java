package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.enonic.xp.content.Content;

final class ListExtractionStrategy
    implements ExtractionStrategy
{
    @Override
    @SuppressWarnings("unchecked")
    public List<Content> extract( final Object jsApiResult )
    {
        if ( jsApiResult instanceof List )
        {
            final List<Map<String, Object>> listContentAsMap = (List<Map<String, Object>>) jsApiResult;
            return listContentAsMap.stream().map( ContentDeserializer::convert ).toList();
        }

        return Collections.emptyList();
    }
}
