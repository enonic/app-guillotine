package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import com.enonic.xp.portal.url.MediaUrlParts;
import com.enonic.xp.portal.url.PageUrlParts;

final class UrlPartsHelper
{
    private UrlPartsHelper()
    {
    }

    static Map<String, Object> toMap( final PageUrlParts parts )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", parts.getPath() );
        result.put( "queryString", parts.getQueryString() );
        return result;
    }

    static Map<String, Object> toMap( final MediaUrlParts parts )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", parts.getPath() );
        result.put( "queryString", parts.getQueryString() );
        result.put( "context", parts.getContext() );
        result.put( "id", parts.getId() );
        result.put( "hash", parts.getHash() );
        result.put( "scale", parts.getScale() );
        result.put( "name", parts.getName() );
        return result;
    }
}
