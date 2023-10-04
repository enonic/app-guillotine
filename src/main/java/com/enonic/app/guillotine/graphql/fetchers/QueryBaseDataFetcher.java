package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.commands.FindContentsParams;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;

public abstract class QueryBaseDataFetcher
    implements DataFetcher<Object>
{
    protected FindContentsParams createQueryParams( Integer offset, Integer first, DataFetchingEnvironment environment )
    {
        FindContentsParams.Builder builder = FindContentsParams.create().setStart( offset ).setFirst( first ).setQuery(
            createQuery( environment.getArgument( "query" ) ) ).setSort( createSort( environment.getArgument( "sort" ) ) );

        if ( environment.getArgument( "aggregations" ) != null )
        {
            builder.setAggregations( createAggregations( environment.getArgument( "aggregations" ) ) );
        }

        if ( environment.getArgument( "highlight" ) != null )
        {
            builder.setHighlight( createHighlight( environment.getArgument( "highlight" ) ) );
        }

        return builder.build();
    }

    private Object createQuery( final Object query )
    {
        return createDslQuery( CastHelper.cast( query ) );
    }

    private Object createSort( final Object sort )
    {
        List<Map<String, Object>> result = new ArrayList<>();

        ArrayHelper.forceArray( sort ).forEach( sortItem -> {
            Map<String, Object> sortItemAsMap = CastHelper.cast( sortItem );

            Map<String, Object> sortAsMap = new HashMap<>();

            sortAsMap.put( "field", sortItemAsMap.get( "field" ) );
            sortAsMap.computeIfAbsent( "direction", k -> sortItemAsMap.get( "direction" ) );
            sortAsMap.computeIfAbsent( "unit", k -> sortItemAsMap.get( "unit" ) );

            if ( sortItemAsMap.get( "location" ) != null )
            {
                Map<String, Object> locationAsMap = CastHelper.cast( sortItemAsMap.get( "location" ) );
                sortAsMap.put( "location", Map.of( "lat", locationAsMap.get( "lat" ), "lon", locationAsMap.get( "lon" ) ) );
            }

            result.add( sortAsMap );
        } );

        return result;
    }

    private Map<String, Object> createAggregations( List<Map<String, Object>> inputAggregations )
    {
        Map<String, Object> holder = new LinkedHashMap<>();

        inputAggregations.forEach( inputAggregation -> createAggregation( holder, inputAggregation ) );

        return holder;
    }

    private void createAggregation( Map<String, Object> holder, Map<String, Object> inputAggregation )
    {
        String inputAggregationName = inputAggregation.get( "name" ).toString();

        Map<String, Object> aggregationProjection = new HashMap<>();
        holder.put( inputAggregationName, aggregationProjection );

        Constants.SUPPORTED_AGGREGATIONS.forEach( aggregationName -> {
            if ( inputAggregation.get( aggregationName ) != null )
            {
                aggregationProjection.put( aggregationName, inputAggregation.get( aggregationName ) );
            }
        } );

        List<Map<String, Object>> subAggregations = CastHelper.cast( inputAggregation.get( "subAggregations" ) );
        if ( subAggregations != null && !subAggregations.isEmpty() )
        {
            Map<String, Object> subAggregationHolder = new HashMap<>();
            aggregationProjection.put( "aggregations", subAggregationHolder );

            subAggregations.forEach( subAggregation -> createAggregation( subAggregationHolder, subAggregation ) );
        }
    }

    private Map<String, Object> createDslQuery( Map<String, Object> inputQueryDsl )
    {
        Map<String, Object> result = new HashMap<>();

        createDslMatchAll( result, CastHelper.cast( inputQueryDsl.get( "matchAll" ) ) );
        createDslTermExpression( result, CastHelper.cast( inputQueryDsl.get( "term" ) ) );
        createDslLikeExpression( result, CastHelper.cast( inputQueryDsl.get( "like" ) ) );
        createDslInExpression( result, CastHelper.cast( inputQueryDsl.get( "in" ) ) );
        createDslRangeExpression( result, CastHelper.cast( inputQueryDsl.get( "range" ) ) );
        createDslPathMatchExpression( result, CastHelper.cast( inputQueryDsl.get( "pathMatch" ) ) );
        createDslExistsExpression( result, CastHelper.cast( inputQueryDsl.get( "exists" ) ) );
        createDslStringExpression( result, CastHelper.cast( inputQueryDsl.get( "fulltext" ) ), "fulltext" );
        createDslStringExpression( result, CastHelper.cast( inputQueryDsl.get( "ngram" ) ), "ngram" );
        createDslStemmedExpression( result, CastHelper.cast( inputQueryDsl.get( "stemmed" ) ) );
        createDslBooleanExpression( result, CastHelper.cast( inputQueryDsl.get( "boolean" ) ) );

        return result;
    }

    private void createDslMatchAll( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> matchAll = new HashMap<>();
            if ( expression.get( "boost" ) != null )
            {
                matchAll.put( "boost", expression.get( "boost" ) );
            }
            holder.put( "matchAll", matchAll );
        }
    }

    private void createDslTermExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "field", expression.get( "field" ) );
            dslExpression.put( "value", extractPropertyValue( CastHelper.cast( expression.get( "value" ) ) ) );

            if ( expression.get( "boost" ) != null )
            {
                dslExpression.put( "boost", expression.get( "boost" ) );
            }
            holder.put( "term", dslExpression );
        }
    }

    private void createDslLikeExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "field", expression.get( "field" ) );
            dslExpression.put( "value", CastHelper.cast( expression.get( "value" ) ) );

            if ( expression.get( "boost" ) != null )
            {
                dslExpression.put( "boost", expression.get( "boost" ) );
            }
            holder.put( "like", dslExpression );
        }
    }

    private void createDslInExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "field", expression.get( "field" ) );
            dslExpression.put( "values", extractPropertyValues( expression ) );

            if ( expression.get( "localTimeValues" ) != null )
            {
                dslExpression.put( "type", "time" );
            }
            if ( expression.get( "localDateValues" ) != null || expression.get( "localDateTimeValues" ) != null ||
                expression.get( "instantValues" ) != null )
            {
                dslExpression.put( "type", "dateTime" );
            }
            if ( expression.get( "boost" ) != null )
            {
                dslExpression.put( "boost", expression.get( "boost" ) );
            }
            holder.put( "in", dslExpression );
        }
    }

    private void createDslRangeExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "field", expression.get( "field" ) );

            if ( expression.get( "lt" ) != null )
            {
                dslExpression.put( "lt", extractPropertyValue( CastHelper.cast( expression.get( "lt" ) ) ) );
            }
            if ( expression.get( "lte" ) != null )
            {
                dslExpression.put( "lte", extractPropertyValue( CastHelper.cast( expression.get( "lte" ) ) ) );
            }
            if ( expression.get( "gt" ) != null )
            {
                dslExpression.put( "gt", extractPropertyValue( CastHelper.cast( expression.get( "gt" ) ) ) );
            }
            if ( expression.get( "gte" ) != null )
            {
                dslExpression.put( "gte", extractPropertyValue( CastHelper.cast( expression.get( "gte" ) ) ) );
            }
            if ( expression.get( "boost" ) != null )
            {
                dslExpression.put( "boost", expression.get( "boost" ) );
            }

            Arrays.asList( expression.get( "lt" ), expression.get( "lte" ), expression.get( "gt" ), expression.get( "gte" ) ).forEach(
                item -> {
                    if ( item != null )
                    {
                        setTypeBaseOnValue( CastHelper.cast( item ), dslExpression );
                    }
                } );

            holder.put( "range", dslExpression );
        }
    }

    private void setTypeBaseOnValue( Map<String, Object> source, Map<String, Object> target )
    {
        if ( source.get( "localTime" ) != null )
        {
            target.put( "type", "time" );
        }
        if ( source.get( "localDate" ) != null || source.get( "localDateTime" ) != null || source.get( "instant" ) != null )
        {
            target.put( "type", "dateTime" );
        }
    }

    private void createDslPathMatchExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "field", expression.get( "field" ) );
            dslExpression.put( "path", expression.get( "path" ) );

            if ( expression.get( "minimumMatch" ) != null )
            {
                dslExpression.put( "minimumMatch", expression.get( "minimumMatch" ) );
            }
            if ( expression.get( "boost" ) != null )
            {
                dslExpression.put( "boost", expression.get( "boost" ) );
            }

            holder.put( "pathMatch", dslExpression );
        }
    }

    private void createDslExistsExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "field", expression.get( "field" ) );

            if ( expression.get( "boost" ) != null )
            {
                dslExpression.put( "boost", expression.get( "boost" ) );
            }

            holder.put( "exists", dslExpression );
        }
    }

    private void createDslStringExpression( Map<String, Object> holder, Map<String, Object> expression, String expressionName )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "fields", expression.get( "fields" ) );
            dslExpression.put( "query", expression.get( "query" ) );

            if ( expression.get( "operator" ) != null )
            {
                dslExpression.put( "operator", expression.get( "operator" ) );
            }

            holder.put( expressionName, dslExpression );
        }
    }

    private void createDslStemmedExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            dslExpression.put( "fields", expression.get( "fields" ) );
            dslExpression.put( "query", expression.get( "query" ) );

            if ( expression.get( "operator" ) != null )
            {
                dslExpression.put( "operator", expression.get( "operator" ) );
            }
            if ( expression.get( "language" ) != null )
            {
                dslExpression.put( "language", expression.get( "language" ) );
            }
            if ( expression.get( "boost" ) != null )
            {
                dslExpression.put( "boost", expression.get( "boost" ) );
            }

            holder.put( "stemmed", dslExpression );
        }
    }


    private void createDslBooleanExpression( Map<String, Object> holder, Map<String, Object> expression )
    {
        if ( expression != null )
        {
            Map<String, Object> dslExpression = new HashMap<>();
            if ( expression.get( "should" ) != null )
            {
                dslExpression.put( "should", populateBooleanExpression( expression.get( "should" ) ) );
            }
            if ( expression.get( "must" ) != null )
            {
                dslExpression.put( "must", populateBooleanExpression( expression.get( "must" ) ) );
            }
            if ( expression.get( "mustNot" ) != null )
            {
                dslExpression.put( "mustNot", populateBooleanExpression( expression.get( "mustNot" ) ) );
            }
            if ( expression.get( "filter" ) != null )
            {
                dslExpression.put( "filter", populateBooleanExpression( expression.get( "filter" ) ) );
            }
            holder.put( "boolean", dslExpression );
        }
    }

    private List<Map<String, Object>> populateBooleanExpression( Object expression )
    {
        List<Map<String, Object>> result = new ArrayList<>();
        ArrayHelper.forceArray( expression ).forEach(
            booleanExpression -> result.add( createDslQuery( CastHelper.cast( booleanExpression ) ) ) );
        return result;
    }

    private Map<String, Object> createHighlight( Map<String, Object> inputHighlight )
    {
        Map<String, Object> result = new HashMap<>();

        inputHighlight.keySet().forEach( key -> {
            if ( inputHighlight.get( key ) != null )
            {
                result.put( key, inputHighlight.get( key ) );
            }
        } );
        result.put( "properties", createHighlightProperties( inputHighlight ) );

        return result;
    }

    private Map<String, Object> createHighlightProperties( Map<String, Object> inputHighlight )
    {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> properties = CastHelper.cast( inputHighlight.get( "properties" ) );

        properties.forEach( highlightProperty -> {
            Map<String, Object> propertyData = new HashMap<>();
            highlightProperty.keySet().stream().filter( prop -> !"propertyName".equals( prop ) ).forEach( prop -> {
                if ( highlightProperty.get( prop ) != null )
                {
                    propertyData.put( prop, highlightProperty.get( prop ) );
                }
            } );
            result.put( highlightProperty.get( "propertyName" ).toString(), propertyData );
        } );

        return result;
    }

    private Object extractPropertyValue( Map<String, Object> graphQLValue )
    {
        if ( graphQLValue.get( "string" ) != null )
        {
            return graphQLValue.get( "string" );
        }
        if ( graphQLValue.get( "double" ) != null )
        {
            return graphQLValue.get( "double" );
        }
        if ( graphQLValue.get( "long" ) != null )
        {
            return graphQLValue.get( "long" );
        }
        if ( graphQLValue.get( "boolean" ) != null )
        {
            return graphQLValue.get( "boolean" );
        }
        if ( graphQLValue.get( "localDate" ) != null )
        {
            return graphQLValue.get( "localDate" );
        }
        if ( graphQLValue.get( "localDateTime" ) != null )
        {
            return graphQLValue.get( "localDateTime" );
        }
        if ( graphQLValue.get( "localTime" ) != null )
        {
            return graphQLValue.get( "localTime" );
        }
        if ( graphQLValue.get( "instant" ) != null )
        {
            return graphQLValue.get( "instant" );
        }

        throw new IllegalArgumentException( "Value must be not null" );
    }

    private Object extractPropertyValues( Map<String, Object> graphQLValue )
    {
        if ( graphQLValue.get( "stringValues" ) != null )
        {
            return graphQLValue.get( "stringValues" );
        }
        if ( graphQLValue.get( "doubleValues" ) != null )
        {
            return graphQLValue.get( "doubleValues" );
        }
        if ( graphQLValue.get( "longValues" ) != null )
        {
            return graphQLValue.get( "longValues" );
        }
        if ( graphQLValue.get( "booleanValues" ) != null )
        {
            return graphQLValue.get( "booleanValues" );
        }
        if ( graphQLValue.get( "localDateValues" ) != null )
        {
            return graphQLValue.get( "localDateValues" );
        }
        if ( graphQLValue.get( "localDateTimeValues" ) != null )
        {
            return graphQLValue.get( "localDateTimeValues" );
        }
        if ( graphQLValue.get( "localTimeValues" ) != null )
        {
            return graphQLValue.get( "localTimeValues" );
        }
        if ( graphQLValue.get( "instantValues" ) != null )
        {
            return graphQLValue.get( "instantValues" );
        }

        throw new IllegalArgumentException( "Value must be not null" );
    }
}
