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

        // one set of params for the whole field, so that url = baseUrl + path + queryString:
        // the URL belongs to the site named by siteKey when there is one, and to the site of the
        // content otherwise
        final PageUrlParams params = buildParams( environment, content );

        final Map<String, Object> result = UrlPartsHelper.anyPagePartSelected( environment.getSelectionSet() )
            ? UrlPartsHelper.toMap( portalUrlService.pageUrlParts( params ) )
            : new LinkedHashMap<>();

        if ( environment.getSelectionSet().contains( "url" ) )
        {
            result.put( "url", portalUrlService.pageUrl( params ) );
        }

        return result;
    }

    private static PageUrlParams buildParams( final DataFetchingEnvironment environment, final Content content )
    {
        final PageUrlParams params = new PageUrlParams().id( content.getId().toString() )
            .base( GuillotineLocalContextHelper.getSiteBase( environment ) );

        if ( environment.getArgument( "params" ) instanceof Map<?, ?> queryParams )
        {
            queryParams.forEach( ( key, value ) -> params.param( key.toString(), value ) );
        }

        return params;
    }
}
