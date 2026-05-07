package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ListExtractionStrategy
    implements ExtractionStrategy<List<Map<String, Object>>>
{
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extract( final Object jsApiResult )
    {
        if ( jsApiResult instanceof List )
        {
            return ( (List<Map<String, Object>>) jsApiResult );
        }

        return Collections.emptyList();
    }
}
