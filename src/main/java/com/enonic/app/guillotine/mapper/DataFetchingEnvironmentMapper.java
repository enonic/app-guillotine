package com.enonic.app.guillotine.mapper;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.dataloader.DataLoader;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public final class DataFetchingEnvironmentMapper
    implements MapSerializable
{
    private final DataFetchingEnvironment env;

    public DataFetchingEnvironmentMapper( final DataFetchingEnvironment env )
    {
        this.env = env;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        MapMapper.serializeKeyValue( gen, "source", this.env.getSource() );
        MapMapper.serializeKeyValue( gen, "args", this.env.getArguments() );
        MapMapper.serializeKeyValue( gen, "localContext", this.env.getLocalContext() );

        gen.rawValue( "loadFromDataLoader", (Function<ScriptValue, Object>) params -> {
            if ( params != null && params.isObject() )
            {
                Map<String, Object> input = params.getMap();
                DataLoader<Map<String, Object>, Object> dataLoader =
                    env.getDataLoader( Objects.requireNonNull( input.get( "dataLoader" ) ).toString() );
                return dataLoader.load( CastHelper.cast( input.get( "params" ) ) );
            }
            else
            {
                return null;
            }
        } );
    }
}
