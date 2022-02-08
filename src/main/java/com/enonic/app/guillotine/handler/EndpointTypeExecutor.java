package com.enonic.app.guillotine.handler;

import java.util.concurrent.Callable;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;

public class EndpointTypeExecutor
{
    public static <T> T execute( PortalRequest portalRequest, String endpointType, Callable<T> callable )
    {
        String projectBranchTuple[] = getProjectBranchTuple( portalRequest, endpointType );

        return ContextBuilder.create().
            repositoryId( "com.enonic.cms." + projectBranchTuple[0] ).
            branch( projectBranchTuple[1] ).
            authInfo( ContextAccessor.current().getAuthInfo() ).
            build().
            callWith( callable );
    }

    private static String[] getProjectBranchTuple( final PortalRequest portalRequest, final String endpointType )
    {
        String baseUri = portalRequest.getBaseUri();
        String url = portalRequest.getUrl();

        int start = url.indexOf( baseUri ) + baseUri.length() + 1;
        int offset = url.indexOf( "/_/" + endpointType );

        return url.substring( start, offset ).split( "/", -1 );
    }
}
