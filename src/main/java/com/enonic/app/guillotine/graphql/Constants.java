package com.enonic.app.guillotine.graphql;

import java.util.List;

public interface Constants
{
    List<String> SUPPORTED_AGGREGATIONS =
        List.of( "terms", "stats", "range", "dateRange", "dateHistogram", "geoDistance", "min", "max", "count" );


}
