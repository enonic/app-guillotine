package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.enonic.app.guillotine.graphql.helper.CastHelper;

public class ArgumentsValidator
{
    private static final int FIRST_ARG_MAX_VALUE = 1000;

    public static void validateArguments( final Map<String, Object> arguments )
    {
        Object first = arguments.get( "first" );
        if ( first != null && ( (int) first < 0 || (int) first > FIRST_ARG_MAX_VALUE ) )
        {
            throw new IllegalArgumentException( "Invalid argument \"first\": The value must be between 0 and " + FIRST_ARG_MAX_VALUE );
        }
        Object offset = arguments.get( "offset" );
        if ( offset != null && (int) offset < 0 )
        {
            throw new IllegalArgumentException( "Invalid argument \"offset\": The value must be equal or greater 0." );
        }
    }

    public static void validateArgumentsForQueryField( final Map<String, Object> arguments )
    {
        validateArguments( arguments );

        if ( arguments.get( "aggregations" ) != null )
        {
            List<Map<String, Object>> aggregations = CastHelper.cast( arguments.get( "aggregations" ) );
            aggregations.forEach( ArgumentsValidator::validateAggregation );
        }

        if ( arguments.get( "highlight" ) != null )
        {
            Map<String, Object> highlight = CastHelper.cast( arguments.get( "highlight" ) );

            if ( highlight.get( "properties" ) != null )
            {
                List<Map<String, Object>> properties = CastHelper.cast( highlight.get( "properties" ) );

                if ( properties == null || properties.isEmpty() )
                {
                    throw new IllegalArgumentException( "Highlight properties must be not empty" );
                }

                if ( properties.stream().anyMatch(
                    property -> property.get( "propertyName" ) == null || "".equals( property.get( "propertyName" ) ) ) )
                {
                    throw new IllegalArgumentException( "Highlight propertyName is required and can not be empty" );
                }
            }
        }
    }

    public static void validateAggregation( final Map<String, Object> aggregation )
    {
        if ( aggregation.get( "name" ) == null || "".equals( aggregation.get( "name" ) ) )
        {
            throw new IllegalArgumentException( "The \"name\" field of Aggregation type is mandatory" );
        }

        long numberOfAggregations = Constants.SUPPORTED_AGGREGATIONS.stream().filter( aggregation::containsKey ).count();

        if ( numberOfAggregations == 0 || numberOfAggregations > 1 )
        {
            throw new IllegalArgumentException(
                "Aggregation must have only one type of aggregations from (" + String.join( ", ", Constants.SUPPORTED_AGGREGATIONS ) +
                    ")" );
        }

        if ( aggregation.get( "subAggregations" ) != null )
        {
            List<Map<String, Object>> subAggregations = CastHelper.cast( aggregation.get( "subAggregations" ) );
            subAggregations.forEach( ArgumentsValidator::validateAggregation );
        }
    }

    public static void validateDslQuery( final Map<String, Object> arguments )
    {
        validateArgumentsForQueryField( arguments );

        if ( arguments.get( "query" ) != null )
        {
            Map<String, Object> query = CastHelper.cast( arguments.get( "query" ) );
            validateOnlyOneFieldMustBeNotNull( query, "DSLQuery" );
            validateGraphQlDSLFields( query );
        }
    }

    public static void validateOnlyOneFieldMustBeNotNull( Map<String, Object> graphQLProjection, String fieldName )
    {
        if ( graphQLProjection != null )
        {
            validateOnlyOneFieldMustBeNotNull( graphQLProjection, new ArrayList<>( graphQLProjection.keySet() ), fieldName );
        }
    }

    public static void validateGraphQlDSLFields( Map<String, Object> dslQueryObject )
    {
        validateGraphQlDslBooleanExpression( dslQueryObject );
        validateDslRangeExpression( dslQueryObject );
        validateDslInExpression( dslQueryObject );
        validateDslTermExpression( dslQueryObject );
    }

    public static void validateGraphQlDslBooleanExpression( Map<String, Object> dslQueryObject )
    {
        if ( dslQueryObject.get( "boolean" ) != null )
        {
            Map<String, Object> booleanField = CastHelper.cast( dslQueryObject.get( "boolean" ) );
            booleanField.keySet().stream().filter( fieldName -> !"boost".equals( fieldName ) ).forEach( fieldName -> {
                if ( booleanField.get( fieldName ) != null )
                {
                    List<Map<String, Object>> queries = CastHelper.cast( booleanField.get( fieldName ) );
                    queries.forEach( ArgumentsValidator::validateGraphQlDSLFields );
                }
            } );
        }
    }

    public static void validateDslTermExpression( Map<String, Object> dslQueryObject )
    {
        if ( dslQueryObject.get( "term" ) != null )
        {
            Map<String, Object> termDslExpr = CastHelper.cast( dslQueryObject.get( "term" ) );
            Map<String, Object> termValue = CastHelper.cast( termDslExpr.get( "value" ) );
            validateOnlyOneFieldMustBeNotNull( termValue, "Term.value" );
        }
    }

    public static void validateDslRangeExpression( Map<String, Object> dslQueryObject )
    {
        if ( dslQueryObject.get( "range" ) != null )
        {
            Map<String, Object> rangeField = CastHelper.cast( dslQueryObject.get( "range" ) );

            if ( rangeField.get( "lt" ) != null && rangeField.get( "lte" ) != null && rangeField.get( "gt" ) != null &&
                rangeField.get( "gte" ) != null )
            {
                throw new IllegalArgumentException( "At least one range property must be specified" );
            }
            if ( rangeField.get( "lt" ) != null && rangeField.get( "lte" ) != null )
            {
                throw new IllegalArgumentException( "lt and lte cannot be used together." );
            }
            if ( rangeField.get( "gt" ) != null && rangeField.get( "gte" ) != null )
            {
                throw new IllegalArgumentException( "gt and gte cannot be used together." );
            }

            Map<String, Object> gt = CastHelper.cast( rangeField.get( "gt" ) );
            Map<String, Object> gte = CastHelper.cast( rangeField.get( "gte" ) );
            Map<String, Object> lt = CastHelper.cast( rangeField.get( "lt" ) );
            Map<String, Object> lte = CastHelper.cast( rangeField.get( "lte" ) );

            validateOnlyOneFieldMustBeNotNull( gt, "Range.gt" );
            validateOnlyOneFieldMustBeNotNull( gte, "Range.gte" );
            validateOnlyOneFieldMustBeNotNull( lt, "Range.lt" );
            validateOnlyOneFieldMustBeNotNull( lte, "Range.lte" );

            Set<String> fields = new HashSet<>();
            collectNameOfNonNullField( fields, gt );
            collectNameOfNonNullField( fields, gte );
            collectNameOfNonNullField( fields, lt );
            collectNameOfNonNullField( fields, lte );

            if ( fields.size() > 1 )
            {
                throw new IllegalArgumentException( "Range. All values must be of the same type" );
            }
        }
    }

    private static void collectNameOfNonNullField( Set<String> holder, Map<String, Object> values )
    {
        if ( values != null )
        {
            holder.addAll( values.keySet() );
        }
    }

    public static void validateDslInExpression( Map<String, Object> dslQueryObject )
    {
        if ( dslQueryObject.get( "in" ) != null )
        {
            Map<String, Object> inDslExpr = CastHelper.cast( dslQueryObject.get( "in" ) );

            List<String> fields = inDslExpr.keySet().stream().filter(
                graphQlField -> !"field".equals( graphQlField ) && !"boost".equals( graphQlField ) ).collect( Collectors.toList() );

            validateOnlyOneFieldMustBeNotNull( dslQueryObject, fields, "In.values" );
        }
    }

    public static void validateOnlyOneFieldMustBeNotNull( Map<String, Object> graphQLProjection, List<String> fields, String fieldName )
    {
        int amountOfNonNullableProperties = 0;
        for ( String field : fields )
        {
            if ( graphQLProjection.get( field ) != null )
            {
                amountOfNonNullableProperties++;
            }
            if ( amountOfNonNullableProperties > 1 )
            {
                throw new IllegalArgumentException( "Must be set only one field for " + fieldName );
            }
        }
    }

}
