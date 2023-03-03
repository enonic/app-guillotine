package com.enonic.app.guillotine.url;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;

import com.enonic.xp.portal.url.UrlTypeConstants;
import com.enonic.xp.web.servlet.ServletRequestUrlHelper;
import com.enonic.xp.web.servlet.UriRewritingResult;

import static com.google.common.base.Strings.isNullOrEmpty;

public class UrlBuilderBase
{
    protected final String rewriteUrl( String url, String type, HttpServletRequest rawRequest )
    {
        final UriRewritingResult rewritingResult = ServletRequestUrlHelper.rewriteUri( rawRequest, url );

        String uri = rewritingResult.getRewrittenUri();

        if ( UrlTypeConstants.ABSOLUTE.equals( type ) )
        {
            return ServletRequestUrlHelper.getServerUrl( rawRequest ) + uri;
        }
        else if ( UrlTypeConstants.WEBSOCKET.equals( type ) )
        {
            return ServletRequestUrlHelper.getServerUrl( new HttpServletRequestWrapper( rawRequest )
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

}
