package com.enonic.app.guillotine.graphql.helper;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequest;

public final class PortalRequestHelper
{
    private PortalRequestHelper()
    {
    }

    public static PortalRequest createPortalRequest( final PortalRequest source, final DataFetchingEnvironment environment )
    {
        if ( source == null )
        {
            return null;
        }

        final PortalRequest result = createDefaultPortalRequest( source );
        result.setRepositoryId( GuillotineLocalContextHelper.getRepositoryId( environment, source.getRepositoryId() ) );
        result.setBranch( GuillotineLocalContextHelper.getBranch( environment, source.getBranch() ) );
        return result;
    }

    public static PortalRequest createPortalRequest( final PortalRequest source, final ApplicationKey applicationKey )
    {
        if ( source == null )
        {
            return null;
        }

        final PortalRequest result = createDefaultPortalRequest( source );
        result.setApplicationKey( applicationKey );
        return result;
    }

    private static PortalRequest createDefaultPortalRequest( final PortalRequest source )
    {
        final PortalRequest result = new PortalRequest();

        result.setRepositoryId( source.getRepositoryId() );
        result.setBranch( source.getBranch() );
        result.setApplicationKey( source.getApplicationKey() );
        result.setContentPath( source.getContentPath() );
        result.setMode( source.getMode() );
        result.setRawRequest( source.getRawRequest() );
        result.setContent( source.getContent() );
        result.setSite( source.getSite() );
        result.setMethod( source.getMethod() );
        result.setBaseUri( source.getBaseUri() );
        result.setRawPath( source.getRawPath() );
        result.setWebSocketContext( source.getWebSocketContext() );
        result.setComponent( source.getComponent() );
        result.setControllerScript( source.getControllerScript() );
        result.setPageDescriptor( source.getPageDescriptor() );
        result.setPageTemplate( source.getPageTemplate() );
        result.setContextPath( source.getContextPath() );
        result.setValidTicket( source.isValidTicket() );
        result.setBody( source.getBody() );
        result.setIdProvider( source.getIdProvider() );
        result.setHost( source.getHost() );
        result.setPort( source.getPort() );
        result.setScheme( source.getScheme() );
        result.setPath( source.getPath() );
        result.setEndpointPath( source.getEndpointPath() );
        result.setUrl( source.getUrl() );
        result.setContentType( source.getContentType() );
        result.setRemoteAddress( source.getRemoteAddress() );

        return result;
    }
}
