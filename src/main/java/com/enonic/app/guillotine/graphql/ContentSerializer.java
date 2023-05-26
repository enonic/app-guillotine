package com.enonic.app.guillotine.graphql;

import java.util.Map;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mapper.ContentMapper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.SiteMapper;
import com.enonic.xp.content.Content;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.site.Site;

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

    public static Map<String, Object> serialize( Site site )
    {
        if ( site == null )
        {
            return null;
        }

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new SiteMapper( site ).serialize( generator );

        Map<String, Object> result = CastHelper.cast( generator.getRoot() );
        if ( result.get( "data" ) != null )
        {
            Map<String, Object> data = CastHelper.cast( result.get( "data" ) );
            data.remove( "siteConfig" );
        }

        return result;
    }

    public static Object serialize( ScriptValue scriptValue )
    {
        if ( scriptValue == null )
        {
            return null;
        }
        else if ( scriptValue.isArray() )
        {
            return scriptValue.getList();
        }
        else if ( scriptValue.isObject() )
        {
            return scriptValue.getMap();
        }
        else if ( scriptValue.isValue() )
        {
            return scriptValue.getValue();
        }
        else
        {
            return null;
        }
    }
}
