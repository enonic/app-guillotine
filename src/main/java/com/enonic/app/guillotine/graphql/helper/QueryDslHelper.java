package com.enonic.app.guillotine.graphql.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryDslHelper
{
    public static Map<String, Object> createDslQuery( final Map<String, Object> inputQueryDsl )
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

    private static void createDslMatchAll( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static void createDslTermExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static void createDslLikeExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static void createDslInExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static void createDslRangeExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static void setTypeBaseOnValue( final Map<String, Object> source, final Map<String, Object> target )
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

    private static void createDslPathMatchExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static void createDslExistsExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static void createDslStringExpression( final Map<String, Object> holder, final Map<String, Object> expression,
                                                   String expressionName )
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

    private static void createDslStemmedExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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


    private static void createDslBooleanExpression( final Map<String, Object> holder, final Map<String, Object> expression )
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

    private static List<Map<String, Object>> populateBooleanExpression( final Object expression )
    {
        List<Map<String, Object>> result = new ArrayList<>();
        ArrayHelper.forceArray( expression ).forEach(
            booleanExpression -> result.add( createDslQuery( CastHelper.cast( booleanExpression ) ) ) );
        return result;
    }

    private static Object extractPropertyValue( final Map<String, Object> graphQLValue )
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

    private static Object extractPropertyValues( final Map<String, Object> graphQLValue )
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
