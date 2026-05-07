package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

final class SingleExtractionStrategy
    implements ExtractionStrategy<Map<String, Object>>
{
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> extract( final Object jsApiResult )
    {
        if ( jsApiResult instanceof Map )
        {
            return (Map<String, Object>) jsApiResult;
        }

        return null;
    }
}
