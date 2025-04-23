package com.enonic.app.guillotine.graphql;

import java.util.List;

public interface Constants
{
    String CONTENT_ID_FIELD = "__contentId";

    String CONTENTS_WITH_ATTACHMENTS_FIELD = "__contentsWithAttachments";

    String ATTACHMENTS_FIELD = "__attachments";

    String PROJECT_ARG = "project";

    String BRANCH_ARG = "branch";

    String SITE_ARG = "siteKey";

    String SITE_HEADER = "X-Guillotine-SiteKey";

    String SITE_BASE_URL = "__siteBaseUrl";

    List<String> SUPPORTED_AGGREGATIONS =
        List.of( "terms", "stats", "range", "dateRange", "dateHistogram", "geoDistance", "min", "max", "count" );

}
