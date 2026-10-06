package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.portal.url.PageUrlPartsParams;
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

        return UrlPartsHelper.toMap( portalUrlService.pageUrlParts( buildParams( environment, content ) ) );
    }

    private PageUrlPartsParams buildParams( final DataFetchingEnvironment environment, final Content content )
    {
        final PageUrlPartsParams.Builder params = PageUrlPartsParams.create()
            .setId( content.getId().toString() )
            .setScope( GuillotineLocalContextHelper.getPortalScope( environment, portalUrlService ) );

        if ( environment.getArgument( "params" ) instanceof Map<?, ?> queryParams )
        {
            params.setQueryParams( ParamsUrHelper.convertToMultimap( CastHelper.cast( queryParams ) ) );
        }

        return params.build();
    }
}
