package com.enonic.app.guillotine.graphql;

import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.xp.content.Content;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class UrlHelper
{

    public static String resolveAttachmentUrlById( DataFetchingEnvironment environment, PortalUrlService portalUrlService )
    {
        Content source = environment.getSource();
        PortalRequest portalRequest = environment.getLocalContext();

        AttachmentUrlParams params = new AttachmentUrlParams().id( source.getId().toString() ).download(
            Objects.toString( environment.getArgument( "download" ), "false" ) ).type( environment.getArgument( "type" ) ).portalRequest(
            portalRequest );

        return portalUrlService.attachmentUrl( params );
    }

    public static String resolveAttachmentUrlByName( DataFetchingEnvironment environment, PortalUrlService portalUrlService )
    {
        AttachmentProxy source = environment.getSource();
        PortalRequest portalRequest = environment.getLocalContext();

        AttachmentUrlParams params = new AttachmentUrlParams().id( source.getContentId().toString() ).name( source.getName() ).download(
            Objects.toString( environment.getArgument( "download" ), "false" ) ).type( environment.getArgument( "type" ) ).portalRequest(
            portalRequest );

        return portalUrlService.attachmentUrl( params );
    }
}
