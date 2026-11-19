package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.language.Document;
import graphql.language.IntValue;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mcp.shape.GraphQlErrors;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentIdsByParentResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.exception.ForbiddenAccessException;

import static com.enonic.app.guillotine.mcp.McpResults.roundTrip;
import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class QueryToolTest
    extends ArticleSchemaTestSupport
{
    private static final AuthenticationInfo ALICE = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).build() )
        .principals( RoleKeys.AUTHENTICATED, RoleKeys.EVERYONE )
        .build();

    private static final String DEEP_QUERY = "{ guillotine { get(key: \"a1\") { parent { parent { parent { _id } } } } } }";

    private QueryTool tool()
    {
        return new QueryTool( schemaProvider, guillotineConfigService, resolver );
    }

    private Map<String, Object> run( final Map<String, Object> args )
    {
        return createAdminContext().callWith( () -> tool().execute( args ) );
    }

    @Test
    public void testQueryUsesResolvedProjectWithoutGuillotineArguments()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );

        final Map<String, Object> result =
            run( Map.of( "query", "{ guillotine { get(key: \"a1\") { _id displayName _project _branch } } }" ) );

        assertNull( result.get( "isError" ) );
        final Map<String, Object> structured = structured( roundTrip( result ) );
        assertEquals( "myproject", structured.get( "project" ) );
        assertEquals( "draft", structured.get( "branch" ) );
        assertEquals( Map.of( "guillotine", Map.of( "get", Map.of( "_id", "a1", "displayName", "First", "_project", "myproject",
                                                                     "_branch", "draft" ) ) ), structured.get( "data" ) );
        assertTrue( text( result ).startsWith( "Query executed on project myproject (draft).\n" ) );
    }

    @Test
    public void testMutationRejectedBeforeExecution()
    {
        final SchemaProvider provider = mock( SchemaProvider.class );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );

        final ToolError error = assertThrows( ToolError.class, () -> tool.execute(
            Map.of( "query", "query Q { guillotine { get(key: \"a\") { _id } } } mutation M { delete(key: \"a\") }",
                    "operationName", "Q" ) ) );

        assertEquals( "Only query operations are allowed, but the document contains a mutation 'M'. " +
                          "This tool is read-only: remove the mutation and send query operations only.", error.userMessage() );
        verifyNoInteractions( provider, resolver );
    }

    @Test
    public void testSubscriptionRejectedBeforeExecution()
    {
        final SchemaProvider provider = mock( SchemaProvider.class );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );

        final ToolError error =
            assertThrows( ToolError.class, () -> tool.execute( Map.of( "query", "subscription { changes { _id } }" ) ) );

        assertTrue( error.userMessage().startsWith( "Only query operations are allowed, but the document contains a subscription." ) );
        verifyNoInteractions( provider, resolver );
    }

    @Test
    public void testQueryDeeperThanLimitIsRejected()
    {
        when( guillotineConfigService.getMcpMaxDepth() ).thenReturn( 4 );

        final Map<String, Object> result = run( Map.of( "query", DEEP_QUERY ) );

        assertEquals( true, result.get( "isError" ) );
        assertEquals( "Query failed: Query depth 6 exceeds the limit of 4 nested levels (mcp.maxDepth). " +
                          "Select fewer nested fields, or split the query into several calls.", text( result ) );
        verifyNoInteractions( contentService );
    }

    @Test
    public void testQueryWithinDepthLimitRuns()
    {
        when( guillotineConfigService.getMcpMaxDepth() ).thenReturn( 6 );

        final Map<String, Object> result = run( Map.of( "query", DEEP_QUERY ) );

        assertNull( result.get( "isError" ), () -> text( result ) );
    }

    @Test
    public void testComplexityLimit()
    {
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 10 );
        final String aliases =
            IntStream.range( 0, 10 ).mapToObj( i -> "a" + i + ": get(key: \"a1\") { _id }" ).collect( Collectors.joining( " " ) );

        final ToolError error = assertThrows( ToolError.class, () -> run( Map.of( "query", "{ guillotine { " + aliases + " } }" ) ) );

        assertEquals( "Query complexity 21 exceeds the limit of 10 (mcp.maxComplexity); every field counts, multiplied by the first " +
                          "argument of the lists around it. Select fewer fields, use a smaller first and paginate, or split the query " +
                          "into several calls.", error.userMessage() );
        verifyNoInteractions( contentService, resolver );
    }

    @Test
    public void testOperationNamePicksOneOperation()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );
        when( contentService.getById( ContentId.from( "a2" ) ) ).thenReturn( article( "a2", "Second" ) );
        final String document = "query First { guillotine { get(key: \"a1\") { displayName } } } " +
            "query Second($key: ID) { guillotine { get(key: $key) { displayName } } }";

        final Map<String, Object> result =
            run( Map.of( "query", document, "operationName", "Second", "variables", Map.of( "key", "a2" ) ) );

        assertEquals( Map.of( "guillotine", Map.of( "get", Map.of( "displayName", "Second" ) ) ), structured( result ).get( "data" ) );
    }

    @Test
    public void testUnknownOperationName()
    {
        final String document = "query First { guillotine { get(key: \"a1\") { _id } } } query Second { guillotine { " +
            "get(key: \"a2\") { _id } } }";

        final ToolError error =
            assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", document, "operationName", "Third" ) ) );

        assertEquals( "Operation 'Third' is not in the document. Operations in the document: First, Second.", error.userMessage() );
    }

    @Test
    public void testSeveralOperationsRequireOperationName()
    {
        final ToolError error = assertThrows( ToolError.class, () -> tool().execute(
            Map.of( "query", "query A { guillotine { getSite { _id } } } { guillotine { getSite { _id } } }" ) ) );

        assertEquals( "The document contains 2 operations; operationName is required. Pass operationName, one of: A, (anonymous).",
                      error.userMessage() );
    }

    @Test
    public void testSyntaxError()
    {
        final ToolError error = assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", "{ guillotine { get( } }" ) ) );

        assertTrue( error.userMessage().startsWith( "Invalid GraphQL document: " ), error.userMessage() );
        assertTrue( error.userMessage().endsWith( "Fix the syntax; call describe-type to get a valid example query." ) );
    }

    @Test
    public void testInvalidArguments()
    {
        assertEquals( "query is required and must be a GraphQL document string. Call describe-type first to get a ready example query.",
                      assertThrows( ToolError.class, () -> tool().execute( Map.of() ) ).userMessage() );
        assertEquals( "Invalid variables: expected a JSON object. Pass variables as a JSON object keyed by variable name, without the $.",
                      assertThrows( ToolError.class,
                                    () -> tool().execute( Map.of( "query", "{ guillotine { getSite { _id } } }", "variables",
                                                                  List.of( 1 ) ) ) ).userMessage() );
        assertEquals( "Invalid operationName: 5. Pass the name of one query operation from the document.",
                      assertThrows( ToolError.class,
                                    () -> tool().execute( Map.of( "query", "{ guillotine { getSite { _id } } }", "operationName",
                                                                  5 ) ) ).userMessage() );
    }

    @Test
    public void testPartialErrorsNextToData()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );

        final Map<String, Object> result = run( Map.of( "query", "{ ok: guillotine { get(key: \"a1\") { _id } } " +
            "bad: guillotine(scopeKey: \"missing\") { get(key: \"a1\") { _id } } }" ) );

        assertNull( result.get( "isError" ) );
        final Map<String, Object> structured = structured( roundTrip( result ) );
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "ok", Map.of( "get", Map.of( "_id", "a1" ) ) );
        data.put( "bad", null );
        assertEquals( data, structured.get( "data" ) );
        final List<Map<String, Object>> errors = CastHelper.cast( structured.get( "errors" ) );
        assertEquals( 1, errors.size() );
        assertEquals( List.of( "bad" ), errors.get( 0 ).get( "path" ) );
        assertEquals( "Content for the \"scopeKey\" argument not found: \"missing\"", errors.get( 0 ).get( "message" ) );
        assertTrue( text( result ).startsWith( "Query executed on project myproject (draft) with 1 error; see errors next to data." ) );
    }

    @Test
    public void testValidationErrorIsErrorResult()
    {
        final Map<String, Object> result = run( Map.of( "query", "{ guillotine { nope } }" ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( text( result ).startsWith( "Query failed: " ), text( result ) );
        assertTrue( text( result ).contains( "nope" ) );
        assertFalse( text( result ).contains( "graphql." ) );
    }

    @Test
    public void testUndefinedRootFieldGetsNextStep()
    {
        final Map<String, Object> result = run( Map.of( "query", "{ bogus }" ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( text( result ).contains( "bogus" ), text( result ) );
        assertTrue( text( result ).endsWith( GraphQlErrors.VALIDATION_HINT ), text( result ) );
    }

    @Test
    public void testHugeResponseTruncatedWithinBudget()
    {
        when( guillotineConfigService.getMcpMaxResultChars() ).thenReturn( 1000 );
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "\"Long\" \\ title ".repeat( 20 ) ) );
        final String aliases =
            IntStream.range( 0, 30 ).mapToObj( i -> "a" + i + ": get(key: \"a1\") { displayName }" ).collect( Collectors.joining( " " ) );

        final Map<String, Object> result = run( Map.of( "query", "{ guillotine { " + aliases + " } }" ) );

        final String json = McpJson.write( result );
        final Map<String, Object> parsed = McpJson.readObject( json );
        final Map<String, Object> structured = structured( parsed );
        assertTrue( McpJson.length( structured ) <= 1000, () -> "structured result is " + McpJson.length( structured ) + " chars" );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( "myproject", structured.get( "project" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.startsWith( "Result truncated to fit 1000 characters" ), hint );
        assertTrue( hint.endsWith( ". Select fewer fields or pass a smaller first." ), hint );
        final String text = text( parsed );
        assertEquals( structured, McpJson.readObject( text.substring( text.indexOf( '\n' ) + 1 ) ) );
    }

    @Test
    public void testFieldsResolvingToNullStayInData()
    {
        final Map<String, Object> result = run( Map.of( "query", "{ guillotine { get(key: \"/nope\") { _path } } }" ) );

        assertNull( result.get( "isError" ) );
        final Map<String, Object> structured = structured( roundTrip( result ) );
        final Map<String, Object> guillotine = CastHelper.cast( CastHelper.<Map<String, Object>>cast( structured.get( "data" ) ).get( "guillotine" ) );
        assertTrue( guillotine.containsKey( "get" ), guillotine::toString );
        assertNull( guillotine.get( "get" ) );
        assertTrue( text( result ).endsWith( "{\"guillotine\":{\"get\":null}}}" ), text( result ) );
    }

    @Test
    public void testHttpPathIgnoresMcpLimits()
    {
        when( guillotineConfigService.getMcpMaxDepth() ).thenReturn( 2 );
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 2 );

        final Map<String, Object> specification =
            createAdminContext().callWith( () -> schemaProvider.executeToSpecification( DEEP_QUERY, Map.of() ) );
        assertNull( specification.get( "errors" ), () -> String.valueOf( specification.get( "errors" ) ) );
        assertTrue( specification.containsKey( "data" ) );

        final Map<String, Object> viaApi = executeQuery( schemaProvider.getSchema(), DEEP_QUERY );
        assertFalse( viaApi.containsKey( "errors" ), () -> String.valueOf( viaApi.get( "errors" ) ) );

        assertThrows( ToolError.class, () -> run( Map.of( "query", DEEP_QUERY ) ) );
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 500 );
        assertEquals( true, run( Map.of( "query", DEEP_QUERY ) ).get( "isError" ) );
    }

    @Test
    public void testExtraInstrumentationOnlyWhenPassed()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );

        final Map<String, Object> withoutLimits = createAdminContext().callWith(
            () -> schemaProvider.executeToSpecification( DEEP_QUERY, null, null, null ) );
        assertNull( withoutLimits.get( "errors" ) );

        final Map<String, Object> limited = createAdminContext().callWith(
            () -> schemaProvider.executeToSpecification( DEEP_QUERY, null, null, QueryLimits.depthLimit( 2 ) ) );
        assertTrue( String.valueOf( limited.get( "errors" ) ).contains( "exceeds the limit of 2" ) );
    }

    @Test
    public void testManyLongErrorsStayWithinBudget()
    {
        final String key = "k".repeat( 4096 );
        final String aliases = IntStream.range( 0, 150 )
            .mapToObj( i -> "a" + i + ": guillotine(scopeKey: \"" + key + "\") { getSite { _id } }" )
            .collect( Collectors.joining( " " ) );

        final Map<String, Object> result = run( Map.of( "query", "{ " + aliases + " }" ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( McpJson.length( result ) <= guillotineConfigService.getMcpMaxResultChars() );
        assertTrue( McpJson.length( result ) <= 1000, () -> "error result is " + McpJson.length( result ) + " chars" );
        assertTrue( text( result ).startsWith( "Query failed: a0: Content for the \"scopeKey\" argument not found: \"kkk" ), text( result ) );
    }

    @Test
    public void testManyPartialErrorsStayWithinBudget()
    {
        when( guillotineConfigService.getMcpMaxResultChars() ).thenReturn( 1000 );
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );
        final String key = "k".repeat( 4096 );
        final String aliases = IntStream.range( 0, 100 )
            .mapToObj( i -> "a" + i + ": guillotine(scopeKey: \"" + key + "\") { getSite { _id } }" )
            .collect( Collectors.joining( " " ) );

        final Map<String, Object> result =
            run( Map.of( "query", "{ ok: guillotine { get(key: \"a1\") { _id } } " + aliases + " }" ) );

        assertNull( result.get( "isError" ) );
        assertTrue( McpJson.length( result ) <= 1000, () -> "result is " + McpJson.length( result ) + " chars" );
        McpJson.readObject( McpJson.write( result ) );
    }

    @Test
    public void testUserInputIsNotEchoedInFull()
    {
        final String longName = "Op" + "x".repeat( 10000 );
        final ToolError unknown = assertThrows( ToolError.class, () -> tool().execute(
            Map.of( "query", "query A { guillotine { getSite { _id } } }", "operationName", longName ) ) );
        assertTrue( unknown.userMessage().length() < 400, unknown.userMessage() );

        final ToolError invalid =
            assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", "{ a }", "operationName", List.of( longName ) ) ) );
        assertTrue( invalid.userMessage().length() < 400, invalid.userMessage() );

        final ToolError variables =
            assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", "{ a }", "variables", longName ) ) );
        assertFalse( variables.userMessage().contains( "xxx" ) );

        final ToolError syntax = assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", "{ " + longName + "(" ) ) );
        assertTrue( syntax.userMessage().length() < 400, syntax.userMessage() );
        assertFalse( syntax.userMessage().contains( ".." ), syntax.userMessage() );
    }

    @Test
    public void testProjectAndBranchInsideQueryAreRejected()
    {
        final SchemaProvider provider = mock( SchemaProvider.class );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );
        final List<Map<String, Object>> forms = List.of( Map.of( "query", "{ guillotine(project: \"other\") { getSite { _id } } }" ),
                                                         Map.of( "query",
                                                                 "query ($b: String) { guillotine(branch: $b) { getSite { _id } } }",
                                                                 "variables", Map.of( "b", "master" ) ),
                                                         Map.of( "query", "{ x: guillotine(project: \"other\", branch: \"master\") { " +
                                                             "getSite { _id } } }" ),
                                                         Map.of( "query", "{ ...F } fragment F on Query { guillotine(project: \"other\") " +
                                                             "{ getSite { _id } } }" ),
                                                         Map.of( "query", "{ ... on Query { guillotine(branch: \"master\") { " +
                                                             "getSite { _id } } } }" ) );

        for ( final Map<String, Object> args : forms )
        {
            final ToolError error = assertThrows( ToolError.class, () -> tool.execute( args ), () -> args.toString() );
            assertTrue( error.userMessage().startsWith( "Pass project and branch as tool arguments, not inside the query." ),
                        error.userMessage() );
        }
        verifyNoInteractions( provider, resolver );
    }

    @Test
    public void testFirstAboveLimitIsRejectedBeforeExecution()
    {
        final SchemaProvider provider = mock( SchemaProvider.class );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );
        final List<Map<String, Object>> forms = List.of( Map.of( "query", "{ guillotine { queryDslConnection(first:10000){edges{node{" +
                                                             "children(first:1000){children(first:1000){_id}}}}} } }" ),
                                                         Map.of( "query", "query ($n: Int) { guillotine { getChildren(key: \"/\", " +
                                                             "first: $n) { _id } } }", "variables", Map.of( "n", 101 ) ),
                                                         Map.of( "query", "query ($n: Int = 500) { guillotine { getChildren(key: " +
                                                             "\"/\", first: $n) { _id } } }" ) );

        for ( final Map<String, Object> args : forms )
        {
            final ToolError error = assertThrows( ToolError.class, () -> tool.execute( args ), () -> args.toString() );
            assertEquals( "first must not exceed 100 in this tool. Use first <= 100 and paginate with after, or use the search tool " +
                              "for paging.", error.userMessage() );
        }
        verifyNoInteractions( provider, resolver );
    }

    @Test
    public void testComplexityMultipliesByFirst()
    {
        final String query = "query ($n: Int) { guillotine { queryDslConnection(query: {matchAll: {}}, first: 100) { edges { node { " +
            "children(first: $n) { children(first: 100) { _id } } } } } } }";

        final ToolError large = assertThrows( ToolError.class, () -> run( Map.of( "query", query, "variables", Map.of( "n", 100 ) ) ) );
        assertTrue( large.userMessage().startsWith( "Query complexity 1010302 exceeds the limit of 500 (mcp.maxComplexity)" ),
                    large.userMessage() );

        final ToolError small = assertThrows( ToolError.class, () -> run( Map.of( "query", query, "variables", Map.of( "n", 1 ) ) ) );
        assertTrue( small.userMessage().startsWith( "Query complexity 10402 exceeds the limit of 500" ), small.userMessage() );
        verifyNoInteractions( contentService, resolver );
    }

    @Test
    public void testListFieldsWithoutFirstCountWithTheirDefaultPageSize()
    {
        final ToolError error = assertThrows( ToolError.class, () -> run(
            Map.of( "query", "{ guillotine { queryDsl(query: {matchAll: {}}) { children { children { children { _id } } } } } }" ) ) );

        assertTrue( error.userMessage().startsWith( "Query complexity 11112 exceeds the limit of 500" ), error.userMessage() );
        verifyNoInteractions( contentService, resolver );

        final String unsetFirst = "query ($n: Int) { guillotine { getChildren(key: \"/\", first: $n) { _id _path } } }";
        withoutChildren();
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 22 );
        final Map<String, Object> within = run( Map.of( "query", unsetFirst ) );
        assertNull( within.get( "isError" ), () -> text( within ) );
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 21 );
        assertTrue( assertThrows( ToolError.class, () -> run( Map.of( "query", unsetFirst ) ) ).userMessage()
                        .startsWith( "Query complexity 22 exceeds the limit of 21" ) );
    }

    @Test
    public void testSmallFirstStaysWithinComplexity()
    {
        withoutChildren();
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 30 );
        final String query = "query ($n: Int) { guillotine { getChildren(key: \"/\", first: $n) { _id _path } } }";

        final Map<String, Object> within = run( Map.of( "query", query, "variables", Map.of( "n", 14 ) ) );
        assertNull( within.get( "isError" ), () -> text( within ) );
        assertTrue( assertThrows( ToolError.class, () -> run( Map.of( "query", query, "variables", Map.of( "n", 15 ) ) ) ).userMessage()
                        .startsWith( "Query complexity 32 exceeds the limit of 30" ) );
    }

    @Test
    public void testFragmentsCountForDepthAndComplexity()
    {
        when( guillotineConfigService.getMcpMaxDepth() ).thenReturn( 4 );
        final Map<String, Object> deep = run( Map.of( "query", "{ guillotine { get(key: \"a1\") { ...P } } } " +
            "fragment P on Content { parent { ... on Content { parent { parent { _id } } } } }" ) );
        assertEquals( "Query failed: Query depth 6 exceeds the limit of 4 nested levels (mcp.maxDepth). " +
                          "Select fewer nested fields, or split the query into several calls.", text( deep ) );

        when( guillotineConfigService.getMcpMaxDepth() ).thenReturn( 10 );
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 8 );
        for ( final String query : List.of( "{ guillotine { a: get(key: \"a1\") { _id _path displayName } " +
                                                "b: get(key: \"a1\") { _id _path displayName } } }",
                                            "{ guillotine { a: get(key: \"a1\") { ...F } b: get(key: \"a1\") { ...F } } } " +
                                                "fragment F on Content { _id _path displayName }",
                                            "{ guillotine { a: get(key: \"a1\") { ...F } b: get(key: \"a1\") { ... on Content " +
                                                "{ ...F } } } } fragment F on Content { _id _path displayName }" ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> run( Map.of( "query", query ) ) );
            assertTrue( error.userMessage().startsWith( "Query complexity 9 exceeds the limit of 8" ), error.userMessage() );
        }
        verifyNoInteractions( contentService );
    }

    @Test
    public void testVariableOfWrongTypeIsCleanError()
    {
        final Map<String, Object> result = run( Map.of( "query", "query ($n: Int) { guillotine { getChildren(key: \"/\", first: $n) { " +
            "_id } } }", "variables", Map.of( "n", Map.of( "a", 1 ) ) ) );

        assertEquals( true, result.get( "isError" ) );
        final String text = text( result );
        assertTrue( text.startsWith( "Query failed: " ), text );
        assertTrue( text.contains( "'n'" ), text );
        assertFalse( text.contains( "java." ) || text.contains( "graphql." ) || text.contains( "Exception" ), text );
        verifyNoInteractions( contentService );
    }

    @Test
    public void testRunsWithCallerPermissionsForAuthenticatedUser()
    {
        final List<AuthenticationInfo> seen = new ArrayList<>();
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenAnswer( invocation -> {
            seen.add( ContextAccessor.current().getAuthInfo() );
            assertEquals( "com.enonic.cms.myproject", ContextAccessor.current().getRepositoryId().toString() );
            return article( "a1", "First" );
        } );
        final AuthenticationInfo alice = AuthenticationInfo.create()
            .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).build() )
            .principals( RoleKeys.AUTHENTICATED, RoleKeys.EVERYONE )
            .build();

        final Map<String, Object> result = ContextBuilder.create()
            .authInfo( alice )
            .build()
            .callWith( () -> tool().execute( Map.of( "query", "{ guillotine { get(key: \"a1\") { _id } } }" ) ) );

        assertNull( result.get( "isError" ), () -> text( result ) );
        assertEquals( 1, seen.size() );
        assertEquals( "user:system:alice", seen.get( 0 ).getUser().getKey().toString() );
        assertFalse( seen.get( 0 ).hasRole( RoleKeys.ADMIN ) );
        assertFalse( seen.get( 0 ).hasRole( RoleKeys.CONTENT_MANAGER_ADMIN ) );
    }

    @Test
    public void testRunsWithCallerPermissionsForAnonymous()
    {
        final List<AuthenticationInfo> seen = new ArrayList<>();
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenAnswer( invocation -> {
            seen.add( ContextAccessor.current().getAuthInfo() );
            throw new ForbiddenAccessException( ContextAccessor.current().getAuthInfo().getUser() );
        } );

        final ToolError error = assertThrows( ToolError.class, () -> ContextBuilder.create()
            .authInfo( AuthenticationInfo.unAuthenticated() )
            .build()
            .callWith( () -> tool().execute( Map.of( "query", "{ guillotine { get(key: \"a1\") { _id } } }" ) ) ) );

        assertEquals( 1, seen.size() );
        assertFalse( seen.get( 0 ).isAuthenticated() );
        assertFalse( seen.get( 0 ).hasRole( RoleKeys.ADMIN ) );
        assertTrue( error.userMessage().startsWith( "Access denied: the current user cannot read branch 'draft'" ), error.userMessage() );
    }

    @Test
    public void testVariableDefaultsComeFromTheSelectedOperationOnly()
    {
        final String small = "query A($n: Int = 1) { guillotine { getChildren(key: \"/\", first: $n) { _id } } }";
        final String large = "query B($n: Int = 500) { guillotine { getChildren(key: \"/\", first: $n) { _id } } }";

        for ( final String document : List.of( small + " " + large, large + " " + small ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> checkLimits( document, "B", Map.of(), false ), document );
            assertTrue( error.userMessage().startsWith( "first must not exceed 100 in this tool." ), error.userMessage() );
            assertDoesNotThrow( () -> checkLimits( document, "A", Map.of(), false ), document );
        }
    }

    @Test
    public void testVariableDefaultIsAppliedWhenExecuting()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );
        when( contentService.getById( ContentId.from( "a2" ) ) ).thenReturn( article( "a2", "Second" ) );
        final String query = "query ($key: ID = \"a1\") { guillotine { get(key: $key) { _id displayName } } }";

        final Map<String, Object> byDefault = run( Map.of( "query", query ) );
        final Map<String, Object> overridden = run( Map.of( "query", query, "variables", Map.of( "key", "a2" ) ) );

        assertNull( byDefault.get( "isError" ), () -> text( byDefault ) );
        assertEquals( Map.of( "guillotine", Map.of( "get", Map.of( "_id", "a1", "displayName", "First" ) ) ),
                      structured( roundTrip( byDefault ) ).get( "data" ) );
        assertEquals( Map.of( "guillotine", Map.of( "get", Map.of( "_id", "a2", "displayName", "Second" ) ) ),
                      structured( roundTrip( overridden ) ).get( "data" ) );
    }

    @Test
    public void testVariableDefaultCountsForComplexityUnlessOverridden()
    {
        withoutChildren();
        when( guillotineConfigService.getMcpMaxComplexity() ).thenReturn( 30 );
        final String query = "query ($n: Int = 15) { guillotine { getChildren(key: \"/\", first: $n) { _id _path } } }";

        assertTrue( assertThrows( ToolError.class, () -> run( Map.of( "query", query ) ) ).userMessage()
                        .startsWith( "Query complexity 32 exceeds the limit of 30" ) );
        final Map<String, Object> overridden = run( Map.of( "query", query, "variables", Map.of( "n", 14 ) ) );
        assertNull( overridden.get( "isError" ), () -> text( overridden ) );
    }

    @Test
    public void testGraphQlExtensionsAreLeftOut()
    {
        final GraphQLSchema schema = schemaProvider.getSchema();
        final SchemaProvider provider = mock( SchemaProvider.class );
        when( provider.getSchema() ).thenReturn( schema );
        final Map<String, Object> guillotine = new LinkedHashMap<>();
        guillotine.put( "get", Map.of( "_id", "a1" ) );
        guillotine.put( "site", null );
        final Map<String, Object> error = Map.of( "message", "Site not found", "path", List.of( "guillotine", "site" ), "locations",
                                                  List.of( Map.of( "line", 1, "column", 30 ) ), "extensions",
                                                  Map.of( "classification", "DataFetchingException" ) );
        when( provider.executeToSpecification( any(), any(), any(), any() ) ).thenReturn(
            Map.of( "data", Map.of( "guillotine", guillotine ), "errors", List.of( error ), "extensions",
                    Map.of( "tracing", Map.of( "version", 1 ) ) ) );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );

        final Map<String, Object> result =
            run( tool, Map.of( "query", "{ guillotine { get(key: \"a1\") { _id } site: getSite { _id } } }" ) );

        assertNull( result.get( "isError" ), () -> text( result ) );
        final Map<String, Object> structured = structured( roundTrip( result ) );
        assertEquals( List.of( "project", "branch", "data", "errors" ), List.copyOf( structured.keySet() ) );
        assertEquals( List.of( Map.of( "message", "Site not found", "path", List.of( "guillotine", "site" ) ) ),
                      structured.get( "errors" ) );
        assertFalse( McpJson.write( result ).contains( "tracing" ), () -> McpJson.write( result ) );
        assertFalse( McpJson.write( result ).contains( "classification" ), () -> McpJson.write( result ) );
    }

    @Test
    public void testOtherOperationDefaultCannotLiftTheComplexityLimit()
    {
        final String document = "query A($n: Int = 1) { guillotine { queryDsl(query: {matchAll: {}}, first: $n) { _id } } } " +
            "query B($n: Int = 100) { guillotine { queryDsl(query: {matchAll: {}}, first: $n) { _id children(first: $n) { _id } } } }";
        final GraphQLSchema schema = schemaProvider.getSchema();
        final SchemaProvider provider = mock( SchemaProvider.class );
        when( provider.getSchema() ).thenReturn( schema );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );

        final ToolError error =
            assertThrows( ToolError.class, () -> run( tool, Map.of( "query", document, "operationName", "B" ) ) );

        assertTrue( error.userMessage().startsWith( "Query complexity 10202 exceeds the limit of 500" ), error.userMessage() );
        verifyNoInteractions( contentService, resolver );
    }

    @Test
    public void testPrincipalFieldsAreRejectedForNonCmsCallers()
    {
        final List<Map<String, Object>> forms = List.of(
            Map.of( "query", "{ guillotine { queryDsl(query: {term: {field: \"modifier\", value: \"user:system:alice\"}}) { _id } } }" ),
            Map.of( "query", "{ guillotine { queryDsl(query: {like: {field: \"owner\", value: \"*carol*\"}}) { _id } } }" ),
            Map.of( "query", "{ guillotine { queryDsl(query: {boolean: {must: [{exists: {field: \"Creator._orderby\"}}]}}) { _id } } }" ),
            Map.of( "query", "{ guillotine { queryDsl(query: {fulltext: {fields: [\"displayName\", \"owner^2\"], query: \"a\"}}) " +
                "{ _id } } }" ),
            Map.of( "query", "{ guillotine { queryDslConnection(query: {matchAll: {}}, first: 0, aggregations: [{name: \"m\", " +
                "terms: {field: \"modifier\"}}]) { totalCount } } }" ),
            Map.of( "query", "{ guillotine { queryDslConnection(query: {matchAll: {}}, first: 0, aggregations: [{name: \"t\", " +
                "terms: {field: \"type\"}, subAggregations: [{name: \"o\", terms: {field: \"owner._orderby\"}}]}]) { totalCount } } }" ),
            Map.of( "query", "{ guillotine { queryDslConnection(query: {matchAll: {}}, highlight: {properties: [{propertyName: " +
                "\"modifier\"}]}) { totalCount } } }" ),
            Map.of( "query", "{ guillotine { queryDsl(query: {matchAll: {}}, sort: [{field: \"creator\", direction: DESC}]) { _id } } }" ),
            Map.of( "query", "{ guillotine { getChildren(key: \"/\", sort: \"_name ASC, modifier DESC\") { _id } } }" ),
            Map.of( "query", "{ guillotine { queryDsl(query: {term: {field: \"_permissions_read\", value: \"role:everyone\"}}) " +
                "{ _id } } }" ),
            Map.of( "query", "query ($q: QueryDSLInput) { guillotine { queryDsl(query: $q) { _id } } }", "variables",
                    Map.of( "q", Map.of( "term", Map.of( "field", "modifier._orderby", "value", "x" ) ) ) ),
            Map.of( "query", "query ($f: String!) { guillotine { queryDsl(query: {term: {field: $f, value: \"x\"}}) { _id } } }",
                    "variables", Map.of( "f", "owner" ) ),
            Map.of( "query", "query ($s: String) { guillotine { getChildren(key: \"/\", sort: $s) { _id } } }", "variables",
                    Map.of( "s", "modifier DESC" ) ),
            Map.of( "query", "query ($a: [AggregationInput]) { guillotine { queryDslConnection(query: {matchAll: {}}, " +
                "aggregations: $a) { totalCount } } }", "variables",
                    Map.of( "a", List.of( Map.of( "name", "m", "terms", Map.of( "field", "modifier" ) ) ) ) ),
            Map.of( "query", "query ($q: QueryDSLInput = {term: {field: \"owner\", value: \"x\"}}) { guillotine { queryDsl(query: $q) " +
                "{ _id } } }" ) );

        for ( final AuthenticationInfo caller : List.of( AuthenticationInfo.unAuthenticated(), ALICE ) )
        {
            for ( final Map<String, Object> args : forms )
            {
                final ToolError error = assertThrows( ToolError.class, () -> ContextBuilder.create()
                    .authInfo( caller )
                    .build()
                    .callWith( () -> tool().execute( args ) ), () -> args.toString() );
                assertTrue( error.userMessage().contains( "holds user keys, which the current caller may not see." ), error.userMessage() );
            }
        }
        verifyNoInteractions( contentService, resolver );

        for ( final Map<String, Object> args : forms )
        {
            final Map<String, Object> variables = CastHelper.cast( args.getOrDefault( "variables", Map.of() ) );
            assertDoesNotThrow( () -> checkLimits( (String) args.get( "query" ), null, variables, true ), () -> args.toString() );
        }
    }

    @Test
    public void testPrincipalSortFieldsAreFoundWhateverTheSeparator()
    {
        final String query = "query ($s: String) { guillotine { getChildren(key: \"/\", sort: $s) { _id } } }";
        for ( final String sort : List.of( "owner\u2028DESC", "owner\u3000DESC", "owner\u2009DESC", "owner\u001fDESC", "owner\tDESC",
                                           "_name ASC,modifier DESC", "_name ASC ,\u00a0creator", "geoDistance('owner','1,2')",
                                           "geoDistance(\"data.location\", \"1,2\"), modifier._orderby ASC" ) )
        {
            final ToolError error =
                assertThrows( ToolError.class, () -> checkLimits( query, null, Map.of( "s", sort ), false ), () -> sort );
            assertTrue( error.userMessage().startsWith( "Invalid sort: " ) &&
                            error.userMessage().contains( "holds user keys, which the current caller may not see." ), error.userMessage() );
        }
        for ( final String sort : List.of( "_name ASC, modifiedTime\u3000DESC", "geoDistance('data.location','1,2') ASC",
                                           "displayName\tdesc" ) )
        {
            assertDoesNotThrow( () -> checkLimits( query, null, Map.of( "s", sort ), false ), sort );
        }
    }

    @Test
    public void testPrincipalCheckKeepsOtherFields()
    {
        assertDoesNotThrow( () -> checkLimits( "{ guillotine { queryDslConnection(query: {term: {field: \"data.owner\", value: " +
                                                   "\"x\"}}, aggregations: [{name: \"t\", terms: {field: \"ownership\"}}], " +
                                                   "sort: [{field: \"modifiedTime\"}]) { totalCount } } }", null, Map.of(), false ) );
    }

    @Test
    public void testAllFieldsFailingIsAnError()
    {
        final Map<String, Object> result =
            run( Map.of( "query", "{ guillotine { queryDsl(query: {matchAll: {}}, first: -5) { _id } } }" ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( text( result ).startsWith( "Query failed: guillotine.queryDsl: " ), text( result ) );
    }

    @Test
    public void testAccessDeniedWithoutDataGivesTheBranchHint()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenThrow( new ForbiddenAccessException( null ) );

        final ToolError error = assertThrows( ToolError.class, () -> ContextBuilder.create()
            .authInfo( AuthenticationInfo.unAuthenticated() )
            .build()
            .callWith( () -> tool().execute( Map.of( "query", "{ guillotine { queryDsl(query: {matchAll: {}}) { _id } } }" ) ) ) );

        assertEquals( "Access denied: the current user cannot read branch 'draft' of project 'myproject'. Anonymous callers can read " +
                          "only published content: omit branch or pass \"master\".", error.userMessage() );
    }

    @Test
    public void testDeclaredFirstDefaultCountsForComplexity()
    {
        final String query = "{ guillotine { items { _id } } }";
        final List<Consumer<GraphQLArgument.Builder>> defaults =
            List.of( first -> first.defaultValueLiteral( IntValue.of( 3 ) ), first -> first.defaultValueProgrammatic( 3 ) );

        for ( final Consumer<GraphQLArgument.Builder> declared : defaults )
        {
            final GraphQLSchema schema = schemaWithItems( declared );
            assertDoesNotThrow( () -> checkLimits( query, schema, 5 ) );
            assertTrue( assertThrows( ToolError.class, () -> checkLimits( query, schema, 4 ) ).userMessage()
                            .startsWith( "Query complexity 5 exceeds the limit of 4" ) );
            assertDoesNotThrow( () -> checkLimits( "{ guillotine { items(first: 2) { _id } } }", schema, 4 ) );
        }
        assertTrue( assertThrows( ToolError.class, () -> checkLimits( query, schemaWithItems( first -> {
        } ), 11 ) ).userMessage().startsWith( "Query complexity 12 exceeds the limit of 11" ) );
    }

    private void withoutChildren()
    {
        when( contentService.findIdsByParent( any( FindContentByParentParams.class ) ) ).thenReturn(
            FindContentIdsByParentResult.create().totalHits( 0 ).contentIds( ContentIds.empty() ).build() );
        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenReturn( Contents.empty() );
    }

    private static GraphQLSchema schemaWithItems( final Consumer<GraphQLArgument.Builder> firstDefault )
    {
        final GraphQLArgument.Builder first = GraphQLArgument.newArgument().name( "first" ).type( Scalars.GraphQLInt );
        firstDefault.accept( first );
        final GraphQLObjectType item = GraphQLObjectType.newObject()
            .name( "Item" )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "_id" ).type( Scalars.GraphQLString ) )
            .build();
        final GraphQLObjectType guillotine = GraphQLObjectType.newObject()
            .name( "Guillotine" )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "items" ).argument( first ).type( GraphQLList.list( item ) ) )
            .build();
        return GraphQLSchema.newSchema()
            .query( GraphQLObjectType.newObject()
                        .name( "Query" )
                        .field( GraphQLFieldDefinition.newFieldDefinition().name( "guillotine" ).type( guillotine ) ) )
            .build();
    }

    private static void checkLimits( final String query, final GraphQLSchema schema, final int maxComplexity )
    {
        final Document document = new Parser().parseDocument( query );
        QueryLimits.check( document, document.getDefinitionsOfType( OperationDefinition.class ).get( 0 ), Map.of(), maxComplexity, true,
                           () -> schema );
    }

    @Test
    public void testIntrospectionLimitIsAToolError()
    {
        final ToolError error = assertThrows( ToolError.class, () -> run( Map.of( "query", "{ a: __type(name: \"Query\") { name } " +
            "b: __type(name: \"Content\") { name } }" ) ) );

        assertEquals( "Introspection limit exceeded: This request is not asking for introspection in good faith - Query.__type is " +
                          "present too often. Ask for one __type per query, or call describe-type to see a content type.",
                      error.userMessage() );
        assertNull( run( Map.of( "query", "{ __type(name: \"Query\") { name } }" ) ).get( "isError" ) );
    }

    private void checkLimits( final String query, final String operationName, final Map<String, Object> variables,
                              final boolean principalsAllowed )
    {
        final Document document = new Parser().parseDocument( query );
        final OperationDefinition operation = document.getDefinitionsOfType( OperationDefinition.class )
            .stream()
            .filter( definition -> operationName == null || operationName.equals( definition.getName() ) )
            .findFirst()
            .orElseThrow();
        QueryLimits.check( document, operation, variables, 1_000_000, principalsAllowed, schemaProvider::getSchema );
    }

    private Map<String, Object> run( final QueryTool tool, final Map<String, Object> args )
    {
        return createAdminContext().callWith( () -> tool.execute( args ) );
    }
}
