package com.enonic.app.guillotine.graphql;

import java.util.List;

public interface Constants
{
    String CONTENT_ID_FIELD = "__contentId";

    String PROJECT_ARG = "project";

    String BRANCH_ARG = "branch";

    String SITE_ARG = "siteKey";

    String SITE_HEADER = "X-Guillotine-SiteKey";

    List<String> SUPPORTED_AGGREGATIONS =
        List.of( "terms", "stats", "range", "dateRange", "dateHistogram", "geoDistance", "min", "max", "count" );

}
