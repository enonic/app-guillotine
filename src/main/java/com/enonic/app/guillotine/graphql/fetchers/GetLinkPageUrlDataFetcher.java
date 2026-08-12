package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetLinkPageUrlDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private final PortalUrlService portalUrlService;

    public GetLinkPageUrlDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public Map<String, Object> get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Map<String, Object> sourceAsMap = environment.getSource();

        // contentId is only present on content links: media links have no page URL
        final Object contentId = sourceAsMap == null ? null : sourceAsMap.get( "contentId" );
        if ( contentId == null )
        {
            return null;
        }

        return GuillotineLocalContextHelper.executeInContext( environment, () -> {
            final Map<String, Object> result = UrlPartsHelper.anyPagePartSelected( environment.getSelectionSet() )
                ? UrlPartsHelper.toMap( portalUrlService.pageUrlParts( new PageUrlParams().id( contentId.toString() ) ) )
                : new LinkedHashMap<>();

            if ( environment.getSelectionSet().contains( "url" ) )
            {
                result.put( "url", portalUrlService.pageUrl( new PageUrlParams().id( contentId.toString() )
                                                                 .baseUrl( GuillotineLocalContextHelper.getSiteBaseUrl( environment ) ) ) );
            }

            return result;
        } );
    }
}
