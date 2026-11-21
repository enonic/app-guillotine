package com.enonic.app.guillotine.mcp.tools;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;
import com.enonic.app.guillotine.mcp.shape.ToolError;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryLimitsTest
    extends ArticleSchemaTestSupport
{
    private static final String ARTICLES = "query: {term: {field: \"type\", value: \"" + ARTICLE + "\"}}";

    private static final String ROWS_LIMIT = "content items; the limit is 2000.";

    private static final String BUCKETS_LIMIT = "aggregation buckets; the limit is 1000.";

    private static final String NESTED_TERMS = "query ($a: [AggregationInput]) { guillotine { queryDslConnection(query: {matchAll: {}}, " +
        "first: 0, aggregations: $a) { totalCount } } }";

    /**
     * The shapes of the six harmless queries the previous limit rejected in live runs: one list of up to 100 articles, with plain
     * values, an unbounded {@code tags} list, the author and the cover.
     */
    @Test
    public void testOneLevelListsOfUpToOneHundredItemsPass()
    {
        final String titles = "{ guillotine { queryDsl(" + ARTICLES + ", first: %d) { displayName ... on " + ARTICLE_TYPE +
            " { data { title } } } } }";
        final String full = "{ guillotine { queryDsl(" + ARTICLES + ", first: %d) { _path ... on " + ARTICLE_TYPE + " { data { " +
            "title tags author { displayName } cover { _path } body { processedHtml } } } } } }";
        final String connection = "{ guillotine { queryDslConnection(" + ARTICLES + ", first: %d) { totalCount edges { node { " +
            "displayName ... on " + ARTICLE_TYPE + " { data { title tags } } } } } } }";

        for ( final String query : List.of( titles.formatted( 100 ), full.formatted( 100 ), full.formatted( 30 ),
                                            connection.formatted( 100 ), titles.formatted( 25 ), full.formatted( 20 ) ) )
        {
            assertDoesNotThrow( () -> check( query ), query );
        }
    }

    @Test
    public void testListsOfValuesInsideAnItemCountNothing()
    {
        assertDoesNotThrow( () -> check( "{ guillotine { queryDsl(" + ARTICLES + ", first: 100) { ... on " + ARTICLE_TYPE + " { data { " +
                                             "tags(first: 100) author { displayName } } } } } }" ) );
    }

    @Test
    public void testRepeatedContentSelectorsCountTheItemsTheyLoad()
    {
        final String related = "{ guillotine { queryDsl(" + ARTICLES + ", first: 100) { ... on " + ARTICLE_TYPE + " { data { " +
            "related%s { _id } } } } } }";

        assertDoesNotThrow( () -> check( related.formatted( "(first: 19)" ) ) );
        assertEquals( "Query reads up to 10100 " + ROWS_LIMIT, firstSentences( rejected( related.formatted( "(first: 100)" ) ) ) );
        assertEquals( "Query reads up to 10100 " + ROWS_LIMIT, firstSentences( rejected( related.formatted( "" ) ) ) );
        assertDoesNotThrow( () -> check( "{ guillotine { get(key: \"a1\") { ... on " + ARTICLE_TYPE + " { data { related { _id } } } } } }" ) );

        final String nested = "{ guillotine { queryDsl(" + ARTICLES + ", first: 2) { ... on " + ARTICLE_TYPE + " { data { related(first: 5) { " +
            "... on " + ARTICLE_TYPE + " { data { related(first: 5) { ... on " + ARTICLE_TYPE + " { data { related { _id } } } } } } } } } } } }";
        assertEquals( "Query reads up to 5062 " + ROWS_LIMIT, firstSentences( rejected( nested ) ) );
    }

    @Test
    public void testItemSetsMultiplyTheContentTheyReference()
    {
        final String links = "{ guillotine { queryDsl(" + ARTICLES + ", first: 10) { ... on " + ARTICLE_TYPE + " { data { " +
            "links%s { %s } } } } } }";

        assertDoesNotThrow( () -> check( links.formatted( "", "label" ) ) );
        assertDoesNotThrow( () -> check( links.formatted( "(first: 10)", "target(first: 19) { _id }" ) ) );
        assertEquals( "Query reads up to 2010 " + ROWS_LIMIT,
                      firstSentences( rejected( links.formatted( "(first: 10)", "target(first: 20) { _id }" ) ) ) );
        assertEquals( "Query reads up to 2010 " + ROWS_LIMIT,
                      firstSentences( rejected( links.formatted( "", "target(first: 2) { _id }" ) ) ) );
    }

    @Test
    public void testNestedContentListsMultiply()
    {
        final String nested = "{ guillotine { queryDsl(" + ARTICLES + ", first: %d) { children(first: %d) { _id } } } }";

        assertDoesNotThrow( () -> check( nested.formatted( 100, 19 ) ) );
        assertDoesNotThrow( () -> check( nested.formatted( 20, 99 ) ) );
        assertEquals( "Query reads up to 2100 " + ROWS_LIMIT, firstSentences( rejected( nested.formatted( 100, 20 ) ) ) );

        final String deep = "{ guillotine { queryDslConnection(query: {matchAll: {}}, first: 100) { edges { node { " +
            "children(first: 100) { children(first: 100) { _id } } } } } } }";
        assertEquals( "Query reads up to 1010100 " + ROWS_LIMIT, firstSentences( rejected( deep ) ) );
    }

    @Test
    public void testListsWithoutFirstCountTheirDefaultPageSize()
    {
        assertDoesNotThrow( () -> check( "{ guillotine { queryDsl(query: {matchAll: {}}) { children { children { _id } } } } }" ) );
        assertEquals( "Query reads up to 2010 " + ROWS_LIMIT, firstSentences(
            rejected( "{ guillotine { queryDsl(query: {matchAll: {}}) { children(first: 100) { children(first: 1) { _id } } } } }" ) ) );
    }

    @Test
    public void testAliasesAndFragmentSpreadsEachCount()
    {
        final String aliases = IntStream.range( 0, 21 )
            .mapToObj( i -> "a" + i + ": queryDsl(query: {matchAll: {}}, first: 100) { _id }" )
            .collect( Collectors.joining( " " ) );
        assertEquals( "Query reads up to 2100 " + ROWS_LIMIT, firstSentences( rejected( "{ guillotine { " + aliases + " } }" ) ) );

        final String fragments = "{ guillotine { ...F ...F ...F } } fragment F on HeadlessCms { queryDsl(query: {matchAll: {}}, " +
            "first: 100) { ...C } } fragment C on Content { children(first: 6) { _id } }";
        assertEquals( "Query reads up to 2100 " + ROWS_LIMIT, firstSentences( rejected( fragments ) ) );
    }

    @Test
    public void testVariablesAndTheirDefaultsSetFirst()
    {
        final String query = "query ($n: Int = 100, $m: Int) { guillotine { queryDsl(query: {matchAll: {}}, first: $n) { " +
            "children(first: $m) { _id } } } }";

        assertDoesNotThrow( () -> check( query, Map.of( "m", 19 ) ) );
        assertTrue( rejected( query, Map.of( "m", 20 ) ).startsWith( "Query reads up to 2100 " ) );
        assertDoesNotThrow( () -> check( query, Map.of( "n", 10, "m", 100 ) ) );
    }

    @Test
    public void testDeclaredFirstDefaultCounts()
    {
        final String query = "{ guillotine { items { items(first: 100) { _id } } } }";
        final List<Consumer<GraphQLArgument.Builder>> defaults = List.of( first -> first.defaultValueProgrammatic( 30 ),
                                                                          first -> first.defaultValueLiteral(
                                                                              graphql.language.IntValue.of( 30 ) ) );
        for ( final Consumer<GraphQLArgument.Builder> declared : defaults )
        {
            assertTrue( rejected( query, schemaWithItems( declared ) ).startsWith( "Query reads up to 3030 " ) );
        }
        assertDoesNotThrow( () -> check( query, schemaWithItems( first -> {
        } ) ) );
    }

    @Test
    public void testNestedAggregationsMultiplyTheirBuckets()
    {
        final Map<String, Object> hundredByHundred = Map.of( "name", "authors", "terms", Map.of( "field", "data.author", "size", 100 ),
                                                             "subAggregations", List.of( Map.of( "name", "tags", "terms",
                                                                                                 Map.of( "field", "data.tags",
                                                                                                         "size", 100 ) ) ) );
        assertEquals( "Query asks for up to 10100 " + BUCKETS_LIMIT,
                      firstSentences( rejected( NESTED_TERMS, Map.of( "a", List.of( hundredByHundred ) ) ) ) );

        assertDoesNotThrow( () -> check( NESTED_TERMS, Map.of( "a", List.of( Map.of( "name", "authors", "terms",
                                                                                      Map.of( "field", "data.author", "size", 30 ),
                                                                                      "subAggregations", List.of(
                Map.of( "name", "tags", "terms", Map.of( "field", "data.tags", "size", 30 ) ) ) ) ) ) ) );

        final List<Map<String, Object>> mixed = List.of(
            Map.of( "name", "authors", "terms", Map.of( "field", "data.author" ), "subAggregations",
                    List.of( Map.of( "name", "s", "stats", Map.of( "field", "data.score" ) ) ) ),
            Map.of( "name", "months", "dateHistogram", Map.of( "field", "modifiedTime", "interval", "1M", "minDocCount", 1 ) ),
            Map.of( "name", "ranges", "range", Map.of( "field", "data.score", "ranges",
                                                       List.of( Map.of( "to", 1 ), Map.of( "from", 1, "to", 5 ), Map.of( "from", 5 ) ) ) ) );
        assertDoesNotThrow( () -> check( NESTED_TERMS, Map.of( "a", mixed ) ) );
        final List<Map<String, Object>> eightTimes = IntStream.range( 0, 8 ).mapToObj( i -> mixed ).flatMap( List::stream ).toList();
        assertDoesNotThrow( () -> check( NESTED_TERMS, Map.of( "a", eightTimes ) ) );
        final List<Map<String, Object>> nineTimes = IntStream.range( 0, 9 ).mapToObj( i -> mixed ).flatMap( List::stream ).toList();
        assertEquals( "Query asks for up to " + ( 9 * ( 20 + 100 + 3 ) ) + " " + BUCKETS_LIMIT,
                      firstSentences( rejected( NESTED_TERMS, Map.of( "a", nineTimes ) ) ) );
    }

    @Test
    public void testLiteralAggregationsCount()
    {
        final String literal = "{ guillotine { queryDsl(query: {matchAll: {}}, first: 11) { _id } " +
            "queryDslConnection(query: {matchAll: {}}, first: 0, aggregations: [{name: \"t\", terms: {field: \"type\", size: 100, " +
            "order: \"_count DESC\"}, subAggregations: [{name: \"u\", terms: {field: \"owner\", size: 10, order: \"_count DESC\"}}]}]) " +
            "{ totalCount } } }";
        assertEquals( "Query asks for up to 1100 " + BUCKETS_LIMIT, firstSentences( rejected( literal ) ) );
    }

    @Test
    public void testTermsWithoutOrderGetANote()
    {
        assertEquals( List.of( QueryLimits.UNORDERED_TERMS_NOTE ),
                      check( NESTED_TERMS, Map.of( "a", List.of( Map.of( "name", "t", "terms", Map.of( "field", "type" ) ) ) ) ) );
        assertEquals( List.of(), check( NESTED_TERMS, Map.of( "a", List.of(
            Map.of( "name", "t", "terms", Map.of( "field", "type", "order", "_count DESC" ) ) ) ) ) );
        assertEquals( List.of( QueryLimits.UNORDERED_TERMS_NOTE ), check(
            "{ guillotine { queryDslConnection(query: {matchAll: {}}, aggregations: {name: \"t\", terms: {field: \"type\", " +
                "order: \" \"}}) { totalCount } } }" ) );
        assertEquals( List.of(), check( "{ guillotine { queryDsl(query: {matchAll: {}}) { _id } } }" ) );
    }

    @Test
    public void testArgumentLimits()
    {
        assertEquals( "first must not exceed 100 in this tool. Use first <= 100 and page with after or offset.",
                      rejected( "{ guillotine { queryDsl(query: {matchAll: {}}, first: 101) { _id } } }" ) );
        assertEquals( "first must not exceed 100 in this tool. Use first <= 100 and page with after or offset.",
                      rejected( "query ($n: Int) { guillotine { getChildren(key: \"/\", first: $n) { _id } } }", Map.of( "n", 500 ) ) );
        assertEquals( "first must not exceed 100 in this tool. Use first <= 100 and page with after or offset.",
                      rejected( "query ($n: Int = 500) { guillotine { getChildren(key: \"/\", first: $n) { _id } } }" ) );

        final String window = "offset or after plus first must not exceed 10000 in this tool: the search index cannot page beyond " +
            "that. Narrow the query with a filter or a different sort instead of paging this deep.";
        assertEquals( window, rejected( "{ guillotine { queryDsl(query: {matchAll: {}}, offset: 9990, first: 11) { _id } } }" ) );
        assertDoesNotThrow( () -> check( "{ guillotine { queryDsl(query: {matchAll: {}}, offset: 9990, first: 10) { _id } } }" ) );
        assertEquals( window, rejected( "query ($c: String) { guillotine { queryDslConnection(query: {matchAll: {}}, after: $c, " +
                                            "first: 1) { totalCount } } }",
                                        Map.of( "c", ConnectionHelper.encodeCursor( "9999" ) ) ) );

        assertEquals( "Aggregation size must not exceed 100 in this tool. Use size <= 100, or narrow the query so fewer buckets are " +
                          "needed.", rejected( NESTED_TERMS, Map.of( "a", List.of(
            Map.of( "name", "t", "terms", Map.of( "field", "type", "size", 101 ) ) ) ) ) );
    }

    @Test
    public void testAfterOfAGuillotineConnectionMustBeACursor()
    {
        final String connection = "query ($c: String) { guillotine { queryDslConnection(query: {matchAll: {}}, after: $c, first: 1) { " +
            "totalCount } } }";
        final String refused = "after is not a cursor from a previous result. Drop after to start from the first item, or pass the " +
            "endCursor (or an edge cursor) of the page before.";

        assertDoesNotThrow( () -> check( connection, Map.of( "c", ConnectionHelper.encodeCursor( "9" ) ) ) );
        assertDoesNotThrow( () -> check( connection, Map.of() ) );
        for ( final String cursor : List.of( "abc", "%%%", "", ConnectionHelper.encodeCursor( "-1" ), ConnectionHelper.encodeCursor( "9999999999" ) ) )
        {
            assertEquals( refused, rejected( connection, Map.of( "c", cursor ) ), cursor );
        }
        assertEquals( refused, rejected( "{ guillotine { get(key: \"a1\") { childrenConnection(after: \"abc\") { totalCount } } } }" ) );
    }

    @Test
    public void testAfterOfAnotherFieldIsNotJudgedAsACursor()
    {
        final GraphQLObjectType query = GraphQLObjectType.newObject()
            .name( "Query" )
            .field( GraphQLFieldDefinition.newFieldDefinition()
                        .name( "events" )
                        .argument( GraphQLArgument.newArgument().name( "after" ).type( Scalars.GraphQLString ) )
                        .type( Scalars.GraphQLString ) )
            .build();

        assertDoesNotThrow( () -> check( "{ events(after: \"2024-01-01\") }", Map.of(), GraphQLSchema.newSchema().query( query ).build() ) );
    }

    @Test
    public void testDateHistogramsNeedMinDocCount()
    {
        final String refused = "dateHistogram needs minDocCount of at least 1 in this tool. Pass minDocCount in each dateHistogram, " +
            "e.g. minDocCount: 1 to leave out empty buckets.";
        assertEquals( refused, rejected( NESTED_TERMS, Map.of( "a", List.of(
            Map.of( "name", "d", "dateHistogram", Map.of( "field", "modifiedTime", "interval", "1M" ) ) ) ) ) );
        assertEquals( refused, rejected( NESTED_TERMS, Map.of( "a", List.of(
            Map.of( "name", "d", "dateHistogram", Map.of( "field", "modifiedTime", "interval", "1M", "minDocCount", 0 ) ) ) ) ) );
        assertEquals( refused, rejected( "{ guillotine { queryDslConnection(query: {matchAll: {}}, " +
                                                                    "aggregations: [{name: \"d\", dateHistogram: {field: " +
                                                                    "\"modifiedTime\", interval: \"1d\"}}]) { totalCount } } }" ) );
        assertDoesNotThrow( () -> check( "{ guillotine { queryDslConnection(query: {matchAll: {}}, aggregations: [{name: \"d\", " +
                                             "dateHistogram: {field: \"modifiedTime\", interval: \"1M\", minDocCount: 1}}]) { " +
                                             "totalCount } } }" ) );
    }

    @Test
    public void testProjectAndBranchInsideTheQueryAreRefused()
    {
        for ( final String query : List.of( "{ guillotine(project: \"other\") { getSite { _id } } }",
                                             "query ($b: String) { guillotine(branch: $b) { getSite { _id } } }",
                                             "{ ...F } fragment F on Query { guillotine(project: \"other\") { getSite { _id } } }",
                                             "{ ... on Query { x: guillotine(branch: \"master\") { getSite { _id } } } }" ) )
        {
            assertTrue( rejected( query ).startsWith( "Pass project and branch as tool arguments, not inside the query." ), query );
        }
        assertDoesNotThrow( () -> check( "{ guillotine(scopeKey: \"/site\") { getSite { _id } } }" ) );
    }

    private List<String> check( final String query )
    {
        return check( query, Map.of() );
    }

    private List<String> check( final String query, final Map<String, Object> variables )
    {
        return check( query, variables, schemaProvider.getSchema() );
    }

    private void check( final String query, final GraphQLSchema schema )
    {
        check( query, Map.of(), schema );
    }

    private static List<String> check( final String query, final Map<String, Object> variables, final GraphQLSchema schema )
    {
        final Document document = new Parser().parseDocument( query );
        final OperationDefinition operation = document.getDefinitionsOfType( OperationDefinition.class ).get( 0 );
        return QueryLimits.check( document, operation, variables, () -> schema );
    }

    private String rejected( final String query )
    {
        return rejected( query, Map.of() );
    }

    private String rejected( final String query, final Map<String, Object> variables )
    {
        return assertThrows( ToolError.class, () -> check( query, variables ) ).userMessage();
    }

    private static String rejected( final String query, final GraphQLSchema schema )
    {
        return assertThrows( ToolError.class, () -> check( query, Map.of(), schema ) ).userMessage();
    }

    private static String firstSentences( final String message )
    {
        return message.substring( 0, message.indexOf( ". " ) + 1 );
    }

    private static GraphQLSchema schemaWithItems( final Consumer<GraphQLArgument.Builder> firstDefault )
    {
        final GraphQLArgument.Builder first = GraphQLArgument.newArgument().name( "first" ).type( Scalars.GraphQLInt );
        firstDefault.accept( first );
        final GraphQLObjectType item = GraphQLObjectType.newObject()
            .name( "Item" )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "_id" ).type( Scalars.GraphQLString ) )
            .field( GraphQLFieldDefinition.newFieldDefinition()
                        .name( "items" )
                        .argument( first )
                        .argument( GraphQLArgument.newArgument().name( "sort" ).type( Scalars.GraphQLString ) )
                        .type( GraphQLList.list( graphql.schema.GraphQLTypeReference.typeRef( "Item" ) ) ) )
            .build();
        final GraphQLObjectType guillotine = GraphQLObjectType.newObject()
            .name( "Guillotine" )
            .field( GraphQLFieldDefinition.newFieldDefinition()
                        .name( "items" )
                        .argument( first )
                        .argument( GraphQLArgument.newArgument().name( "sort" ).type( Scalars.GraphQLString ) )
                        .type( GraphQLList.list( item ) ) )
            .build();
        return GraphQLSchema.newSchema()
            .query( GraphQLObjectType.newObject()
                        .name( "Query" )
                        .field( GraphQLFieldDefinition.newFieldDefinition().name( "guillotine" ).type( guillotine ) ) )
            .build();
    }
}
