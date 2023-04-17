package com.enonic.app.guillotine.graphql;

import java.util.Map;

import com.enonic.app.guillotine.mapper.ContentMapper;
import com.enonic.xp.content.Content;

public final class ContentSerializer
{
    public static Map<String, Object> serialize( Content content )
    {
        if ( content == null )
        {
            return null;
        }

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new ContentMapper( content ).serialize( generator );
        return CastHelper.cast( generator.getRoot() );
    }
}
