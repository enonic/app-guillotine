package com.enonic.app.guillotine.handler.attachment;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.enonic.app.guillotine.PortalConfig;
import com.enonic.app.guillotine.handler.EndpointTypeExecutor;
import com.enonic.xp.annotation.Order;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalResponse;
import com.enonic.xp.portal.handler.EndpointHandler;
import com.enonic.xp.portal.handler.WebHandlerHelper;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebException;
import com.enonic.xp.web.WebRequest;
import com.enonic.xp.web.WebResponse;
import com.enonic.xp.web.handler.WebHandler;
import com.enonic.xp.web.handler.WebHandlerChain;

@Component(immediate = true, service = WebHandler.class, configurationPid = "com.enonic.xp.portal")
@Order(99)
public final class AttachmentHandler
    extends EndpointHandler
{
    private static final Pattern PATTERN = Pattern.compile( "([^/]+)/([^/^:]+)(?::([^/]+))?/([^/]+)" );

    private ContentService contentService;

    private volatile String privateCacheControlHeaderConfig;

    private volatile String publicCacheControlHeaderConfig;

    public AttachmentHandler()
    {
        super( EnumSet.of( HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS ), "attachment" );
    }

    @Activate
    @Modified
    public void activate( final PortalConfig config )
    {
        privateCacheControlHeaderConfig = config.media_private_cacheControl();
        publicCacheControlHeaderConfig = config.media_public_cacheControl();
    }

    @Override
    public boolean canHandle( final WebRequest webRequest )
    {
        return super.canHandle( webRequest ) && webRequest instanceof PortalRequest;
    }

    @Override
    protected PortalResponse doHandle( final WebRequest webRequest, final WebResponse webResponse, final WebHandlerChain webHandlerChain )
        throws Exception
    {
        WebHandlerHelper.checkAdminAccess( webRequest );

        final String restPath = findRestPath( webRequest );
        final Matcher matcher = PATTERN.matcher( restPath );

        if ( !matcher.find() )
        {
            throw WebException.notFound( "Not a valid attachment url pattern" );
        }

        return EndpointTypeExecutor.execute( (PortalRequest) webRequest, "attachment", () -> {
            final AttachmentHandlerWorker worker = new AttachmentHandlerWorker( (PortalRequest) webRequest );
            worker.contentService = this.contentService;
            worker.download = "download".equals( matcher.group( 1 ) );
            worker.id = ContentId.from( matcher.group( 2 ) );
            worker.fingerprint = matcher.group( 3 );
            worker.name = matcher.group( 4 );
            worker.privateCacheControlHeaderConfig = this.privateCacheControlHeaderConfig;
            worker.publicCacheControlHeaderConfig = this.publicCacheControlHeaderConfig;
            return worker.execute();
        } );
    }

    @Reference
    public void setContentService( final ContentService contentService )
    {
        this.contentService = contentService;
    }

}