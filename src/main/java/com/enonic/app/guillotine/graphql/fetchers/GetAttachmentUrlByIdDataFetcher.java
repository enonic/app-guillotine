package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetAttachmentUrlByIdDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetAttachmentUrlByIdDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        PortalRequest portalRequest = PortalRequestAccessor.get();

        Map<String, Object> sourceAsMap = environment.getSource();

        AttachmentUrlParams params = new AttachmentUrlParams().id( sourceAsMap.get( "_id" ).toString() ).download(
            Objects.toString( environment.getArgument( "download" ), "false" ) ).type( environment.getArgument( "type" ) ).portalRequest(
            portalRequest ); // TODO include params

        return portalUrlService.attachmentUrl( params );
    }
}
