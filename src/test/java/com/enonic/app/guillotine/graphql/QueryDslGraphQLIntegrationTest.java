package com.enonic.app.guillotine.graphql;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.aggregation.Buckets;
import com.enonic.xp.aggregation.DateRangeBucket;
import com.enonic.xp.aggregation.NumericRangeBucket;
import com.enonic.xp.aggregation.SingleValueMetricAggregation;
import com.enonic.xp.aggregation.StatsAggregation;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.highlight.HighlightedProperties;
import com.enonic.xp.highlight.HighlightedProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class QueryDslGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{

    @Test
    public void testQueryDslField()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.from( "contentId" ) ).hits( 100 ).totalHits( 1000 ).build() );

        when( contentService.getByIds( any() ) ).thenReturn( Contents.from( ContentFixtures.createMediaContent() ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getQueryDslField.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        List<Map<String, Object>> queryDsl = CastHelper.cast( getFieldFromGuillotine( result, "queryDsl" ) );

        assertEquals( 1, queryDsl.size() );
    }

    @Test
    public void testQueryDslConnectionField()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.from( "contentId" ) ).hits( 100 ).totalHits( 1000 ).aggregations(
                Aggregations.create().add( SingleValueMetricAggregation.create( "count" ).value( 3d ).build() ).add(
                    StatsAggregation.create( "stats" ).avg( 0 ).max( 0 ).min( 0 ).sum( 0 ).count( 0 ).build() ).add(
                    BucketAggregation.bucketAggregation( "bucket1" ).buckets(
                        Buckets.create().add( Bucket.create().key( "key" ).docCount( 1 ).build() ).build() ).build() ).add(
                    BucketAggregation.bucketAggregation( "bucket2" ).buckets( Buckets.create().add(
                        NumericRangeBucket.create().key( "key" ).from( 0 ).to( 1 ).docCount( 1 ).build() ).build() ).build() ).add(
                    BucketAggregation.bucketAggregation( "bucket2" ).buckets( Buckets.create().add(
                        DateRangeBucket.create().key( "key" ).from( LocalDateTime.now().minusDays( 1L ).toInstant( ZoneOffset.UTC ) ).to(
                            Instant.now() ).docCount( 1 ).build() ).build() ).build() ).build() ).highlight(
                Map.of( ContentId.from( "contentId" ),
                        HighlightedProperties.create().add( HighlightedProperty.create().name( "propName" ).build() ).build() ) ).build() );

        when( contentService.getByIds( any() ) ).thenReturn( Contents.from( ContentFixtures.createMediaContent() ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result =
            executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getQueryDslConnectionField.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> queryDslConnection = CastHelper.cast( getFieldFromGuillotine( result, "queryDslConnection" ) );

        assertNotNull( queryDslConnection );
    }

    @Test
    public void testQueryField()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.from( "contentId" ) ).hits( 100 ).totalHits( 1000 ).build() );

        when( contentService.getByIds( any() ) ).thenReturn( Contents.from( ContentFixtures.createMediaContent() ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/QueryField.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        List<Map<String, Object>> query = CastHelper.cast( getFieldFromGuillotine( result, "query" ) );

        assertNotNull( query );
        assertEquals( 1, query.size() );
    }

    @Test
    public void testQueryConnectionField()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.from( "contentId" ) ).hits( 100 ).totalHits( 1000 ).build() );

        when( contentService.getByIds( any() ) ).thenReturn( Contents.from( ContentFixtures.createMediaContent() ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/QueryConnection.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> queryConnection = CastHelper.cast( getFieldFromGuillotine( result, "queryConnection" ) );

        assertNotNull( queryConnection );
    }
}
