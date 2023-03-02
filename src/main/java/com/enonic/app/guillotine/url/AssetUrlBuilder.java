package com.enonic.app.guillotine.url;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.portal.url.UrlTypeConstants;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.server.RunMode;
import com.enonic.xp.web.servlet.ServletRequestUrlHelper;
import com.enonic.xp.web.servlet.UriRewritingResult;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AssetUrlBuilder
{
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final AssetUrlParams params;

    private final ResourceService resourceService;

    public AssetUrlBuilder( final AssetUrlParams params, final ResourceService resourceService )
    {
        this.params = params;
        this.resourceService = resourceService;
    }

    public String buildUrl()
    {
        final StringBuilder str = new StringBuilder();
        appendPart( str, params.getPortalRequest().getBaseUri() );

        final ApplicationKey applicationKey = resolveApplication();

        final Resource resource = this.resourceService.getResource( ResourceKey.from( applicationKey, "META-INF/MANIFEST.MF" ) );
        if ( !resource.exists() )
        {
            throw new IllegalArgumentException( "Could not find application [" + applicationKey + "]" );
        }

        final String fingerprint = RunMode.get() == RunMode.DEV ? String.valueOf( stableTime() ) : toHex( resource.getTimestamp() );

        appendPart( str, applicationKey + ":" + fingerprint );
        appendPart( str, this.params.getPath() );

        final UriRewritingResult rewritingResult =
            ServletRequestUrlHelper.rewriteUri( params.getPortalRequest().getRawRequest(), str.toString() );

        final String uri = rewritingResult.getRewrittenUri();

        if ( UrlTypeConstants.ABSOLUTE.equals( params.getType() ) )
        {
            return ServletRequestUrlHelper.getServerUrl( params.getPortalRequest().getRawRequest() ) + uri;
        }
        else if ( UrlTypeConstants.WEBSOCKET.equals( params.getType() ) )
        {
            return ServletRequestUrlHelper.getServerUrl( new HttpServletRequestWrapper( params.getPortalRequest().getRawRequest() )
            {
                @Override
                public String getScheme()
                {
                    return isSecure() ? "wss" : "ws";
                }
            } ) + uri;
        }
        else
        {
            return uri;
        }
    }

    protected final void appendPart( final StringBuilder str, final String urlPart )
    {
        if ( isNullOrEmpty( urlPart ) )
        {
            return;
        }

        final boolean endsWithSlash = ( str.length() > 0 ) && ( str.charAt( str.length() - 1 ) == '/' );
        final String normalized = normalizePath( urlPart );

        if ( !endsWithSlash )
        {
            str.append( "/" );
        }

        str.append( normalized );
    }

    private String urlEncodePathSegment( final String value )
    {
        return UrlEscapers.urlPathSegmentEscaper().escape( value );
    }

    private String normalizePath( final String value )
    {
        if ( value == null )
        {
            return null;
        }

        if ( !value.contains( "/" ) )
        {
            return urlEncodePathSegment( value );
        }

        return StreamSupport.stream( Splitter.on( '/' ).trimResults().omitEmptyStrings().split( value ).spliterator(), false ).map(
            this::urlEncodePathSegment ).collect( Collectors.joining( "/" ) );
    }

    public ApplicationKey resolveApplication()
    {
        if ( params.getApplication() != null )
        {
            return ApplicationKey.from( params.getApplication() );
        }

        return params.getPortalRequest().getApplicationKey();
    }

    private static long stableTime()
    {
        final Long localScopeTime = (Long) ContextAccessor.current().getLocalScope().getAttribute( "__currentTimeMillis" );
        return Objects.requireNonNullElse( localScopeTime, System.currentTimeMillis() );
    }

    public static String toHex( final long value )
    {
        final char[] buffer = new char[Long.BYTES * 2];
        for ( int i = 0; i < Long.BYTES; i++ )
        {
            final byte v = (byte) ( value >>> ( Long.SIZE - ( i + 1 ) * Byte.SIZE ) );
            buffer[2 * i] = HEX[( v >>> 4 ) & 0x0f];
            buffer[2 * i + 1] = HEX[v & 0x0f];
        }
        return new String( buffer );
    }

}
