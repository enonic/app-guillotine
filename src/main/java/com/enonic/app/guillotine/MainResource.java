package com.enonic.app.guillotine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.jaxrs.JaxRsComponent;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalResponse;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.web.HttpMethod;

@Path("/api")
@Component(immediate = true, property = "group=guillotine",  enabled = false)
public class MainResource
    implements JaxRsComponent
{
    private final ControllerScriptFactory controllerScriptFactory;

    @Activate
    public MainResource( final @Reference ControllerScriptFactory controllerScriptFactory )
    {
        this.controllerScriptFactory = controllerScriptFactory;
    }

    @GET
    public PortalResponse headless( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse )
    {
        ApplicationKey applicationKey = ApplicationKey.from( "com.enonic.app.guillotine" );

        PortalRequest portalRequest = new PortalRequest();
        portalRequest.setMethod( HttpMethod.GET );
        portalRequest.setRawRequest( httpServletRequest );
        portalRequest.setApplicationKey( applicationKey );

        ResourceKey script = ResourceKey.from( applicationKey, "headless/headless.js" );
        ControllerScript controllerScript = controllerScriptFactory.fromScript( script );

        return controllerScript.execute( portalRequest );
    }
}
