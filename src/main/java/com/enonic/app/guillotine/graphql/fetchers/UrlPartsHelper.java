package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import com.enonic.xp.portal.url.AttachmentUrlParts;
import com.enonic.xp.portal.url.ImageUrlParts;
import com.enonic.xp.portal.url.PageUrlParts;

final class UrlPartsHelper
{
    private UrlPartsHelper()
    {
    }

    static Map<String, Object> toMap( final PageUrlParts parts )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", parts.path() );
        result.put( "queryString", parts.queryString() );
        return result;
    }

    static Map<String, Object> toMap( final ImageUrlParts parts )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", parts.path() );
        result.put( "queryString", parts.queryString() );
        result.put( "context", parts.context() );
        result.put( "id", parts.id() );
        result.put( "fingerprint", parts.fingerprint() );
        result.put( "scale", parts.scale() );
        result.put( "name", parts.name() );
        return result;
    }

    static Map<String, Object> toMap( final AttachmentUrlParts parts, final String intent )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", parts.path() );
        result.put( "queryString", parts.queryString() );
        result.put( "context", parts.context() );
        result.put( "id", parts.id() );
        result.put( "fingerprint", parts.fingerprint() );
        result.put( "name", parts.name() );
        result.put( "intent", intent );
        return result;
    }
}
