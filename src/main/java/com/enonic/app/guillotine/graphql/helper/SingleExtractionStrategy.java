package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class SingleExtractionStrategy
    implements ExtractionStrategy
{
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extract( final Object jsApiResult )
    {
        if ( jsApiResult instanceof Map )
        {
            final Map<String, Object> contentAsMap = (Map<String, Object>) jsApiResult;
            return List.of( contentAsMap );
        }

        return Collections.emptyList();
    }
}
