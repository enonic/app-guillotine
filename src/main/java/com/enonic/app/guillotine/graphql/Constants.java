package com.enonic.app.guillotine.graphql;

import java.util.List;

public interface Constants
{
    String CONTENT_ID_FIELD = "__contentId";

    String GUILLOTINE_TARGET_PROJECT_CTX = "__targetProject";

    String GUILLOTINE_TARGET_BRANCH_CTX = "__targetBranch";

    String GUILLOTINE_TARGET_SITE_CTX = "__targetSiteKey";

    List<String> SUPPORTED_AGGREGATIONS =
        List.of( "terms", "stats", "range", "dateRange", "dateHistogram", "geoDistance", "min", "max", "count" );

}
