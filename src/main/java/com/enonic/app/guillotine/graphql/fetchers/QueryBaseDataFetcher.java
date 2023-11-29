package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.commands.FindContentsParams;
import com.enonic.app.guillotine.graphql.helper.AggregationHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.HighlightHelper;
import com.enonic.app.guillotine.graphql.helper.QueryDslHelper;
import com.enonic.app.guillotine.graphql.helper.SortDslHelper;

public abstract class QueryBaseDataFetcher
    implements DataFetcher<Object>
{
    protected FindContentsParams createQueryParams( Integer offset, Integer first, DataFetchingEnvironment environment )
    {
        FindContentsParams.Builder builder = FindContentsParams.create().setStart( offset ).setFirst( first ).setQuery(
            createQuery( environment.getArgument( "query" ) ) ).setSort( SortDslHelper.createDslSort( environment.getArgument( "sort" ) ) );

        if ( environment.getArgument( "aggregations" ) != null )
        {
            builder.setAggregations( AggregationHelper.createAggregations( environment.getArgument( "aggregations" ) ) );
        }

        if ( environment.getArgument( "highlight" ) != null )
        {
            builder.setHighlight( HighlightHelper.createHighlight( environment.getArgument( "highlight" ) ) );
        }

        return builder.build();
    }

    private Object createQuery( final Object query )
    {
        return query != null ? QueryDslHelper.createDslQuery( CastHelper.cast( query ) ) : null;
    }

}
