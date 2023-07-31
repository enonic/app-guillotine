package com.enonic.app.guillotine.graphql;

import java.util.List;

public interface Constants
{
    String CONTENT_ID_FIELD = "__contentId";

    List<String> SUPPORTED_AGGREGATIONS =
        List.of( "terms", "stats", "range", "dateRange", "dateHistogram", "geoDistance", "min", "max", "count" );

}
