package com.enonic.app.guillotine.graphql;

import java.util.List;

public interface Constants
{
    String CURRENT_CONTENT_FIELD = "__currentContent";

    String PROJECT_ARG = "project";

    String BRANCH_ARG = "branch";

    String SITE_ARG = "siteKey";

    String SITE_HEADER = "X-Guillotine-SiteKey";

    String SITE_BASE_URL = "__siteBaseUrl";

    List<String> SUPPORTED_AGGREGATIONS =
        List.of( "terms", "stats", "range", "dateRange", "dateHistogram", "geoDistance", "min", "max", "count" );

}
