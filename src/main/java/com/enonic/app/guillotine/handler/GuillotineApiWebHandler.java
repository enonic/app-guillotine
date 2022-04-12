package com.enonic.app.guillotine.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebRequest;
import com.enonic.xp.web.WebResponse;
import com.enonic.xp.web.handler.BaseWebHandler;
import com.enonic.xp.web.handler.WebHandler;
import com.enonic.xp.web.handler.WebHandlerChain;

@Component(immediate = true, service = WebHandler.class)
public class GuillotineApiWebHandler
    extends BaseWebHandler
{
    private static final ApplicationKey APPLICATION_KEY = ApplicationKey.from( "com.enonic.app.guillotine" );

    private final ControllerScriptFactory controllerScriptFactory;

    @Activate
    public GuillotineApiWebHandler( final @Reference ControllerScriptFactory controllerScriptFactory )
    {
        super( -49 );
        this.controllerScriptFactory = controllerScriptFactory;
    }

    @Override
    protected boolean canHandle( final WebRequest webRequest )
    {
        final String rawPath = webRequest.getRawPath();
        return webRequest.getMethod() == HttpMethod.POST && !rawPath.contains( "/_/" ) &&
            ( rawPath.startsWith( "/admin/site" ) || rawPath.startsWith( "/site" ) );
    }

    @Override
    protected WebResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        PortalRequest portalRequest;
        if ( webRequest instanceof PortalRequest )
        {
            portalRequest = (PortalRequest) webRequest;
        }
        else
        {
            portalRequest = new PortalRequest( webRequest );
        }

        portalRequest.setContextPath( portalRequest.getBaseUri() );
        portalRequest.setApplicationKey( APPLICATION_KEY );

        ResourceKey scriptDir = ResourceKey.from( APPLICATION_KEY, "graphql" );
        ControllerScript controllerScript = controllerScriptFactory.fromDir( scriptDir );
        return controllerScript.execute( portalRequest );
    }
}
