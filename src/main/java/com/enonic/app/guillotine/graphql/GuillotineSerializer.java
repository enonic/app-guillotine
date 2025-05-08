package com.enonic.app.guillotine.graphql;

import java.util.Map;

import graphql.execution.DataFetcherResult;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mapper.ContentMapper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.PermissionsMapper;
import com.enonic.app.guillotine.mapper.SiteMapper;
import com.enonic.xp.content.Content;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.site.Site;
import com.enonic.xp.sortvalues.SortValuesProperty;

public final class GuillotineSerializer
{
    private GuillotineSerializer()
    {
    }

    public static Map<String, Object> serialize( final Content content )
    {
        if ( content == null )
        {
            return null;
        }

        final GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new ContentMapper( content ).serialize( generator );
        return CastHelper.cast( generator.getRoot() );
    }

    public static Map<String, Object> serialize( final Content content, final SortValuesProperty sort, final Float score )
    {
        if ( content == null )
        {
            return null;
        }

        final GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new ContentMapper( content, sort, score ).serialize( generator );
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

    public static Object serializePermissions( Content content )
    {
        if ( content == null )
        {
            return null;
        }

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new PermissionsMapper( content ).serialize( generator );
        return generator.getRoot();
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
            if ( scriptValue.getValue() instanceof DataFetcherResult )
            {
                return scriptValue.getValue();
            }
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
