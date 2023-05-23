package com.enonic.app.guillotine.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.enonic.app.guillotine.GuillotineConfig;
import com.enonic.app.guillotine.QueryPlaygroundUIMode;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.repository.RepositoryUtils;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebRequest;
import com.enonic.xp.web.WebResponse;
import com.enonic.xp.web.handler.BaseWebHandler;
import com.enonic.xp.web.handler.WebHandler;
import com.enonic.xp.web.handler.WebHandlerChain;

@Component(immediate = true, service = WebHandler.class, configurationPid = "com.enonic.app.guillotine")
public class QueryPlaygroundWebHandler
    extends BaseWebHandler
{
    private static final Pattern URL_PATTERN = Pattern.compile( "^/site/(([a-z0-9\\-:])([a-z0-9_\\-.:])*)(:?$|/_static/.+?)" );

    private static final ApplicationKey APPLICATION_KEY = ApplicationKey.from( "com.enonic.app.guillotine" );

    private static final ApplicationKey WELCOME_APP_KEY = ApplicationKey.from( "com.enonic.xp.app.welcome" );

    private final ControllerScriptFactory controllerScriptFactory;

    private final ApplicationService applicationService;

    private QueryPlaygroundUIMode queryPlaygroundUIMode;

    @Activate
    public QueryPlaygroundWebHandler( final @Reference ControllerScriptFactory controllerScriptFactory,
                                      final @Reference ApplicationService applicationService )
    {
        super( -51 );
        this.controllerScriptFactory = controllerScriptFactory;
        this.applicationService = applicationService;
    }

    @Activate
    @Modified
    public void activate( final GuillotineConfig config )
    {
        this.queryPlaygroundUIMode = QueryPlaygroundUIMode.from( config.queryplayground_ui_mode() );
    }

    @Override
    protected boolean canHandle( final WebRequest webRequest )
    {
        String path = webRequest.getRawPath();
        boolean isSDK = applicationService.get( WELCOME_APP_KEY ) != null;
        boolean shouldBeHandled = isSDK
            ? ( queryPlaygroundUIMode == QueryPlaygroundUIMode.ON || queryPlaygroundUIMode == QueryPlaygroundUIMode.AUTO )
            : queryPlaygroundUIMode == QueryPlaygroundUIMode.ON;
        return webRequest.getMethod() == HttpMethod.GET && !webRequest.isWebSocket() && hasAdminRight( webRequest ) && shouldBeHandled &&
            URL_PATTERN.matcher( path ).matches();
    }

    @Override
    protected WebResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        final Matcher matcher = URL_PATTERN.matcher( webRequest.getRawPath() );
        matcher.matches();

        final PortalRequest portalRequest = castToPortalRequest( webRequest );
        portalRequest.setContextPath( portalRequest.getBaseUri() );
        portalRequest.setApplicationKey( APPLICATION_KEY );
        portalRequest.setRepositoryId( RepositoryUtils.fromContentRepoName( matcher.group( 1 ) ) );

        final ResourceKey script = ResourceKey.from( APPLICATION_KEY, "graphiql/graphiql.js" );
        final ControllerScript controllerScript = controllerScriptFactory.fromScript( script );

        return controllerScript.execute( portalRequest );
    }

    private static boolean hasAdminRight( final WebRequest webRequest )
    {
        return webRequest.getRawRequest().isUserInRole( RoleKeys.CONTENT_MANAGER_ADMIN_ID ) ||
            webRequest.getRawRequest().isUserInRole( RoleKeys.ADMIN_ID );
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
