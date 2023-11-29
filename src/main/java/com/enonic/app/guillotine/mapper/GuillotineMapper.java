package com.enonic.app.guillotine.mapper;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.transformer.GuillotineUtil;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class GuillotineMapper
    implements MapSerializable
{
    private final GuillotineUtil guillotineUtil;

    public GuillotineMapper( final GuillotineUtil guillotineUtil )
    {
        this.guillotineUtil = guillotineUtil;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.rawValue( "getSite", (Function<ScriptValue, Object>) scriptValue -> {
            Map<String, Object> params = scriptValue.getMap();

            String key = Objects.requireNonNull( CastHelper.cast( params.get( "key" ) ), "The \"key\" parameter is required." );
            String project = CastHelper.cast( params.get( "project" ) );
            String branch = CastHelper.cast( params.get( "branch" ) );

            return guillotineUtil.getSite( key, project, branch );
        } );

        gen.rawValue( "getContent", (Function<ScriptValue, Object>) scriptValue -> {
            Map<String, Object> params = scriptValue.getMap();

            String key = Objects.requireNonNull( CastHelper.cast( params.get( "key" ) ), "The \"key\" parameter is required." );
            String project = CastHelper.cast( params.get( "project" ) );
            String branch = CastHelper.cast( params.get( "branch" ) );

            return guillotineUtil.getContent( key, project, branch );
        } );

        gen.rawValue( "query", (Function<ScriptValue, Object>) scriptValue -> {
            Map<String, Object> inputParams = scriptValue.getMap();

            Map<String, Object> params = CastHelper.cast( inputParams.get( "params" ) );
            String project = CastHelper.cast( inputParams.get( "project" ) );
            String branch = CastHelper.cast( inputParams.get( "branch" ) );

            return guillotineUtil.query( params, project, branch );
        } );
    }
}
