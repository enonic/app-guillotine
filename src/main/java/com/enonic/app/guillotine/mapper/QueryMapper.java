package com.enonic.app.guillotine.mapper;

import java.util.Map;

import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.highlight.HighlightedProperties;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;
import com.enonic.xp.sortvalues.SortValuesProperty;

public final class QueryMapper
    implements MapSerializable
{
    private final Contents contents;

    private final long total;

    private final Aggregations aggregations;

    private final Map<ContentId, HighlightedProperties> highlight;

    private final Map<ContentId, SortValuesProperty> sortValues;

    private final Map<ContentId, Float> scoreValues;

    public QueryMapper( final Contents contents, FindContentIdsByQueryResult queryResult )
    {
        this.contents = contents;
        this.total = queryResult.getTotalHits();
        this.aggregations = queryResult.getAggregations();
        this.highlight = queryResult.getHighlight();
        this.sortValues = queryResult.getSort();
        this.scoreValues = queryResult.getScore();
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.value( "total", this.total );
        gen.value( "count", this.contents.getSize() );
        serialize( gen, this.contents );
        serialize( gen, aggregations );
        serialize( gen, highlight );
    }

    private void serialize( final MapGenerator gen, final Contents contents )
    {
        gen.array( "hits" );
        for ( Content content : contents )
        {
            gen.map();
            final SortValuesProperty sort = sortValues != null ? sortValues.get( content.getId() ) : null;
            final Float score = scoreValues != null ? scoreValues.get( content.getId() ) : null;

            new ContentMapper( content, sort, score ).serialize( gen );
            gen.end();
        }
        gen.end();
    }

    private void serialize( final MapGenerator gen, final Aggregations aggregations )
    {
        if ( aggregations != null )
        {
            gen.map( "aggregations" );
            new AggregationMapper( aggregations ).serialize( gen );
            gen.end();
        }
    }

    private void serialize( final MapGenerator gen, Map<ContentId, HighlightedProperties> highlight )
    {
        if ( highlight != null )
        {
            gen.map( "highlight" );
            new HighlightMapper( highlight ).serialize( gen );
            gen.end();
        }
    }
}
