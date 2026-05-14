package com.enonic.app.guillotine.handler;

import java.util.EnumSet;
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
import com.enonic.xp.portal.RenderMode;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.project.ProjectName;
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
public class GuillotineApiWebHandler
    extends BaseWebHandler
{
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^/(admin/com.enonic.app.contentstudio/main/_/admin:extension/com.enonic.app.guillotine:guillotine|site)/(?!_static/)(?<project>[^/]+)/(?<branch>[^/]+)(/?)$" );

    private static final ApplicationKey APPLICATION_KEY = ApplicationKey.from( "com.enonic.app.guillotine" );

    private final ControllerScriptFactory controllerScriptFactory;

    @Activate
    public GuillotineApiWebHandler( final @Reference ControllerScriptFactory controllerScriptFactory )
    {
        super( -49, EnumSet.of( HttpMethod.POST, HttpMethod.GET, HttpMethod.OPTIONS ) );
        this.controllerScriptFactory = controllerScriptFactory;
    }

    @Override
    protected boolean canHandle( final WebRequest webRequest )
    {
        return URL_PATTERN.matcher( webRequest.getRawPath() ).matches();
    }

    @Override
    protected WebResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        final Matcher matcher = URL_PATTERN.matcher( webRequest.getRawPath() );
        matcher.matches();

        final RepositoryId repositoryId = ProjectName.from( matcher.group( "project" ) ).getRepoId();
        final Branch branch = Branch.from( matcher.group( "branch" ) );

        final PortalRequest portalRequest = castToPortalRequest( webRequest );
        portalRequest.setRepositoryId( repositoryId );
        portalRequest.setBranch( branch );
        portalRequest.setApplicationKey( APPLICATION_KEY );

        if ( webRequest.getRawPath().startsWith( "/site/" ) )
        {
            portalRequest.setContextPath( portalRequest.getBaseUri() );
            portalRequest.setMode( RenderMode.LIVE );
        }
        else
        {
            final String baseUri = "/admin/com.enonic.app.contentstudio/site/inline";
            portalRequest.setMode( RenderMode.INLINE );
            portalRequest.setBaseUri( baseUri );
            portalRequest.setContextPath( baseUri );
        }

        final ResourceKey script = ResourceKey.from( APPLICATION_KEY, "api/api.js" );
        final ControllerScript controllerScript = controllerScriptFactory.fromScript( script );

        final Context xpContext = ContextBuilder.copyOf( ContextAccessor.current() ).repositoryId( repositoryId ).branch( branch ).build();
        final PortalResponse portalResponse = xpContext.callWith( () -> controllerScript.execute( portalRequest ) );

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
