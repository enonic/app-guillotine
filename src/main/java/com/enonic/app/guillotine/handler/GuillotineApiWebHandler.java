package com.enonic.app.guillotine.handler;

import com.enonic.app.guillotine.GuillotineConfig;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalResponse;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebRequest;
import com.enonic.xp.web.WebResponse;
import com.enonic.xp.web.handler.BaseWebHandler;
import com.enonic.xp.web.handler.WebHandler;
import com.enonic.xp.web.handler.WebHandlerChain;
import com.enonic.xp.web.websocket.WebSocketConfig;
import com.enonic.xp.web.websocket.WebSocketContext;
import com.enonic.xp.web.websocket.WebSocketEndpoint;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(immediate = true, service = WebHandler.class, configurationPid = "com.enonic.app.guillotine")
public class GuillotineApiWebHandler
    extends BaseWebHandler
{
    private static final ApplicationKey APPLICATION_KEY = ApplicationKey.from( "com.enonic.app.guillotine" );

    private final Pattern urlPattern;

    private final ControllerScriptFactory controllerScriptFactory;

    @Activate
    public GuillotineApiWebHandler( final @Reference ControllerScriptFactory controllerScriptFactory, final GuillotineConfig config )
    {
        super( -49 );
        this.controllerScriptFactory = controllerScriptFactory;
        this.urlPattern = Pattern.compile( "^/(admin/site/preview|site)/(([a-z0-9\\-:])([a-z0-9_\\-.:])*)/(([a-z0-9\\-:])([a-z0-9_\\-.:])*)" + config.endpoint_postfix_regex() + "(/*)$" );
    }

    @Override
    protected boolean canHandle( final WebRequest webRequest )
    {
        final String path = webRequest.getRawPath();
        final Matcher matcher = urlPattern.matcher( path );
        return ( webRequest.getMethod() == HttpMethod.POST || ( webRequest.getMethod() == HttpMethod.GET && webRequest.isWebSocket() ) ) &&
            matcher.matches();
    }

    @Override
    protected WebResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        final PortalRequest portalRequest = castToPortalRequest( webRequest );
        portalRequest.setContextPath( portalRequest.getBaseUri() );
        portalRequest.setApplicationKey( APPLICATION_KEY );

        final ResourceKey script = ResourceKey.from( APPLICATION_KEY, "api/api.js" );
        final ControllerScript controllerScript = controllerScriptFactory.fromScript( script );

        final PortalResponse portalResponse = controllerScript.execute( portalRequest );

        final WebSocketConfig webSocketConfig = portalResponse.getWebSocket();
        final WebSocketContext webSocketContext = portalRequest.getWebSocketContext();

        if ( webSocketContext != null && webSocketConfig != null )
        {
            WebSocketEndpoint webSocketEndpoint = new WebSocketEndpointImpl( webSocketConfig, () -> controllerScript );
            webSocketContext.apply( webSocketEndpoint );
        }

        return portalResponse;
    }

    protected PortalRequest castToPortalRequest( final WebRequest webRequest )
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
        return portalRequest;
    }
}
