package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;


/**
 * The page URL parts of a rich text link to a content, as XP resolved them while processing the text for the content
 * {@code siteKey} names, or for the project without one.
 */
public class GetLinkPageUrlDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> get( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> sourceAsMap = environment.getSource();

        // pageUrl is only present on content links: media links have no page URL
        final Object pageUrl = sourceAsMap == null ? null : sourceAsMap.get( "pageUrl" );
        if ( pageUrl == null )
        {
            return null;
        }

        return (Map<String, Object>) pageUrl;
    }
}
