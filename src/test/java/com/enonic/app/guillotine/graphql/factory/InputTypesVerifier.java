package com.enonic.app.guillotine.graphql.factory;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.scalars.CustomScalars;

import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getNameForGraphQLTypeReference;
import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getOriginalTypeFromGraphQLList;
import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getOriginalTypeFromGraphQLNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputTypesVerifier
{


    private final GuillotineContext context;

    public InputTypesVerifier( final GuillotineContext context )
    {
        this.context = context;
    }

    public void verify()
    {
        verifyProcessHtmlInput();
        verifyNumberRangeInput();
        verifyDateRangeInput();
        verifyGeoPointInput();
        verifyTermsAggregationInput();
        verifyStatsAggregationInput();
        verifyRangeAggregationInput();
        verifyDateRangeAggregationInput();
        verifyDateHistogramAggregationInput();
        verifyGeoDistanceAggregationInput();
        verifyMinAggregationInput();
        verifyMaxAggregationInput();
        verifyValueCountAggregationInput();
        verifyAggregationInput();
        verifyExistsFilterInput();
        verifyNotExistsFilterInput();
        verifyHasValueFilterInput();
        verifyIdsFilterInput();
        verifyBooleanFilterInput();
        verifyFilterInput();
        verifyDSLExpressionValueInput();
        verifyTermDSLExpressionInput();
        verifyLikeDSLExpressionInput();
        verifyInDSLExpressionInput();
        verifyExistsDSLExpressionInput();
        verifyStemmedDSLExpressionInput();
        verifyFulltextDSLExpressionInput();
        verifyNgramDSLExpressionInput();
        verifyPathMatchDSLExpressionInput();
        verifyMatchAllDSLExpressionInput();
        verifyRangeDSLExpressionInput();
        verifyBooleanDSLExpressionInput();
        verifyQueryDSLInput();
        verifyGeoPointSortDslInput();
        verifySortDslInput();
        verifyHighlightPropertiesInputType();
        verifyHighlightInputType();
    }

    private void verifyHighlightInputType()
    {
        GraphQLInputObjectType type = context.getInputType( "HighlightInputType" );

        assertEquals( "HighlightInputType input type", type.getDescription() );

        assertEquals( 11, type.getFieldDefinitions().size() );

        GraphQLType typeOfProperties = getOriginalTypeFromGraphQLNonNull( type, "properties" );
        assertTrue( typeOfProperties instanceof GraphQLList );
        assertEquals( "HighlightPropertiesInputType",
                      getNameForGraphQLTypeReference( ( (GraphQLList) typeOfProperties ).getOriginalWrappedType() ) );

        assertEquals( "HighlightEncoderType", getNameForGraphQLTypeReference( type.getField( "encoder" ).getType() ) );
        assertEquals( "HighlightTagsSchemaType", getNameForGraphQLTypeReference( type.getField( "tagsSchema" ).getType() ) );
        assertEquals( "HighlightFragmenterType", getNameForGraphQLTypeReference( type.getField( "fragmenter" ).getType() ) );
        assertEquals( Scalars.GraphQLInt, type.getField( "fragmentSize" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "noMatchSize" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "numberOfFragments" ).getType() );
        assertEquals( "HighlightOrderType", getNameForGraphQLTypeReference( type.getField( "order" ).getType() ) );
        assertEquals( Scalars.GraphQLString, type.getField( "preTag" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "postTag" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "requireFieldMatch" ).getType() );
    }

    private void verifyHighlightPropertiesInputType()
    {
        GraphQLInputObjectType type = context.getInputType( "HighlightPropertiesInputType" );

        assertEquals( "HighlightProperties input type", type.getDescription() );

        assertEquals( 9, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "propertyName" ) );
        assertEquals( "HighlightFragmenterType", getNameForGraphQLTypeReference( type.getField( "fragmenter" ).getType() ) );
        assertEquals( Scalars.GraphQLInt, type.getField( "fragmentSize" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "noMatchSize" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "numberOfFragments" ).getType() );
        assertEquals( "HighlightOrderType", getNameForGraphQLTypeReference( type.getField( "order" ).getType() ) );
        assertEquals( Scalars.GraphQLString, type.getField( "preTag" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "postTag" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "requireFieldMatch" ).getType() );
    }

    private void verifySortDslInput()
    {
        GraphQLInputObjectType type = context.getInputType( "SortDslInput" );

        assertEquals( "Sort Dsl input type", type.getDescription() );

        assertEquals( 4, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
        assertEquals( "DslSortDirectionType", getNameForGraphQLTypeReference( type.getField( "direction" ).getType() ) );
        assertEquals( "GeoPointSortDslInput", getNameForGraphQLTypeReference( type.getField( "location" ).getType() ) );
        assertEquals( "DslGeoPointDistanceType", getNameForGraphQLTypeReference( type.getField( "unit" ).getType() ) );
    }

    private void verifyGeoPointSortDslInput()
    {
        GraphQLInputObjectType type = context.getInputType( "GeoPointSortDslInput" );

        assertEquals( "GeoPoint Sort Dsl input type", type.getDescription() );

        assertEquals( 2, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLFloat, getOriginalTypeFromGraphQLNonNull( type, "lat" ) );
        assertEquals( Scalars.GraphQLFloat, getOriginalTypeFromGraphQLNonNull( type, "lon" ) );
    }

    private void verifyQueryDSLInput()
    {
        GraphQLInputObjectType type = context.getInputType( "QueryDSLInput" );

        assertEquals( "QueryDSLInput type", type.getDescription() );

        assertEquals( 11, type.getFieldDefinitions().size() );
        assertEquals( "BooleanDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "boolean" ).getType() ) );
        assertEquals( "NgramDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "ngram" ).getType() ) );
        assertEquals( "StemmedDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "stemmed" ).getType() ) );
        assertEquals( "FulltextDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "fulltext" ).getType() ) );
        assertEquals( "MatchAllDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "matchAll" ).getType() ) );
        assertEquals( "PathMatchDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "pathMatch" ).getType() ) );
        assertEquals( "RangeDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "range" ).getType() ) );
        assertEquals( "TermDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "term" ).getType() ) );
        assertEquals( "LikeDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "like" ).getType() ) );
        assertEquals( "InDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "in" ).getType() ) );
        assertEquals( "ExistsDSLExpressionInput", getNameForGraphQLTypeReference( type.getField( "exists" ).getType() ) );
    }

    private void verifyBooleanDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "BooleanDSLExpressionInput" );

        assertEquals( "BooleanDSLExpressionInput type", type.getDescription() );

        assertEquals( 5, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );

        GraphQLType mustField = getOriginalTypeFromGraphQLList( type, "must" );
        assertTrue( mustField instanceof GraphQLTypeReference );
        assertEquals( "QueryDSLInput", ( (GraphQLTypeReference) mustField ).getName() );

        GraphQLType mustNotField = getOriginalTypeFromGraphQLList( type, "mustNot" );
        assertTrue( mustNotField instanceof GraphQLTypeReference );
        assertEquals( "QueryDSLInput", ( (GraphQLTypeReference) mustNotField ).getName() );

        GraphQLType shouldField = getOriginalTypeFromGraphQLList( type, "should" );
        assertTrue( shouldField instanceof GraphQLTypeReference );
        assertEquals( "QueryDSLInput", ( (GraphQLTypeReference) shouldField ).getName() );

        GraphQLType filterField = getOriginalTypeFromGraphQLList( type, "filter" );
        assertTrue( filterField instanceof GraphQLTypeReference );
        assertEquals( "QueryDSLInput", ( (GraphQLTypeReference) filterField ).getName() );
    }

    private void verifyRangeDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "RangeDSLExpressionInput" );

        assertEquals( 6, type.getFieldDefinitions().size() );
        assertEquals( "RangeDSLExpressionInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
        assertEquals( "DSLExpressionValueInput", getNameForGraphQLTypeReference( type.getField( "lt" ).getType() ) );
        assertEquals( "DSLExpressionValueInput", getNameForGraphQLTypeReference( type.getField( "lte" ).getType() ) );
        assertEquals( "DSLExpressionValueInput", getNameForGraphQLTypeReference( type.getField( "gt" ).getType() ) );
        assertEquals( "DSLExpressionValueInput", getNameForGraphQLTypeReference( type.getField( "gte" ).getType() ) );
    }

    private void verifyMatchAllDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "MatchAllDSLExpressionInput" );

        assertEquals( 1, type.getFieldDefinitions().size() );
        assertEquals( "MatchAllDSLExpressionInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );
    }

    private void verifyPathMatchDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "PathMatchDSLExpressionInput" );

        assertEquals( 4, type.getFieldDefinitions().size() );
        assertEquals( "PathMatchDSLExpressionInput type", type.getDescription() );

        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "path" ) );
        assertEquals( Scalars.GraphQLInt, type.getField( "minimumMatch" ).getType() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );
    }

    private void verifyNgramDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "NgramDSLExpressionInput" );

        assertEquals( 3, type.getFieldDefinitions().size() );
        assertEquals( "NgramDSLExpressionInput type", type.getDescription() );

        GraphQLType typeOfFieldsAsNonNull = getOriginalTypeFromGraphQLNonNull( type, "fields" );
        assertTrue( typeOfFieldsAsNonNull instanceof GraphQLList );
        assertEquals( Scalars.GraphQLString, ( (GraphQLList) typeOfFieldsAsNonNull ).getOriginalWrappedType() );

        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "query" ) );
        assertEquals( "DslOperatorType", getNameForGraphQLTypeReference( type.getField( "operator" ).getType() ) );
    }

    private void verifyFulltextDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "FulltextDSLExpressionInput" );

        assertEquals( 3, type.getFieldDefinitions().size() );
        assertEquals( "FulltextDSLExpressionInput type", type.getDescription() );

        GraphQLType typeOfFieldsAsNonNull = getOriginalTypeFromGraphQLNonNull( type, "fields" );
        assertTrue( typeOfFieldsAsNonNull instanceof GraphQLList );
        assertEquals( Scalars.GraphQLString, ( (GraphQLList) typeOfFieldsAsNonNull ).getOriginalWrappedType() );

        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "query" ) );
        assertEquals( "DslOperatorType", getNameForGraphQLTypeReference( type.getField( "operator" ).getType() ) );
    }

    private void verifyStemmedDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "StemmedDSLExpressionInput" );

        assertEquals( 5, type.getFieldDefinitions().size() );
        assertEquals( "StemmedDSLExpressionInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );

        GraphQLType typeOfFieldsAsNonNull = getOriginalTypeFromGraphQLNonNull( type, "fields" );
        assertTrue( typeOfFieldsAsNonNull instanceof GraphQLList );
        assertEquals( Scalars.GraphQLString, ( (GraphQLList) typeOfFieldsAsNonNull ).getOriginalWrappedType() );

        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "query" ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "language" ) );
        assertEquals( "DslOperatorType", getNameForGraphQLTypeReference( type.getField( "operator" ).getType() ) );
    }

    private void verifyExistsDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "ExistsDSLExpressionInput" );

        assertEquals( "ExistsDSLExpressionInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
    }

    private void verifyInDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "InDSLExpressionInput" );

        assertEquals( "InDSLExpressionInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLList( type, "stringValues" ) );
        assertEquals( Scalars.GraphQLFloat, getOriginalTypeFromGraphQLList( type, "doubleValues" ) );
        assertEquals( Scalars.GraphQLInt, getOriginalTypeFromGraphQLList( type, "longValues" ) );
        assertEquals( Scalars.GraphQLBoolean, getOriginalTypeFromGraphQLList( type, "booleanValues" ) );
        assertEquals( ExtendedScalars.Date, getOriginalTypeFromGraphQLList( type, "localDateValues" ) );
        assertEquals( CustomScalars.LocalDateTime, getOriginalTypeFromGraphQLList( type, "localDateTimeValues" ) );
        assertEquals( CustomScalars.LocalTime, getOriginalTypeFromGraphQLList( type, "localTimeValues" ) );
        assertEquals( ExtendedScalars.DateTime, getOriginalTypeFromGraphQLList( type, "instantValues" ) );
    }

    private void verifyLikeDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "LikeDSLExpressionInput" );

        assertEquals( "LikeDSLExpressionInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "value" ) );
    }

    private void verifyTermDSLExpressionInput()
    {
        GraphQLInputObjectType type = context.getInputType( "TermDSLExpressionInput" );

        assertEquals( "TermDSLExpressionInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
        assertEquals( Scalars.GraphQLFloat, type.getField( "boost" ).getType() );
        assertEquals( "DSLExpressionValueInput", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLNonNull( type, "value" ) ) );
    }

    private void verifyDSLExpressionValueInput()
    {
        GraphQLInputObjectType type = context.getInputType( "DSLExpressionValueInput" );

        assertEquals( 8, type.getFieldDefinitions().size() );
        assertEquals( "DSLExpressionValueInput type", type.getDescription() );
        assertEquals( Scalars.GraphQLString, type.getField( "string" ).getType() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "double" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "long" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "boolean" ).getType() );
        assertEquals( ExtendedScalars.Date, type.getField( "localDate" ).getType() );
        assertEquals( CustomScalars.LocalDateTime, type.getField( "localDateTime" ).getType() );
        assertEquals( CustomScalars.LocalTime, type.getField( "localTime" ).getType() );
        assertEquals( ExtendedScalars.DateTime, type.getField( "instant" ).getType() );
    }

    private void verifyFilterInput()
    {
        GraphQLInputObjectType type = context.getInputType( "FilterInput" );

        assertEquals( "Filter input type", type.getDescription() );

        assertEquals( "BooleanFilterInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "boolean" ).getType() ) );
        assertEquals( "ExistsFilterInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "exists" ).getType() ) );
        assertEquals( "NotExistsFilterInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "notExists" ).getType() ) );
        assertEquals( "HasValueFilterInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "hasValue" ).getType() ) );
        assertEquals( "IdsFilterInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "ids" ).getType() ) );
    }

    private void verifyBooleanFilterInput()
    {
        GraphQLInputObjectType type = context.getInputType( "BooleanFilterInput" );

        assertEquals( "BooleanFilter input type", type.getDescription() );

        GraphQLType mustField = getOriginalTypeFromGraphQLList( type, "must" );
        assertTrue( mustField instanceof GraphQLTypeReference );
        assertEquals( "FilterInput", ( (GraphQLTypeReference) mustField ).getName() );

        GraphQLType mustNotField = getOriginalTypeFromGraphQLList( type, "mustNot" );
        assertTrue( mustNotField instanceof GraphQLTypeReference );
        assertEquals( "FilterInput", ( (GraphQLTypeReference) mustNotField ).getName() );

        GraphQLType shouldField = getOriginalTypeFromGraphQLList( type, "should" );
        assertTrue( shouldField instanceof GraphQLTypeReference );
        assertEquals( "FilterInput", ( (GraphQLTypeReference) shouldField ).getName() );
    }

    private void verifyIdsFilterInput()
    {
        GraphQLInputObjectType type = context.getInputType( "IdsFilterInput" );

        assertEquals( "IdsFilter input type", type.getDescription() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLList( type, "values" ) );
    }


    private void verifyHasValueFilterInput()
    {
        GraphQLInputObjectType type = context.getInputType( "HasValueFilterInput" );

        assertEquals( "HasValueFilter input type", type.getDescription() );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "field" ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLList( type, "stringValues" ) );
        assertEquals( Scalars.GraphQLInt, getOriginalTypeFromGraphQLList( type, "intValues" ) );
        assertEquals( Scalars.GraphQLFloat, getOriginalTypeFromGraphQLList( type, "floatValues" ) );
        assertEquals( Scalars.GraphQLBoolean, getOriginalTypeFromGraphQLList( type, "booleanValues" ) );
    }

    private void verifyNotExistsFilterInput()
    {
        GraphQLInputObjectType type = context.getInputType( "NotExistsFilterInput" );

        assertEquals( "NotExistsFilter input type", type.getDescription() );

        assertEquals( 1, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );
    }

    private void verifyExistsFilterInput()
    {
        GraphQLInputObjectType type = context.getInputType( "ExistsFilterInput" );

        assertEquals( "ExistsFilter input type", type.getDescription() );

        assertEquals( 1, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );
    }

    private void verifyAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "AggregationInput" );

        assertEquals( "Aggregation input type", type.getDescription() );

        assertEquals( 11, type.getFieldDefinitions().size() );

        GraphQLInputType typeOfNameField = type.getField( "name" ).getType();
        assertTrue( typeOfNameField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfNameField ).getOriginalWrappedType() );

        GraphQLInputType typeOfSubAggregationsField = type.getField( "subAggregations" ).getType();
        assertTrue( typeOfSubAggregationsField instanceof GraphQLList );

        GraphQLType originalWrappedType = ( (GraphQLList) typeOfSubAggregationsField ).getOriginalWrappedType();
        assertTrue( originalWrappedType instanceof GraphQLTypeReference );
        assertEquals( "AggregationInput", ( (GraphQLTypeReference) originalWrappedType ).getName() );

        assertEquals( "TermsAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "terms" ).getType() ) );
        assertEquals( "StatsAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "stats" ).getType() ) );
        assertEquals( "RangeAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "range" ).getType() ) );
        assertEquals( "DateRangeAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "dateRange" ).getType() ) );
        assertEquals( "DateHistogramAggregationInput",
                      getNameForGraphQLTypeReference( type.getFieldDefinition( "dateHistogram" ).getType() ) );
        assertEquals( "GeoDistanceAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "geoDistance" ).getType() ) );
        assertEquals( "MinAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "min" ).getType() ) );
        assertEquals( "MaxAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "max" ).getType() ) );
        assertEquals( "ValueCountAggregationInput", getNameForGraphQLTypeReference( type.getFieldDefinition( "count" ).getType() ) );
    }

    private void verifyMinAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "MinAggregationInput" );

        assertEquals( "MinAggregation input type", type.getDescription() );

        assertEquals( 1, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );
    }

    private void verifyMaxAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "MaxAggregationInput" );

        assertEquals( "MaxAggregation input type", type.getDescription() );

        assertEquals( 1, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );
    }

    private void verifyValueCountAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "ValueCountAggregationInput" );

        assertEquals( "ValueCount Aggregation input type", type.getDescription() );

        assertEquals( 1, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );
    }

    private void verifyGeoDistanceAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "GeoDistanceAggregationInput" );

        assertEquals( "GeoDistance aggregation input type", type.getDescription() );

        assertEquals( 4, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );

        assertEquals( Scalars.GraphQLString, type.getField( "unit" ).getType() );

        GraphQLInputType typeOfOriginField = type.getField( "origin" ).getType();
        assertTrue( typeOfOriginField instanceof GraphQLNonNull );
        assertEquals( "GeoPointInput", getNameForGraphQLTypeReference( ( (GraphQLNonNull) typeOfOriginField ).getOriginalWrappedType() ) );

        GraphQLInputType typeOfRangesField = type.getField( "ranges" ).getType();
        assertTrue( typeOfRangesField instanceof GraphQLNonNull );
        GraphQLType listTypeOfRangesField = ( (GraphQLNonNull) typeOfRangesField ).getOriginalWrappedType();
        assertEquals( "NumberRangeInput",
                      getNameForGraphQLTypeReference( ( (GraphQLList) listTypeOfRangesField ).getOriginalWrappedType() ) );
    }

    private void verifyDateHistogramAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "DateHistogramAggregationInput" );

        assertEquals( "DateHistogram aggregation input type", type.getDescription() );

        assertEquals( 4, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );

        assertEquals( Scalars.GraphQLString, type.getField( "format" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "interval" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "minDocCount" ).getType() );
    }

    private void verifyDateRangeAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "DateRangeAggregationInput" );

        assertEquals( "DateRange aggregation input type", type.getDescription() );

        assertEquals( 3, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );

        GraphQLInputType typeOfRangesField = type.getField( "ranges" ).getType();
        assertTrue( typeOfRangesField instanceof GraphQLList );
        assertEquals( "DateRangeInput", getNameForGraphQLTypeReference( ( (GraphQLList) typeOfRangesField ).getOriginalWrappedType() ) );

        assertEquals( Scalars.GraphQLString, type.getField( "format" ).getType() );
    }

    private void verifyRangeAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "RangeAggregationInput" );

        assertEquals( "Range aggregation input type", type.getDescription() );

        assertEquals( 2, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );

        GraphQLInputType typeOfRangesField = type.getField( "ranges" ).getType();
        assertTrue( typeOfRangesField instanceof GraphQLList );
        assertEquals( "NumberRangeInput", getNameForGraphQLTypeReference( ( (GraphQLList) typeOfRangesField ).getOriginalWrappedType() ) );
    }

    private void verifyStatsAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "StatsAggregationInput" );

        assertEquals( "Stats aggregation input type", type.getDescription() );

        assertEquals( 1, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );
    }

    private void verifyTermsAggregationInput()
    {
        GraphQLInputObjectType type = context.getInputType( "TermsAggregationInput" );

        assertEquals( "Terms aggregation input type", type.getDescription() );

        assertEquals( 4, type.getFieldDefinitions().size() );
        GraphQLInputType typeOfFieldField = type.getField( "field" ).getType();
        assertTrue( typeOfFieldField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfFieldField ).getOriginalWrappedType() );
        assertEquals( Scalars.GraphQLString, type.getField( "order" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "size" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "minDocCount" ).getType() );
    }

    private void verifyGeoPointInput()
    {
        GraphQLInputObjectType type = context.getInputType( "GeoPointInput" );

        assertEquals( "Geo range input type", type.getDescription() );

        assertEquals( 2, type.getFieldDefinitions().size() );

        GraphQLInputType typeOfLatField = type.getField( "lat" ).getType();
        assertTrue( typeOfLatField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfLatField ).getOriginalWrappedType() );

        GraphQLInputType typeOfLonField = type.getField( "lon" ).getType();
        assertTrue( typeOfLonField instanceof GraphQLNonNull );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) typeOfLonField ).getOriginalWrappedType() );
    }

    private void verifyDateRangeInput()
    {
        GraphQLInputObjectType type = context.getInputType( "DateRangeInput" );

        assertEquals( "Date range input type", type.getDescription() );

        assertEquals( 3, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getField( "key" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "from" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "to" ).getType() );
    }

    private void verifyNumberRangeInput()
    {
        GraphQLInputObjectType type = context.getInputType( "NumberRangeInput" );

        assertEquals( "Number range input type", type.getDescription() );

        assertEquals( 3, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getField( "key" ).getType() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "from" ).getType() );
        assertEquals( Scalars.GraphQLFloat, type.getField( "to" ).getType() );
    }

    private void verifyProcessHtmlInput()
    {
        GraphQLInputObjectType type = context.getInputType( "ProcessHtmlInput" );

        assertEquals( "Process HTML input type", type.getDescription() );

        assertEquals( 3, type.getFieldDefinitions().size() );
        assertEquals( "UrlType", getNameForGraphQLTypeReference( type.getField( "type" ).getType() ) );
        assertEquals( Scalars.GraphQLString, type.getField( "imageSizes" ).getType() );

        GraphQLInputType typeOfImageWidths = type.getField( "imageWidths" ).getType();
        assertTrue( typeOfImageWidths instanceof GraphQLList );

        assertEquals( Scalars.GraphQLInt, ( (GraphQLList) typeOfImageWidths ).getOriginalWrappedType() );
    }

}
