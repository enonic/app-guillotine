package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.commands.FindContentsParams;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.SecurityHelper;

public abstract class QueryBaseDataFetcher
    implements DataFetcher<Object>
{
    private final List<String> HAS_VALUE_FILTERS_VALUES = List.of( "stringValues", "intValues", "floatValues", "booleanValues" );

    protected final GuillotineContext context;

    protected QueryBaseDataFetcher( final GuillotineContext context )
    {
        this.context = context;
    }

    protected FindContentsParams createQueryParams( Integer offset, Integer first, DataFetchingEnvironment environment, boolean queryDsl )
    {
        FindContentsParams.Builder builder = FindContentsParams.create().setStart( offset ).setFirst( first ).setQuery(
            createQuery( environment.getArgument( "query" ), queryDsl ) ).setSort(
            createSort( environment.getArgument( "sort" ), queryDsl ) );

        if ( environment.getArgument( "aggregations" ) != null )
        {
            builder.setAggregations( createAggregations( environment.getArgument( "aggregations" ) ) );
        }

        if ( environment.getArgument( "filters" ) != null )
        {
            builder.setFilters( createFilters( environment.getArgument( "filters" ) ) );
        }

        if ( environment.getArgument( "contentTypes" ) != null )
        {
            builder.setContentTypes( environment.getArgument( "contentTypes" ) );
        }

        if ( environment.getArgument( "highlight" ) != null )
        {
            builder.setHighlight( createHighlight( environment.getArgument( "highlight" ) ) );
        }

        return builder.build();
    }

    private Object createQuery( final Object query, final boolean queryDsl )
    {
        if ( query != null && queryDsl )
        {
            return adaptDslQuery( createDslQuery( CastHelper.cast( query ) ), context );
        }
        return adaptQuery( (String) query, context );
    }

    private Object createSort( final Object sort, final boolean queryDsl )
    {
        if ( queryDsl )
        {
            return ArrayHelper.forceArray( sort );
        }
        return sort;
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

    private List<Map<String, Object>> createFilters( List<Map<String, Object>> inputFilters )
    {
        if ( inputFilters == null || inputFilters.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        inputFilters.forEach( inputFilter -> {
            Map<String, Object> filter = new HashMap<>();

            inputFilter.keySet().forEach( filterName -> {
                if ( "hasValue".equals( filterName ) )
                {
                    filter.put( filterName, processHasValueFilter( CastHelper.cast( inputFilter.get( "hasValue" ) ) ) );
                }
                else if ( "boolean".equals( filterName ) )
                {
                    filter.put( filterName, processBooleanFilter( CastHelper.cast( inputFilter.get( "boolean" ) ) ) );
                }
                else
                {
                    filter.put( filterName, inputFilter.get( filterName ) );
                }
            } );

            result.add( filter );
        } );

        return result;
    }

    private Map<String, Object> processHasValueFilter( final Map<String, Object> inputHasValueFilter )
    {
        if ( inputHasValueFilter.containsKey( "field" ) && inputHasValueFilter.keySet().size() > 2 )
        {
            throw new IllegalArgumentException(
                "HasValueFilter must have only one type of values from (\"stringValues, intValues, floatValues and booleanValues\")" );
        }

        Map<String, Object> result = new HashMap<>();

        result.put( "field", inputHasValueFilter.get( "field" ) );

        HAS_VALUE_FILTERS_VALUES.forEach( fieldName -> {
            if ( inputHasValueFilter.get( fieldName ) != null )
            {
                result.put( "values", inputHasValueFilter.get( fieldName ) );
            }
        } );

        return result;
    }

    private Map<String, Object> processBooleanFilter( final Map<String, Object> inputBooleanFilter )
    {
        Map<String, Object> result = new HashMap<>();

        if ( inputBooleanFilter.get( "must" ) != null )
        {
            result.put( "must", createFilters( CastHelper.cast( inputBooleanFilter.get( "must" ) ) ) );
        }
        if ( inputBooleanFilter.get( "mustNot" ) != null )
        {
            result.put( "mustNot", createFilters( CastHelper.cast( inputBooleanFilter.get( "mustNot" ) ) ) );
        }
        if ( inputBooleanFilter.get( "should" ) != null )
        {
            result.put( "should", createFilters( CastHelper.cast( inputBooleanFilter.get( "should" ) ) ) );
        }

        return result;
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

    private Object adaptQuery( String query, GuillotineContext context )
    {
        if ( context.isGlobalMode() )
        {
            return query;
        }

        String queryPrefix = getAllowedNodePaths( context ).stream().map(
            nodePath -> "_path = \"" + nodePath + "\" OR _path LIKE \"" + nodePath + "/*\"" ).collect( Collectors.joining( " OR " ) );

        return "(" + queryPrefix + ")" + ( query != null ? " AND (" + query + ")" : "" );
    }

    private Object adaptDslQuery( Map<String, Object> dslQuery, GuillotineContext context )
    {
        if ( context.isGlobalMode() )
        {
            return dslQuery;
        }

        List<Map<String, Object>> dslExpressions = new ArrayList<>();

        getAllowedNodePaths( context ).forEach( nodePath -> {
            dslExpressions.add( Collections.singletonMap( "term", createTermOrLikeDslExpression( nodePath ) ) );
            dslExpressions.add( Collections.singletonMap( "like", createTermOrLikeDslExpression( nodePath + "/*" ) ) );
        } );

        return dslQuery != null
            ? Map.of( "boolean", Map.of( "must", List.of( dslQuery, Map.of( "boolean", Map.of( "should", dslExpressions ) ) ) ) )
            : Map.of( "boolean", Map.of( "should", dslExpressions ) );
    }

    private Map<String, String> createTermOrLikeDslExpression( String value )
    {
        Map<String, String> result = new HashMap<>();

        result.put( "field", "_path" );
        result.put( "value", value );

        return result;
    }

    private static List<String> getAllowedNodePaths( GuillotineContext context )
    {
        return SecurityHelper.getAllowedContentPaths( context ).stream().map( contentPath -> "/content" + contentPath ).collect(
            Collectors.toList() );
    }
}
