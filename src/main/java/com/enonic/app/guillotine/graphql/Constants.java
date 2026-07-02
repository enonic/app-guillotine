package com.enonic.app.guillotine.graphql;

import java.util.List;

public interface Constants
{
    String CURRENT_CONTENT_FIELD = "__currentContent";

    String PROJECT_ARG = "project";

    String BRANCH_ARG = "branch";

    String SITE_ARG = "siteKey";

    String PAGE_BASE_URL_ARG = "pageBaseUrl";

    String MEDIA_BASE_URL_ARG = "mediaBaseUrl";

    String SITE_BASE_URL = "__siteBaseUrl";

    String MEDIA_BASE_URL = "__mediaBaseUrl";

    String PAGE_BASE_URL = "__pageBaseUrl";

    String ROOT_BASE_URL = "/";

    String ENDPOINT_PREFIX = "/_/";

    List<String> SUPPORTED_AGGREGATIONS =
        List.of( "terms", "stats", "range", "dateRange", "dateHistogram", "geoDistance", "min", "max", "count" );

}
