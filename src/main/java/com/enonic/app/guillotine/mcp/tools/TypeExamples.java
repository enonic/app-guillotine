package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;

import com.enonic.xp.inputtype.InputTypeName;

/**
 * Three ready {@code query} documents for one content type: the newest items with a few data fields, a filtered count, and a terms
 * aggregation with an explicit order. Page sizes are small constants that stay within {@link QueryLimits} for any type.
 */
final class TypeExamples
{
    static final int LIST_FIRST = 5;

    static final int SELECTOR_FIRST = 3;

    static final int AGGREGATION_SIZE = 10;

    private static final int MAX_SELECTED = 3;

    private static final Set<String> NOT_AGGREGATED =
        Set.of( InputTypeName.HTML_AREA.toString(), InputTypeName.TEXT_AREA.toString(), InputTypeName.GEO_POINT.toString(),
                InputTypeName.ATTACHMENT_UPLOADER.toString(), InputTypeName.MEDIA_UPLOADER.toString(),
                InputTypeName.IMAGE_UPLOADER.toString(), TypeFields.ITEM_SET );

    private static final Set<String> CATEGORIES =
        Set.of( InputTypeName.TAG.toString(), InputTypeName.COMBO_BOX.toString(), InputTypeName.RADIO_BUTTON.toString(),
                InputTypeName.CHECK_BOX.toString() );

    private TypeExamples()
    {
    }

    static List<Map<String, Object>> of( final String contentType, final String graphQlType, final List<TypeFields.Field> fields )
    {
        final String typeTerm = "{term: {field: \"type\", value: {string: \"" + contentType + "\"}}}";
        final List<Map<String, Object>> examples = new ArrayList<>();

        final String data = dataSelection( fields );
        examples.add( example( "Newest first; next page with after: pageInfo.endCursor.",
                               "{ guillotine { queryDslConnection(query: " + typeTerm +
                                   ", sort: {field: \"modifiedTime\", direction: DESC}, first: " + LIST_FIRST +
                                   ") { totalCount edges { node { _id _path displayName modifiedTime" +
                                   ( data.isEmpty() ? "" : " ... on " + graphQlType + " { data { " + data + " } }" ) +
                                   " } } pageInfo { endCursor hasNext } } } }" ) );

        final TypeFields.Field filtered = fields.stream()
            .filter( TypeFields.Field::isReference )
            .findFirst()
            .or( () -> fields.stream()
                .filter( field -> !TypeFields.ITEM_SET.equals( field.input() ) && !field.input().endsWith( "Uploader" ) )
                .findFirst() )
            .orElse( null );
        final String filterField = filtered != null ? filtered.path() : "publish.first";
        examples.add( example( "Count those with " + filterField + ( filtered != null && filtered.isReference()
                                   ? "; for one target, use term with value {string: \"<its _id>\"} instead of exists."
                                   : "; use term or range instead of exists to filter by value." ),
                               "{ guillotine { queryDslConnection(query: {boolean: {filter: [" + typeTerm + ", {exists: {field: \"" +
                                   filterField + "\"}}]}}, first: 0) { totalCount } } }" ) );

        final String aggregated = aggregationField( fields ).map( TypeFields.Field::path ).orElse( "_parentPath" );
        examples.add( example( "Top values of " + aggregated + ".",
                               "{ guillotine { queryDslConnection(query: " + typeTerm + ", first: 0, aggregations: [{name: \"values\", " +
                                   "terms: {field: \"" + aggregated + "\", order: \"_count DESC\", size: " + AGGREGATION_SIZE +
                                   "}}]) { totalCount aggregationsAsJson } } }" ) );
        return examples;
    }

    /**
     * Up to {@link #MAX_SELECTED} top-level data fields that print as short values: scalars, enums and references (as path and
     * display name). Long and rich text, files and sets are left out to keep the example short.
     */
    private static String dataSelection( final List<TypeFields.Field> fields )
    {
        final List<String> selected = new ArrayList<>();
        for ( final TypeFields.Field field : fields )
        {
            if ( selected.size() == MAX_SELECTED )
            {
                break;
            }
            if ( field.nested() || NOT_AGGREGATED.contains( field.input() ) )
            {
                continue;
            }
            final GraphQLType type = GraphQLTypeUtil.unwrapAll( field.type() );
            if ( type instanceof GraphQLScalarType || type instanceof GraphQLEnumType )
            {
                selected.add( field.name() );
            }
            else if ( field.isReference() )
            {
                selected.add( field.name() + ( field.repeated() ? "(first: " + SELECTOR_FIRST + ")" : "" ) + " { _path displayName }" );
            }
        }
        return String.join( " ", selected );
    }

    /**
     * The field whose values are worth counting: a reference, else a category such as a tag or an option, else a short value; never
     * rich or long text, files or sets.
     */
    private static Optional<TypeFields.Field> aggregationField( final List<TypeFields.Field> fields )
    {
        return fields.stream()
            .filter( field -> !NOT_AGGREGATED.contains( field.input() ) )
            .min( Comparator.comparingInt( field -> field.isReference() ? 0 : CATEGORIES.contains( field.input() ) ? 1 : 2 ) );
    }

    private static Map<String, Object> example( final String use, final String query )
    {
        final Map<String, Object> example = new LinkedHashMap<>();
        example.put( "use", use );
        example.put( "query", query );
        return example;
    }

    static boolean isContent( final GraphQLType type )
    {
        if ( type instanceof GraphQLInterfaceType interfaceType )
        {
            return ContentTypeIndex.CONTENT.equals( interfaceType.getName() );
        }
        return type instanceof GraphQLObjectType objectType &&
            objectType.getInterfaces().stream().anyMatch( contentInterface -> ContentTypeIndex.CONTENT.equals( contentInterface.getName() ) );
    }
}
