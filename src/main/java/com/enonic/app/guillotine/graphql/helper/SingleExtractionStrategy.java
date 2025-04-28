package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.enonic.xp.content.Content;

final class SingleExtractionStrategy
    implements ExtractionStrategy
{
    @Override
    @SuppressWarnings("unchecked")
    public List<Content> extract( final Object jsApiResult )
    {
        if ( jsApiResult instanceof Map )
        {
            final Map<String, ?> contentAsMap = (Map<String, ?>) jsApiResult;
            return List.of( ContentDeserializer.convert( contentAsMap ) );
        }

        return Collections.emptyList();
    }
}
