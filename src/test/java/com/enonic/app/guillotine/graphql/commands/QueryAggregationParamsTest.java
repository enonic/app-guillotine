package com.enonic.app.guillotine.graphql.commands;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.aggregation.DateHistogramAggregationQuery;
import com.enonic.xp.query.aggregation.HistogramAggregationQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryAggregationParamsTest
{
    @Test
    void dateHistogramWithoutMinDocCountDefaultsToOne()
    {
        final DateHistogramAggregationQuery query = assertInstanceOf( DateHistogramAggregationQuery.class, single(
            Map.of( "perDay", Map.of( "dateHistogram", Map.of( "field", "modifiedTime", "interval", "1d" ) ) ) ) );

        assertEquals( "modifiedTime", query.getFieldName() );
        assertEquals( "1d", query.getInterval() );
        assertEquals( 1L, query.getMinDocCount() );
    }

    @Test
    void dateHistogramKeepsGivenMinDocCount()
    {
        final DateHistogramAggregationQuery query = assertInstanceOf( DateHistogramAggregationQuery.class, single(
            Map.of( "perDay", Map.of( "dateHistogram", Map.of( "field", "modifiedTime", "minDocCount", 0 ) ) ) ) );

        assertEquals( 0L, query.getMinDocCount() );
    }

    @Test
    void histogramWithoutOptionalNumbersDefaults()
    {
        final HistogramAggregationQuery query = assertInstanceOf( HistogramAggregationQuery.class, single(
            Map.of( "byPrice", Map.of( "histogram", Map.of( "field", "data.price", "interval", 10 ) ) ) ) );

        assertEquals( 10L, query.getInterval() );
        assertEquals( 1L, query.getMinDocCount() );
        assertNull( query.getExtendedBoundMin() );
        assertNull( query.getExtendedBoundMax() );
        assertFalse( query.setExtendedBounds() );
        assertEquals( HistogramAggregationQuery.Order.KEY_ASC, query.getOrder() );
    }

    @Test
    void histogramKeepsGivenNumbers()
    {
        final HistogramAggregationQuery query = assertInstanceOf( HistogramAggregationQuery.class, single( Map.of( "byPrice", Map.of(
            "histogram",
            Map.of( "field", "data.price", "interval", 10, "minDocCount", 0, "extendedBoundMin", 5, "extendedBoundMax", 50, "order",
                    "_count desc" ) ) ) ) );

        assertEquals( 0L, query.getMinDocCount() );
        assertEquals( 5L, query.getExtendedBoundMin() );
        assertEquals( 50L, query.getExtendedBoundMax() );
        assertTrue( query.setExtendedBounds() );
        assertEquals( HistogramAggregationQuery.Order.COUNT_DESC, query.getOrder() );
    }

    private static AggregationQuery single( final Map<String, Object> aggregations )
    {
        final Set<AggregationQuery> result = new QueryAggregationParams().getAggregations( aggregations );
        assertEquals( 1, result.size() );
        return result.iterator().next();
    }
}
