package com.enonic.app.guillotine.url;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;

import static com.google.common.base.Strings.isNullOrEmpty;

public class UrlBuilderBase
{
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
