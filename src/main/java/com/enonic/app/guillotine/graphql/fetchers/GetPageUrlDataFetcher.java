package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetPageUrlDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private final PortalUrlService portalUrlService;

    public GetPageUrlDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public Map<String, Object> get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Map<String, Object> doGet( final DataFetchingEnvironment environment )
    {
        final Content content = GuillotineLocalContextHelper.resolveContent( environment );

        if ( content == null )
        {
            return null;
        }

        final Map<String, Object> result = UrlPartsHelper.anyPagePartSelected( environment.getSelectionSet() )
            ? UrlPartsHelper.toMap( portalUrlService.pageUrlParts( buildParams( environment, content, null ) ) )
            : new LinkedHashMap<>();

        if ( environment.getSelectionSet().contains( "url" ) )
        {
            // same call as content link processing in processHtml: the siteKey-resolved base URL
            // when present, otherwise request/context resolution (no project/branch on the params,
            // so the URL follows the site request when there is one)
            result.put( "url", portalUrlService.pageUrl(
                buildParams( environment, content, GuillotineLocalContextHelper.getSiteBaseUrl( environment ) ) ) );
        }

        return result;
    }

    private static PageUrlParams buildParams( final DataFetchingEnvironment environment, final Content content, final String baseUrl )
    {
        final PageUrlParams params = new PageUrlParams().id( content.getId().toString() ).baseUrl( baseUrl );

        if ( environment.getArgument( "params" ) instanceof Map<?, ?> queryParams )
        {
            queryParams.forEach( ( key, value ) -> params.param( key.toString(), value ) );
        }

        return params;
    }
}
