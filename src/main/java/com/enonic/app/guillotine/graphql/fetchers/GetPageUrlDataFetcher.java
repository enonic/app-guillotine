package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetPageUrlDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetPageUrlDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        PortalRequest portalRequest = PortalRequestAccessor.get();

        Map<String, Object> sourceAsMap = environment.getSource();

        PageUrlParams params =
            new PageUrlParams().id( sourceAsMap.get( "_id" ).toString() ).type( environment.getArgument( "type" ) ).portalRequest(
                portalRequest ); // TODO include params

        return portalUrlService.pageUrl( params );
    }
}
