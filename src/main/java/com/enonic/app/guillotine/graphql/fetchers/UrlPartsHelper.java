package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import com.enonic.xp.portal.url.MediaUrlComponents;
import com.enonic.xp.portal.url.PageUrlComponents;

final class UrlPartsHelper
{
    private UrlPartsHelper()
    {
    }

    static Map<String, Object> toMap( final PageUrlComponents components )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", components.getPath() );
        result.put( "queryString", components.getQueryString() );
        return result;
    }

    static Map<String, Object> toMap( final MediaUrlComponents components )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", components.getPath() );
        result.put( "queryString", components.getQueryString() );
        result.put( "context", components.getContext() );
        result.put( "id", components.getId() );
        result.put( "hash", components.getHash() );
        result.put( "scale", components.getScale() );
        result.put( "name", components.getName() );
        return result;
    }
}
