package com.enonic.app.guillotine.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSortedSet;

import com.enonic.xp.index.IndexConfig;
import com.enonic.xp.index.IndexConfigDocument;
import com.enonic.xp.index.IndexValueProcessor;
import com.enonic.xp.index.PathIndexConfig;
import com.enonic.xp.index.PatternIndexConfigDocument;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

class IndexConfigDocMapper
    implements MapSerializable
{
    private final IndexConfigDocument value;

    IndexConfigDocMapper( final IndexConfigDocument value )
    {
        this.value = value;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        serialize( gen, this.value );
    }

    private void serialize( final MapGenerator gen, final IndexConfigDocument document )
    {
        gen.value( "analyzer", value.getAnalyzer() );

        if ( document instanceof PatternIndexConfigDocument )
        {
            serialize( gen, (PatternIndexConfigDocument) document );
        }
    }

    private void serialize( final MapGenerator gen, final PatternIndexConfigDocument document )
    {
        gen.map( "default" );
        serialize( gen, document.getDefaultConfig() );
        gen.end();

        gen.array( "configs" );

        final ImmutableSortedSet<PathIndexConfig> pathIndexConfigs = document.getPathIndexConfigs();

        for ( final PathIndexConfig pathIndexConfig : pathIndexConfigs )
        {
            gen.map();
            gen.value( "path", pathIndexConfig.getPath().toString() );
            gen.map( "config" );
            serialize( gen, pathIndexConfig.getIndexConfig() );
            gen.end();
            gen.end();
        }

        gen.end();
    }

    private void serialize( final MapGenerator gen, final IndexConfig indexConfig )
    {
        gen.value( "decideByType", indexConfig.isDecideByType() );
        gen.value( "enabled", indexConfig.isEnabled() );
        gen.value( "nGram", indexConfig.isnGram() );
        gen.value( "fulltext", indexConfig.isFulltext() );
        gen.value( "includeInAllText", indexConfig.isIncludeInAllText() );
        gen.value( "path", indexConfig.isPath() );

        final List<IndexValueProcessor> indexValueProcessors = indexConfig.getIndexValueProcessors();

        serializeArray( gen, "indexValueProcessors",
                        indexValueProcessors.stream().map( IndexValueProcessor::getName ).collect( Collectors.toList() ) );

        final List<String> languages = indexConfig.getLanguages();

        serializeArray( gen, "languages", languages );
    }

    private void serializeArray( final MapGenerator gen, final String name, final List<String> values )
    {
        gen.array( name );
        for ( final String value : values )
        {
            gen.value( value );
        }
        gen.end();
    }
}
