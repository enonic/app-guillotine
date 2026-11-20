package com.enonic.app.guillotine.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.app.guillotine.graphql.ImageTransformations;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
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

    private QueryTool tool()
    {
        return new QueryTool( schemaProvider, guillotineConfigService, resolver );
    }

    private Map<String, Object> run( final Map<String, Object> args )
    {
        return createAdminContext().callWith( () -> tool().execute( args ) );
    }

    private static String nested( final int levels )
    {
        return "{ guillotine { get(key: \"a1\") { " + "parent { ".repeat( levels ) + "_id" + " }".repeat( levels ) + " } } }";
    }

    @Test
    public void testQueryRunsInTheResolvedProjectAndBranch()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenAnswer( invocation -> {
            assertEquals( "com.enonic.cms.myproject", ContextAccessor.current().getRepositoryId().toString() );
            assertEquals( "draft", ContextAccessor.current().getBranch().toString() );
            return article( "a1", "First" );
        } );

        final Map<String, Object> result = run( Map.of( "query", "{ guillotine { get(key: \"a1\") { _id displayName } } }" ) );

        assertEquals( Map.of( "project", "myproject", "branch", "draft", "data",
                              Map.of( "guillotine", Map.of( "get", Map.of( "_id", "a1", "displayName", "First" ) ) ) ),
                      structured( result ) );
    }

    @Test
    public void testNullFieldsStayInData()
    {
        final Map<String, Object> result = run( Map.of( "query", "{ guillotine { get(key: \"/nope\") { _path } } }" ) );

        final Map<String, Object> guillotine = new LinkedHashMap<>();
        guillotine.put( "get", null );
        assertEquals( Map.of( "guillotine", guillotine ), structured( result ).get( "data" ) );
        assertTrue( text( result ).endsWith( "\"data\":{\"guillotine\":{\"get\":null}}}" ), text( result ) );
    }

    @Test
    public void testPartialErrorsComeNextToData()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );

        final Map<String, Object> result = run( Map.of( "query", "{ ok: guillotine { get(key: \"a1\") { _id } } " +
            "bad: guillotine(scopeKey: \"missing\") { get(key: \"a1\") { _id } } }" ) );

        assertNull( result.get( "isError" ) );
        final Map<String, Object> structured = structured( result );
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "ok", Map.of( "get", Map.of( "_id", "a1" ) ) );
        data.put( "bad", null );
        assertEquals( data, structured.get( "data" ) );
        assertEquals( List.of( Map.of( "message", "Content for the \"scopeKey\" argument not found: \"missing\"", "path", List.of( "bad" ) ) ),
                      structured.get( "errors" ) );
    }

    @Test
    public void testNoDataIsAnErrorResult()
    {
        final ToolError error = assertThrows( ToolError.class, () -> run( Map.of( "query", "{ guillotine { nope } }" ) ) );

        assertTrue( error.userMessage().startsWith( "Query failed: " ), error.userMessage() );
        assertTrue( error.userMessage().contains( "nope" ), error.userMessage() );
        assertFalse( error.userMessage().contains( "graphql." ), error.userMessage() );
    }

    @Test
    public void testDeniedDraftReadSaysWhichBranchToUse()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenThrow( new ForbiddenAccessException( ALICE.getUser() ) );
        when( contentService.getById( ContentId.from( "a2" ) ) ).thenReturn( article( "a2", "Second" ) );
        final String expected = "Access denied: the caller cannot read branch draft of project myproject. Pass branch master to read " +
            "published content, or ask an administrator for a role in project myproject.";

        final ToolError error = assertThrows( ToolError.class, () -> asAlice(
            () -> tool().execute( Map.of( "query", "{ guillotine { get(key: \"a1\") { _id } } }" ) ) ) );
        assertEquals( expected, error.userMessage() );

        final Map<String, Object> partial = asAlice( () -> tool().execute(
            Map.of( "query", "{ guillotine { a: get(key: \"a1\") { _id } b: get(key: \"a2\") { _id } } }" ) ) );
        assertEquals( List.of( Map.of( "message", expected, "path", List.of( "guillotine", "a" ) ) ), structured( partial ).get( "errors" ) );
        assertFalse( text( partial ).contains( "alice" ), text( partial ) );
    }

    @Test
    public void testDeniedReadForAnAnonymousCallerPointsToMaster()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenThrow( new ForbiddenAccessException( User.anonymous() ) );

        final ToolError error = assertThrows( ToolError.class, () -> ContextBuilder.copyOf( ContextAccessor.current() )
            .authInfo( AuthenticationInfo.unAuthenticated() )
            .build()
            .callWith( () -> tool().execute( Map.of( "query", "{ guillotine { get(key: \"a1\") { _id } } }" ) ) ) );

        assertEquals( "Access denied: the caller cannot read branch draft of project myproject. Anonymous callers read only published " +
                          "content: omit branch or pass branch master.", error.userMessage() );
    }

    @Test
    public void testMutationAndSubscriptionAreRefusedBeforeExecution()
    {
        final SchemaProvider provider = mock( SchemaProvider.class );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );

        assertEquals( "Only query operations are allowed, but the document contains a mutation 'M'. This tool is read-only: remove " +
                          "the mutation and send query operations only.", assertThrows( ToolError.class, () -> tool.execute(
            Map.of( "query", "query Q { guillotine { get(key: \"a\") { _id } } } mutation M { delete(key: \"a\") }", "operationName",
                    "Q" ) ) ).userMessage() );
        assertTrue( assertThrows( ToolError.class, () -> tool.execute( Map.of( "query", "subscription { changes { _id } }" ) ) )
                        .userMessage()
                        .startsWith( "Only query operations are allowed, but the document contains a subscription." ) );
        verifyNoInteractions( provider, resolver );
    }

    @Test
    public void testOperationNameSelectsTheOperation()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );
        when( contentService.getById( ContentId.from( "a2" ) ) ).thenReturn( article( "a2", "Second" ) );
        final String document = "query First { guillotine { get(key: \"a1\") { displayName } } } " +
            "query Second($key: ID) { guillotine { get(key: $key) { displayName } } }";

        final Map<String, Object> result =
            run( Map.of( "query", document, "operationName", "Second", "variables", Map.of( "key", "a2" ) ) );

        assertEquals( Map.of( "guillotine", Map.of( "get", Map.of( "displayName", "Second" ) ) ), structured( result ).get( "data" ) );
        assertEquals( "Operation 'Third' is not in the document. Operations in the document: First, Second.",
                      assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", document, "operationName", "Third" ) ) )
                          .userMessage() );
        assertEquals( "The document contains 2 operations; operationName is required. Pass operationName, one of: First, Second.",
                      assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", document ) ) ).userMessage() );
    }

    @Test
    public void testVariableDefaultsComeFromTheSelectedOperation()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );
        final String document = "query A($n: Int = 500) { guillotine { getChildren(key: \"/\", first: $n) { _id } } } " +
            "query B($key: ID = \"a1\") { guillotine { get(key: $key) { displayName } } }";

        assertEquals( Map.of( "guillotine", Map.of( "get", Map.of( "displayName", "First" ) ) ),
                      structured( run( Map.of( "query", document, "operationName", "B" ) ) ).get( "data" ) );
        assertTrue( assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", document, "operationName", "A" ) ) )
                        .userMessage()
                        .startsWith( "first must not exceed 100" ) );
    }

    @Test
    public void testInvalidDocumentsAndArguments()
    {
        final ToolError syntax = assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", "{ guillotine { get( } }" ) ) );
        assertTrue( syntax.userMessage().startsWith( "Invalid GraphQL document: " ), syntax.userMessage() );

        assertTrue( assertThrows( ToolError.class, () -> tool().execute( Map.of() ) ).userMessage()
                        .startsWith( "query is required and must be a GraphQL document string." ) );
        assertEquals( "Invalid variables: expected a JSON object. Pass variables as a JSON object keyed by variable name, without the $.",
                      assertThrows( ToolError.class, () -> tool().execute(
                          Map.of( "query", "{ guillotine { getSite { _id } } }", "variables", List.of( 1 ) ) ) ).userMessage() );
        assertEquals( "Invalid operationName: 5. Pass the name of one query operation from the document.",
                      assertThrows( ToolError.class, () -> tool().execute(
                          Map.of( "query", "{ guillotine { getSite { _id } } }", "operationName", 5 ) ) ).userMessage() );

        final String longName = "Op" + "x".repeat( 10000 );
        assertTrue( assertThrows( ToolError.class, () -> tool().execute(
            Map.of( "query", "query A { guillotine { getSite { _id } } }", "operationName", longName ) ) ).userMessage().length() < 400 );
        assertTrue( assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", "{ " + longName + "(" ) ) ).userMessage()
                        .length() < 400 );
    }

    @Test
    public void testDocumentsOverTheTokenLimitAreRefused()
    {
        when( guillotineConfigService.getMaxQueryTokens() ).thenReturn( 50 );
        final String aliases =
            IntStream.range( 0, 20 ).mapToObj( i -> "a" + i + ": get(key: \"a1\") { _id }" ).collect( Collectors.joining( " " ) );

        final ToolError error = assertThrows( ToolError.class, () -> tool().execute( Map.of( "query", "{ guillotine { " + aliases + " } }" ) ) );

        assertTrue( error.userMessage().startsWith( "Invalid GraphQL document: " ), error.userMessage() );
        assertTrue( error.userMessage().contains( "50" ), error.userMessage() );
    }

    @Test
    public void testDepthIsLimitedToTenLevels()
    {
        assertNull( run( Map.of( "query", nested( 7 ) ) ).get( "isError" ) );

        final ToolError error = assertThrows( ToolError.class, () -> run( Map.of( "query", nested( 8 ) ) ) );
        assertEquals( "Query failed: Query depth 11 exceeds the limit of 10 nested levels. Select fewer nested fields, or split the " +
                          "query into several calls.", error.userMessage() );
    }

    @Test
    public void testRowLimitIsCheckedBeforeExecution()
    {
        final graphql.schema.GraphQLSchema schema = schemaProvider.getSchema();
        final SchemaProvider provider = mock( SchemaProvider.class );
        when( provider.getSchema() ).thenReturn( schema );
        final QueryTool tool = new QueryTool( provider, guillotineConfigService, resolver );

        final ToolError error = assertThrows( ToolError.class, () -> tool.execute( Map.of( "query",
                                                                                          "{ guillotine { queryDsl(query: {matchAll: {}}, " +
                                                                                              "first: 100) { children(first: 100) { _id } } } }" ) ) );

        assertTrue( error.userMessage().startsWith( "Query reads up to 10100 content items; the limit is 2000." ),
                    error.userMessage() );
        verifyNoInteractions( resolver );
    }

    @Test
    public void testIntrospectionIsLimited()
    {
        final ToolError error = assertThrows( ToolError.class, () -> run(
            Map.of( "query", "{ a: __type(name: \"Query\") { name } b: __type(name: \"Content\") { name } }" ) ) );

        assertTrue( error.userMessage().startsWith( "Introspection limit exceeded: " ), error.userMessage() );
        assertTrue( error.userMessage().contains( "too often. Call" ), error.userMessage() );
        assertTrue( error.userMessage().endsWith( "Call describe-type for the fields of one content type instead of introspecting the schema." ),
                    error.userMessage() );
        assertEquals( Map.of( "__type", Map.of( "name", "Query" ) ),
                      structured( run( Map.of( "query", "{ __type(name: \"Query\") { name } }" ) ) ).get( "data" ) );
    }

    @Test
    public void testUnorderedTermsGetANote()
    {
        when( contentService.find( any( com.enonic.xp.content.ContentQuery.class ) ) ).thenReturn(
            com.enonic.xp.content.FindContentIdsByQueryResult.create()
                .contents( com.enonic.xp.content.ContentIds.empty() )
                .totalHits( 0 )
                .build() );

        final Map<String, Object> structured = structured( run( Map.of( "query", "{ guillotine { queryDslConnection(query: " +
            "{matchAll: {}}, first: 0, aggregations: [{name: \"t\", terms: {field: \"type\"}}]) { totalCount } } }" ) ) );

        assertEquals( QueryLimits.UNORDERED_TERMS_NOTE, structured.get( "note" ) );
        assertEquals( Map.of( "guillotine", Map.of( "queryDslConnection", Map.of( "totalCount", 0 ) ) ), structured.get( "data" ) );
    }

    @Test
    public void testLargeDataIsCutWithinTheBudget()
    {
        when( guillotineConfigService.getMcpMaxResultChars() ).thenReturn( 1000 );
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "\"Long\" \\ title ".repeat( 20 ) ) );
        final String aliases =
            IntStream.range( 0, 30 ).mapToObj( i -> "a" + i + ": get(key: \"a1\") { displayName }" ).collect( Collectors.joining( " " ) );

        final Map<String, Object> result = run( Map.of( "query", "{ guillotine { " + aliases + " } }" ) );

        final Map<String, Object> structured = structured( result );
        assertTrue( McpJson.length( structured ) <= 1000, () -> "structured result is " + McpJson.length( structured ) + " chars" );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( "myproject", structured.get( "project" ) );
        assertTrue( ( (String) structured.get( "hint" ) ).endsWith( "Select fewer fields or pass a smaller first." ),
                    () -> (String) structured.get( "hint" ) );
    }

    @Test
    public void testNullListItemsAreLeftOut()
    {
        when( contentService.getById( ContentId.from( "a1" ) ) ).thenReturn( article( "a1", "First" ) );
        final ProjectBranch target = new ProjectBranch( "myproject", "draft" );
        final graphql.schema.GraphQLSchema schema = schemaProvider.getSchema();
        final SchemaProvider provider = mock( SchemaProvider.class );
        when( provider.getSchema() ).thenReturn( schema );
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "items", java.util.Arrays.asList( Map.of( "_id", "a" ), null ) );
        when( provider.executeToSpecification( any(), any(), any(), any() ) ).thenReturn( Map.of( "data", data ) );

        final Map<String, Object> structured =
            structured( new QueryTool( provider, guillotineConfigService, resolver ).execute( Map.of( "query", "{ __typename }" ) ) );

        assertEquals( target.putInto( new LinkedHashMap<>( Map.of( "data", Map.of( "items", List.of( Map.of( "_id", "a" ) ) ) ) ) ),
                      structured );
    }

    @Test
    public void testHttpPathKeepsItsBehaviour()
    {
        final String deep = nested( 9 );

        final Map<String, Object> specification =
            createAdminContext().callWith( () -> schemaProvider.executeToSpecification( deep, Map.of() ) );
        assertNull( specification.get( "errors" ), () -> String.valueOf( specification.get( "errors" ) ) );

        final Map<String, Object> viaApi = executeQuery( schemaProvider.getSchema(), deep );
        assertFalse( viaApi.containsKey( "errors" ), () -> String.valueOf( viaApi.get( "errors" ) ) );

        final Map<String, Object> withoutInstrumentation =
            createAdminContext().callWith( () -> schemaProvider.executeToSpecification( deep, null, null, null ) );
        assertNull( withoutInstrumentation.get( "errors" ) );

        final Map<String, Object> limited =
            createAdminContext().callWith( () -> schemaProvider.executeToSpecification( deep, null, null, QueryLimits.depthLimit() ) );
        assertTrue( String.valueOf( limited.get( "errors" ) ).contains( "exceeds the limit of 10" ) );
    }

    @Test
    public void testImageTransformationsFollowTheVirtualHostSetting()
    {
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( ContentFixtures.createMediaContent() );
        final Map<String, Object> args = Map.of( "query", "{ guillotine { get(key: \"contentid\") { _id ... on media_Image { " +
            "imageUrl(scale: \"block(10,10)\") { scale } } } } }" );
        final String denied = "Image transformations are not allowed here: remove the 'scale' argument";

        final Map<String, Object> withDeny = createAdminContext().callWith( () -> ContextBuilder.copyOf( ContextAccessor.current() )
            .attribute( ImageTransformations.CONTEXT_ATTRIBUTE, "deny" )
            .build()
            .callWith( () -> tool().execute( args ) ) );
        assertTrue( McpJson.write( withDeny ).contains( denied ), () -> McpJson.write( withDeny ) );

        final PortalRequest previous = PortalRequestAccessor.get();
        try
        {
            final PortalRequest mcpRequest = new PortalRequest();
            mcpRequest.setBaseUri( "/api/system:mcp" );
            PortalRequestAccessor.set( mcpRequest );
            final Map<String, Object> viaApi = run( args );
            assertFalse( McpJson.write( viaApi ).contains( denied ), () -> McpJson.write( viaApi ) );

            PortalRequestAccessor.remove();
            final Map<String, Object> withoutRequest = run( args );
            assertFalse( McpJson.write( withoutRequest ).contains( denied ), () -> McpJson.write( withoutRequest ) );
        }
        finally
        {
            PortalRequestAccessor.set( previous );
        }
    }

    private static <T> T asAlice( final java.util.concurrent.Callable<T> callable )
    {
        return ContextBuilder.copyOf( ContextAccessor.current() ).authInfo( ALICE ).build().callWith( callable );
    }
}
