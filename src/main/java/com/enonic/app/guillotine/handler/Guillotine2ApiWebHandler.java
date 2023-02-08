package com.enonic.app.guillotine.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalResponse;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.repository.RepositoryId;
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

@Component(immediate = true, service = WebHandler.class)
public class Guillotine2ApiWebHandler
    extends BaseWebHandler
{
    private static final Pattern URL_PATTERN = Pattern.compile( "^(/api)([/]*)$" );

    private static final String X_GUILLOTINE_PROJECT_HEADER = "X-Guillotine-Project";

    private static final String X_GUILLOTINE_BRANCH_HEADER = "X-Guillotine-Branch";

    private static final ApplicationKey APPLICATION_KEY = ApplicationKey.from( "com.enonic.app.guillotine" );

    private final ControllerScriptFactory controllerScriptFactory;

    @Activate
    public Guillotine2ApiWebHandler( final @Reference ControllerScriptFactory controllerScriptFactory )
    {
        super( -49 );
        this.controllerScriptFactory = controllerScriptFactory;
    }

    @Override
    protected boolean canHandle( final WebRequest webRequest )
    {
        final String path = webRequest.getRawPath();
        final Matcher matcher = URL_PATTERN.matcher( path );
        return ( webRequest.getMethod() == HttpMethod.POST || ( webRequest.getMethod() == HttpMethod.GET && webRequest.isWebSocket() ) ) &&
            matcher.matches() && webRequest.getHeaders().get( X_GUILLOTINE_PROJECT_HEADER ) != null &&
            webRequest.getHeaders().get( X_GUILLOTINE_BRANCH_HEADER ) != null;
    }

    @Override
    protected WebResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        final PortalRequest portalRequest = castToPortalRequest( webRequest );
        portalRequest.setContextPath( portalRequest.getBaseUri() );
        portalRequest.setApplicationKey( APPLICATION_KEY );

        final ResourceKey script = ResourceKey.from( APPLICATION_KEY, "graphql/graphql.js" );
        final ControllerScript controllerScript = controllerScriptFactory.fromScript( script );

        return createContext( webRequest ).callWith( () -> {
            final PortalResponse portalResponse = controllerScript.execute( portalRequest );
            final WebSocketConfig webSocketConfig = portalResponse.getWebSocket();
            final WebSocketContext webSocketContext = portalRequest.getWebSocketContext();
            if ( webSocketContext != null && webSocketConfig != null )
            {
                WebSocketEndpoint webSocketEndpoint = new WebSocketEndpointImpl( webSocketConfig, () -> controllerScript );
                webSocketContext.apply( webSocketEndpoint );
            }
            return portalResponse;
        } );
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

    private static Context createContext( final WebRequest webRequest )
    {
        return ContextBuilder.from( ContextAccessor.current() ).
            repositoryId(RepositoryId.from( "com.enonic.cms." + webRequest.getHeaders().get( X_GUILLOTINE_PROJECT_HEADER ) ) ).
            branch(Branch.from( webRequest.getHeaders().get( X_GUILLOTINE_BRANCH_HEADER ) ) ).
            build();
    }

}
