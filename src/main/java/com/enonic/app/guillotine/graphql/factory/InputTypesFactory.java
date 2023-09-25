package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.scalars.CustomScalars;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.inputField;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newInputObject;

public class InputTypesFactory
{
    private final GuillotineContext context;

    public InputTypesFactory( final GuillotineContext context )
    {
        this.context = context;
    }

    public void create()
    {
        createProcessHtmlType();
        createNumberRangeInputType();
        createDateRangeInputType();
        createGeoPointInputType();

        createTermsAggregationInputType();
        createStatsAggregationInputType();
        createRangeAggregationInputType();
        createDateRangeAggregationInputType();
        createDateHistogramAggregationInputType();
        createGeoDistanceAggregationInputType();
        createMinAggregationInputType();
        createMaxAggregationInputType();
        createValueCountAggregationInputType();
        createAggregationInputType();

        createExistsFilterInputType();
        createNotExistsFilterInputType();
        createHasValueFilterInputType();
        createIdsFilterInputType();
        createBooleanFilterInputType();
        createFilterInputType();

        createDslExpressionValueInputType();
        createTermExpressionDslInputType();
        createLikeDslExpressionInputType();
        createInDslExpressionInputType();
        createExistsDslExpressionInputType();
        createStemmedDslExpressionInputType();
        createFulltextDslExpressionInputType();
        createNgramDslExpressionInputType();
        createPathMatchDslExpressionInputType();
        createMatchAllDslExpressionInputType();
        createRangeDslExpressionInputType();
        createBooleanExpressionDslInputType();
        createQueryDslInputType();
        createGeoPointDslInputType();
        createSortDslInputType();

        createHighlightPropertiesInputType();
        createHighlightInputType();
    }

    private void createProcessHtmlType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "type", GraphQLTypeReference.typeRef( "UrlType" ) ) );
        fields.add( inputField( "imageWidths", new GraphQLList( Scalars.GraphQLInt ) ) );
        fields.add( inputField( "imageSizes", Scalars.GraphQLString ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "ProcessHtmlInput" ), "Process HTML input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createNumberRangeInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "key", Scalars.GraphQLString ) );
        fields.add( inputField( "from", Scalars.GraphQLFloat ) );
        fields.add( inputField( "to", Scalars.GraphQLFloat ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "NumberRangeInput" ), "Number range input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createDateRangeInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "key", Scalars.GraphQLString ) );
        fields.add( inputField( "from", Scalars.GraphQLString ) );
        fields.add( inputField( "to", Scalars.GraphQLString ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "DateRangeInput" ), "Date range input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createGeoPointInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "lat", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "lon", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "GeoPointInput" ), "Geo range input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createTermsAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "order", Scalars.GraphQLString ) );
        fields.add( inputField( "size", Scalars.GraphQLInt ) );
        fields.add( inputField( "minDocCount", Scalars.GraphQLInt ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "TermsAggregationInput" ), "Terms aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createStatsAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "StatsAggregationInput" ), "Stats aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createRangeAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "ranges", new GraphQLList( GraphQLTypeReference.typeRef( "NumberRangeInput" ) ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "RangeAggregationInput" ), "Range aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createDateRangeAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "format", Scalars.GraphQLString ) );
        fields.add( inputField( "ranges", new GraphQLList( GraphQLTypeReference.typeRef( "DateRangeInput" ) ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "DateRangeAggregationInput" ), "DateRange aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createDateHistogramAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "interval", Scalars.GraphQLString ) );
        fields.add( inputField( "format", Scalars.GraphQLString ) );
        fields.add( inputField( "minDocCount", Scalars.GraphQLInt ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "DateHistogramAggregationInput" ), "DateHistogram aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createGeoDistanceAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "unit", Scalars.GraphQLString ) );
        fields.add( inputField( "origin", new GraphQLNonNull( GraphQLTypeReference.typeRef( "GeoPointInput" ) ) ) );
        fields.add( inputField( "ranges", new GraphQLNonNull( new GraphQLList( GraphQLTypeReference.typeRef( "NumberRangeInput" ) ) ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "GeoDistanceAggregationInput" ), "GeoDistance aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createMinAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "MinAggregationInput" ), "MinAggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createMaxAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "MaxAggregationInput" ), "MaxAggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createValueCountAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "ValueCountAggregationInput" ), "ValueCount Aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createAggregationInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "subAggregations", new GraphQLList( GraphQLTypeReference.typeRef( "AggregationInput" ) ) ) );
        fields.add( inputField( "name", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "terms", GraphQLTypeReference.typeRef( "TermsAggregationInput" ) ) );
        fields.add( inputField( "stats", GraphQLTypeReference.typeRef( "StatsAggregationInput" ) ) );
        fields.add( inputField( "range", GraphQLTypeReference.typeRef( "RangeAggregationInput" ) ) );
        fields.add( inputField( "dateRange", GraphQLTypeReference.typeRef( "DateRangeAggregationInput" ) ) );
        fields.add( inputField( "dateHistogram", GraphQLTypeReference.typeRef( "DateHistogramAggregationInput" ) ) );
        fields.add( inputField( "geoDistance", GraphQLTypeReference.typeRef( "GeoDistanceAggregationInput" ) ) );
        fields.add( inputField( "min", GraphQLTypeReference.typeRef( "MinAggregationInput" ) ) );
        fields.add( inputField( "max", GraphQLTypeReference.typeRef( "MaxAggregationInput" ) ) );
        fields.add( inputField( "count", GraphQLTypeReference.typeRef( "ValueCountAggregationInput" ) ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "AggregationInput" ), "Aggregation input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createExistsFilterInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "ExistsFilterInput" ), "ExistsFilter input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createNotExistsFilterInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "NotExistsFilterInput" ), "NotExistsFilter input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createHasValueFilterInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "stringValues", new GraphQLList( Scalars.GraphQLString ) ) );
        fields.add( inputField( "intValues", new GraphQLList( Scalars.GraphQLInt ) ) );
        fields.add( inputField( "floatValues", new GraphQLList( Scalars.GraphQLFloat ) ) );
        fields.add( inputField( "booleanValues", new GraphQLList( Scalars.GraphQLBoolean ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "HasValueFilterInput" ), "HasValueFilter input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createIdsFilterInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "values", new GraphQLList( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "IdsFilterInput" ), "IdsFilter input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createBooleanFilterInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "must", new GraphQLList( GraphQLTypeReference.typeRef( "FilterInput" ) ) ) );
        fields.add( inputField( "mustNot", new GraphQLList( GraphQLTypeReference.typeRef( "FilterInput" ) ) ) );
        fields.add( inputField( "should", new GraphQLList( GraphQLTypeReference.typeRef( "FilterInput" ) ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "BooleanFilterInput" ), "BooleanFilter input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createFilterInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "boolean", GraphQLTypeReference.typeRef( "BooleanFilterInput" ) ) );
        fields.add( inputField( "exists", GraphQLTypeReference.typeRef( "ExistsFilterInput" ) ) );
        fields.add( inputField( "notExists", GraphQLTypeReference.typeRef( "NotExistsFilterInput" ) ) );
        fields.add( inputField( "hasValue", GraphQLTypeReference.typeRef( "HasValueFilterInput" ) ) );
        fields.add( inputField( "ids", GraphQLTypeReference.typeRef( "IdsFilterInput" ) ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "FilterInput" ), "Filter input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createDslExpressionValueInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "string", Scalars.GraphQLString ) );
        fields.add( inputField( "double", Scalars.GraphQLFloat ) );
        fields.add( inputField( "long", Scalars.GraphQLInt ) );
        fields.add( inputField( "boolean", Scalars.GraphQLBoolean ) );
        fields.add( inputField( "localDate", ExtendedScalars.Date ) );
        fields.add( inputField( "localDateTime", CustomScalars.LocalDateTime ) );
        fields.add( inputField( "localTime", CustomScalars.LocalTime ) );
        fields.add( inputField( "instant", ExtendedScalars.DateTime ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "DSLExpressionValueInput" ), "DSLExpressionValueInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createTermExpressionDslInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );
        fields.add( inputField( "value", new GraphQLNonNull( GraphQLTypeReference.typeRef( "DSLExpressionValueInput" ) ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "TermDSLExpressionInput" ), "TermDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createLikeDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );
        fields.add( inputField( "value", new GraphQLNonNull( Scalars.GraphQLString ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "LikeDSLExpressionInput" ), "LikeDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createInDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );
        fields.add( inputField( "stringValues", new GraphQLList( Scalars.GraphQLString ) ) );
        fields.add( inputField( "doubleValues", new GraphQLList( Scalars.GraphQLFloat ) ) );
        fields.add( inputField( "longValues", new GraphQLList( Scalars.GraphQLInt ) ) );
        fields.add( inputField( "booleanValues", new GraphQLList( Scalars.GraphQLBoolean ) ) );
        fields.add( inputField( "localDateValues", new GraphQLList( ExtendedScalars.Date ) ) );
        fields.add( inputField( "localDateTimeValues", new GraphQLList( CustomScalars.LocalDateTime ) ) );
        fields.add( inputField( "localTimeValues", new GraphQLList( CustomScalars.LocalTime ) ) );
        fields.add( inputField( "instantValues", new GraphQLList( ExtendedScalars.DateTime ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "InDSLExpressionInput" ), "InDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createExistsDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "ExistsDSLExpressionInput" ), "ExistsDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createStemmedDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "fields", new GraphQLNonNull( new GraphQLList( Scalars.GraphQLString ) ) ) );
        fields.add( inputField( "query", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "language", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "operator", GraphQLTypeReference.typeRef( "DslOperatorType" ) ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "StemmedDSLExpressionInput" ), "StemmedDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createFulltextDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "fields", new GraphQLNonNull( new GraphQLList( Scalars.GraphQLString ) ) ) );
        fields.add( inputField( "query", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "operator", GraphQLTypeReference.typeRef( "DslOperatorType" ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "FulltextDSLExpressionInput" ), "FulltextDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createNgramDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "fields", new GraphQLNonNull( new GraphQLList( Scalars.GraphQLString ) ) ) );
        fields.add( inputField( "query", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "operator", GraphQLTypeReference.typeRef( "DslOperatorType" ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "NgramDSLExpressionInput" ), "NgramDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createPathMatchDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "path", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "minimumMatch", Scalars.GraphQLInt ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "PathMatchDSLExpressionInput" ), "PathMatchDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createMatchAllDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "MatchAllDSLExpressionInput" ), "MatchAllDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createRangeDslExpressionInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "lt", GraphQLTypeReference.typeRef( "DSLExpressionValueInput" ) ) );
        fields.add( inputField( "lte", GraphQLTypeReference.typeRef( "DSLExpressionValueInput" ) ) );
        fields.add( inputField( "gt", GraphQLTypeReference.typeRef( "DSLExpressionValueInput" ) ) );
        fields.add( inputField( "gte", GraphQLTypeReference.typeRef( "DSLExpressionValueInput" ) ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "RangeDSLExpressionInput" ), "RangeDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createBooleanExpressionDslInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "should", new GraphQLList( GraphQLTypeReference.typeRef( "QueryDSLInput" ) ) ) );
        fields.add( inputField( "must", new GraphQLList( GraphQLTypeReference.typeRef( "QueryDSLInput" ) ) ) );
        fields.add( inputField( "mustNot", new GraphQLList( GraphQLTypeReference.typeRef( "QueryDSLInput" ) ) ) );
        fields.add( inputField( "filter", new GraphQLList( GraphQLTypeReference.typeRef( "QueryDSLInput" ) ) ) );
        fields.add( inputField( "boost", Scalars.GraphQLFloat ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "BooleanDSLExpressionInput" ), "BooleanDSLExpressionInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createQueryDslInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "boolean", GraphQLTypeReference.typeRef( "BooleanDSLExpressionInput" ) ) );
        fields.add( inputField( "ngram", GraphQLTypeReference.typeRef( "NgramDSLExpressionInput" ) ) );
        fields.add( inputField( "stemmed", GraphQLTypeReference.typeRef( "StemmedDSLExpressionInput" ) ) );
        fields.add( inputField( "fulltext", GraphQLTypeReference.typeRef( "FulltextDSLExpressionInput" ) ) );
        fields.add( inputField( "matchAll", GraphQLTypeReference.typeRef( "MatchAllDSLExpressionInput" ) ) );
        fields.add( inputField( "pathMatch", GraphQLTypeReference.typeRef( "PathMatchDSLExpressionInput" ) ) );
        fields.add( inputField( "range", GraphQLTypeReference.typeRef( "RangeDSLExpressionInput" ) ) );
        fields.add( inputField( "term", GraphQLTypeReference.typeRef( "TermDSLExpressionInput" ) ) );
        fields.add( inputField( "like", GraphQLTypeReference.typeRef( "LikeDSLExpressionInput" ) ) );
        fields.add( inputField( "in", GraphQLTypeReference.typeRef( "InDSLExpressionInput" ) ) );
        fields.add( inputField( "exists", GraphQLTypeReference.typeRef( "ExistsDSLExpressionInput" ) ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "QueryDSLInput" ), "QueryDSLInput type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createGeoPointDslInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "lat", new GraphQLNonNull( Scalars.GraphQLFloat ) ) );
        fields.add( inputField( "lon", new GraphQLNonNull( Scalars.GraphQLFloat ) ) );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "GeoPointSortDslInput" ), "GeoPoint Sort Dsl input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createSortDslInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "field", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( inputField( "direction", GraphQLTypeReference.typeRef( "DslSortDirectionType" ) ) );
        fields.add( inputField( "location", GraphQLTypeReference.typeRef( "GeoPointSortDslInput" ) ) );
        fields.add( inputField( "unit", GraphQLTypeReference.typeRef( "DslGeoPointDistanceType" ) ) );

        GraphQLInputObjectType inputObject = newInputObject( context.uniqueName( "SortDslInput" ), "Sort Dsl input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createHighlightPropertiesInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "propertyName", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.addAll( createHighlightCommonFields() );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "HighlightPropertiesInputType" ), "HighlightProperties input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private void createHighlightInputType()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "properties",
                                new GraphQLNonNull( new GraphQLList( GraphQLTypeReference.typeRef( "HighlightPropertiesInputType" ) ) ) ) );
        fields.add( inputField( "encoder", GraphQLTypeReference.typeRef( "HighlightEncoderType" ) ) );
        fields.add( inputField( "tagsSchema", GraphQLTypeReference.typeRef( "HighlightTagsSchemaType" ) ) );
        fields.addAll( createHighlightCommonFields() );

        GraphQLInputObjectType inputObject =
            newInputObject( context.uniqueName( "HighlightInputType" ), "HighlightInputType input type", fields );
        context.registerType( inputObject.getName(), inputObject );
    }

    private List<GraphQLInputObjectField> createHighlightCommonFields()
    {
        List<GraphQLInputObjectField> fields = new ArrayList<>();

        fields.add( inputField( "fragmenter", GraphQLTypeReference.typeRef( "HighlightFragmenterType" ) ) );
        fields.add( inputField( "fragmentSize", Scalars.GraphQLInt ) );
        fields.add( inputField( "noMatchSize", Scalars.GraphQLInt ) );
        fields.add( inputField( "numberOfFragments", Scalars.GraphQLInt ) );
        fields.add( inputField( "order", GraphQLTypeReference.typeRef( "HighlightOrderType" ) ) );
        fields.add( inputField( "preTag", Scalars.GraphQLString ) );
        fields.add( inputField( "postTag", Scalars.GraphQLString ) );
        fields.add( inputField( "requireFieldMatch", Scalars.GraphQLBoolean ) );

        return fields;
    }

}
