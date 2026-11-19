package com.enonic.app.guillotine.mcp.tools;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.graphql.BaseGraphQLIntegrationTest;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.Cursors;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.aggregation.Buckets;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.WorkflowInfo;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.project.Projects;
import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.expr.DslExpr;
import com.enonic.xp.query.expr.DslOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.content.GetContentTypeParams;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static com.enonic.app.guillotine.mcp.McpResults.args;
import static com.enonic.app.guillotine.mcp.McpResults.successStructured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentToolsIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    private static final PrincipalKey ALICE_KEY = PrincipalKey.ofUser( IdProviderKey.system(), "alice" );

    private static final AuthenticationInfo ALICE =
        AuthenticationInfo.create().user( User.create().key( ALICE_KEY ).login( "alice" ).build() ).principals( RoleKeys.AUTHENTICATED )
            .build();

    private static final AuthenticationInfo ADMIN = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "admin" ) ).login( "admin" ).build() )
        .principals( RoleKeys.AUTHENTICATED, RoleKeys.ADMIN )
        .build();

    private static final ContentTypeName LANDING = ContentTypeName.from( "myapplication:landing" );

    private final Map<String, Content> store = new LinkedHashMap<>();

    private final List<String> searchable = new ArrayList<>();

    private final List<ContentQuery> searches = new ArrayList<>();

    private final AtomicReference<Context> searchContext = new AtomicReference<>();

    private final AtomicReference<Context> labelContext = new AtomicReference<>();

    private final AtomicReference<Context> workflowContext = new AtomicReference<>();

    private Aggregations aggregations = Aggregations.empty();

    private SearchTool search;

    private GetTool get;

    private ListTypesTool listTypes;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        doReturn( "myproject" ).when( guillotineConfigService ).getMcpDefaultProject();

        final NodeService nodeService = mock( NodeService.class );
        when( nodeService.getByPath( ContentConstants.CONTENT_ROOT_PATH ) ).thenReturn(
            Node.create().name( "content" ).parentPath( NodePath.ROOT ).data( new PropertyTree() ).build() );
        final ProjectService projectService = mock( ProjectService.class );
        when( projectService.list() ).thenReturn( Projects.empty() );
        final ProjectBranchResolver resolver = new ProjectBranchResolver( projectService, nodeService, guillotineConfigService );

        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> find( invocation.getArgument( 0 ) ) );
        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenAnswer( invocation -> {
            final GetContentByIdsParams params = invocation.getArgument( 0 );
            workflowContext.set( ContextAccessor.current() );
            return Contents.from(
                params.getIds().stream().map( id -> store.get( id.toString() ) ).filter( content -> content != null ).toList() );
        } );
        when( contentService.getByPath( any( ContentPath.class ) ) ).thenAnswer( invocation -> store.values()
            .stream()
            .filter( content -> content.getPath().equals( invocation.getArgument( 0 ) ) )
            .findFirst()
            .orElse( null ) );

        final ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( contentTypeService.getByName( any( GetContentTypeParams.class ) ) ).thenAnswer( invocation -> {
            final ContentTypeName name = ( (GetContentTypeParams) invocation.getArgument( 0 ) ).getContentTypeName();
            return getCustomContentTypes().stream().filter( type -> type.getName().equals( name ) ).findFirst().orElse(
                ContentType.create().superType( ContentTypeName.structured() ).name( name ).title( "Folder" ).build() );
        } );

        when( contentTypeService.getAll() ).thenReturn(
            ContentTypes.from( List.of( getCustomContentTypes().get( 0 ), ContentType.create()
                .superType( ContentTypeName.structured() )
                .name( ContentTypeName.folder() )
                .title( "Folder" )
                .build() ) ) );

        final ContentGraphQl graphQl = new ContentGraphQl( schemaProvider, contentService );
        search = new SearchTool( graphQl, resolver, guillotineConfigService, contentService, contentTypeService );
        get = new GetTool( graphQl, resolver, guillotineConfigService, contentTypeService, null );
        listTypes = new ListTypesTool( graphQl, resolver, guillotineConfigService, contentTypeService );
    }

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        return List.of( ContentType.create()
                            .superType( ContentTypeName.structured() )
                            .name( LANDING )
                            .title( "Landing" )
                            .addFormItem( Input.create().name( "title" ).label( "Title" ).inputType( InputTypeName.TEXT_LINE ).build() )
                            .addFormItem( Input.create().name( "body" ).label( "Body" ).inputType( InputTypeName.HTML_AREA ).build() )
                            .build() );
    }

    @Test
    public void testSearchArgumentsBecomeDslVariables()
    {
        addFolders( 3 );

        final Map<String, Object> result = as( ALICE, () -> search.execute(
            args( "text", "spring news", "types", List.of( "article", "base:folder" ), "path", "/articles/", "filters",
                  List.of( args( "field", "data.rating", "from", 1, "to", 5 ), args( "field", "modifiedTime", "from", "2026-01-01" ),
                           args( "field", "language", "equals", "en" ) ), "sort", "displayName desc, data.title", "aggregate",
                  List.of( args( "by", "data.author", "size", 5 ), args( "by", "modifiedTime", "interval", "1M" ) ), "count", 2 ) ) );

        final Map<String, Object> structured = successStructured( result );
        assertEquals( "myproject", structured.get( "project" ) );
        assertEquals( "draft", structured.get( "branch" ) );
        assertEquals( 3L, ( (Number) structured.get( "total" ) ).longValue() );

        final ContentQuery query = searches.get( 0 );
        assertEquals( 0, query.getFrom() );
        assertEquals( 2, query.getSize() );
        final List<Map<String, Object>> must = List.of(
            Map.of( "fulltext",
                    Map.of( "fields", List.of( "displayName", "_name", "_allText" ), "query", "spring news", "operator", "AND" ) ),
            Map.of( "boolean", Map.of( "should", List.of( Map.of( "in", Map.of( "field", "type", "values", List.of( "base:folder" ) ) ),
                                                          Map.of( "like", Map.of( "field", "type", "value", "*:article" ) ) ) ) ),
            Map.of( "pathMatch", Map.of( "field", "_path", "path", "/content/articles", "minimumMatch", 2 ) ),
            Map.of( "range", Map.of( "field", "data.rating", "gte", 1, "lt", 5 ) ),
            Map.of( "range", Map.of( "field", "modifiedTime", "gte", "2026-01-01T00:00Z", "type", "dateTime" ) ),
            Map.of( "term", Map.of( "field", "language", "value", "en" ) ) );
        final List<Map<String, Object>> mustNot = List.of( Map.of( "term", Map.of( "field", "_path", "value", "/content/articles" ) ) );
        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", must, "mustNot", mustNot ) ) ), dsl( query ) );
        assertEquals( List.of( "displayName DESC", "data.title ASC", "_path ASC" ), sort( query ) );
        assertEquals( Set.of( "a0", "a1" ), aggregationNames( query ) );
    }

    @Test
    public void testMineOnDefaultBranchIsModifierTermOnDraftInCallerContext()
    {
        addFolders( 1 );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> search.execute( args( "mine", true ) ) ) );

        assertEquals( "draft", structured.get( "branch" ) );
        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of( Map.of( "term", Map.of( "field", "modifier", "value",
                                                                                          "user:system:alice" ) ) ) ) )),
                      dsl( searches.get( 0 ) ) );
        assertEquals( List.of( "modifiedTime DESC", "_path ASC" ), sort( searches.get( 0 ) ) );
        assertEquals( "com.enonic.cms.myproject", searchContext.get().getRepositoryId().toString() );
        assertEquals( ContentConstants.BRANCH_DRAFT, searchContext.get().getBranch() );
        assertEquals( ALICE_KEY, searchContext.get().getAuthInfo().getUser().getKey() );
        assertFalse( searchContext.get().getAuthInfo().hasRole( RoleKeys.ADMIN ) );
    }

    @Test
    public void testWorkflowInProgressOnDraft()
    {
        addFolders( 1 );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> search.execute( args( "workflow", "IN_PROGRESS" ) ) ) );

        assertEquals( "draft", structured.get( "branch" ) );
        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of(
                          Map.of( "term", Map.of( "field", "workflow.state", "value", "IN_PROGRESS" ) ) ) ) )),
                      dsl( searches.get( 0 ) ) );
        assertEquals( "IN_PROGRESS", hits( structured ).get( 0 ).get( "workflow" ) );
    }

    @Test
    public void testWorkflowReadyExcludesTheOtherStates()
    {
        addFolders( 1 );

        as( ALICE, () -> search.execute( args( "workflow", "READY" ) ) );

        final Map<String, Object> notReady =
            Map.of( "in", Map.of( "field", "workflow.state", "values", List.of( "IN_PROGRESS", "PENDING_APPROVAL", "REJECTED" ) ) );
        final Map<String, Object> expected =
            Map.of( "boolean", Map.of( "must", List.of( Map.of( "matchAll", Map.of() ) ), "mustNot", List.of( notReady ) ) );
        assertEquals( unwrap( expected ), dsl( searches.get( 0 ) ) );
    }

    @Test
    public void testAnonymousMineIsRejected()
    {
        final ToolError error = assertThrows( ToolError.class, () -> as( AuthenticationInfo.unAuthenticated(),
                                                                         () -> search.execute( args( "mine", true ) ) ) );

        assertTrue( error.userMessage().contains( "authenticated" ), error.userMessage() );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testAnonymousDefaultsToMaster()
    {
        addFolders( 1 );

        assertEquals( "master", successStructured( as( AuthenticationInfo.unAuthenticated(), () -> search.execute( args() ) ) ).get( "branch" ) );
        assertEquals( ContentConstants.BRANCH_MASTER, searchContext.get().getBranch() );
    }

    @Test
    public void testUserValuesStayVariables()
    {
        addFolders( 1 );
        final String hostile = "\") { _id } } } query X { __schema { types { name } } } #{\"";

        final Map<String, Object> result = as( ALICE, () -> search.execute(
            args( "text", hostile, "filters", List.of( args( "field", "data.title", "equals", hostile ) ) ) ) );

        assertNull( successStructured( result ).get( "errors" ) );
        assertEquals( 1, searches.size() );
        final Map<String, Object> dsl = dsl( searches.get( 0 ) );
        final List<?> must = (List<?>) ( (Map<?, ?>) dsl.get( "boolean" ) ).get( "must" );
        assertEquals( hostile, ( (Map<?, ?>) ( (Map<?, ?>) must.get( 0 ) ).get( "fulltext" ) ).get( "query" ) );
        assertEquals( hostile, ( (Map<?, ?>) ( (Map<?, ?>) must.get( 1 ) ).get( "term" ) ).get( "value" ) );
        assertFalse( SearchTool.QUERY.contains( "__schema" ) );
    }

    @Test
    public void testPagingWalksToTheEnd()
    {
        addFolders( 23 );

        final Set<Object> ids = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do
        {
            final Map<String, Object> args = args( "count", 7 );
            if ( cursor != null )
            {
                args.put( "cursor", cursor );
            }
            final Map<String, Object> structured = successStructured( as( ALICE, () -> search.execute( args ) ) );
            hits( structured ).forEach( hit -> ids.add( hit.get( "_id" ) ) );
            cursor = (String) structured.get( "nextCursor" );
            pages++;
        }
        while ( cursor != null && pages < 10 );

        assertEquals( 4, pages );
        assertEquals( 23, ids.size() );
        assertEquals( List.of( 0, 7, 14, 21 ), searches.stream().map( ContentQuery::getFrom ).toList() );
    }

    @Test
    public void testCursorIsBoundToTheQuery()
    {
        addFolders( 5 );
        final String cursor =
            (String) successStructured( as( ALICE, () -> search.execute( args( "text", "a", "count", 2 ) ) ) ).get( "nextCursor" );

        final ToolError changedFilter =
            assertThrows( ToolError.class, () -> as( ALICE, () -> search.execute( args( "text", "b", "count", 2, "cursor", cursor ) ) ) );
        final ToolError changedBranch = assertThrows( ToolError.class, () -> as( ALICE, () -> search.execute(
            args( "text", "a", "count", 2, "branch", "master", "cursor", cursor ) ) ) );

        assertTrue( changedFilter.userMessage().contains( "cursor does not match this query" ), changedFilter.userMessage() );
        assertTrue( changedBranch.userMessage().contains( "cursor does not match this query" ) );
        assertEquals( 2, ( (List<?>) successStructured( as( ALICE, () -> search.execute( args( "text", "a", "count", 2, "cursor", cursor ) ) ) )
            .get( "hits" ) ).size() );
    }

    @Test
    public void testLargePageIsTruncatedWithinBudget()
    {
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        addFolders( 30 );

        final Map<String, Object> result = as( ALICE, () -> search.execute( args( "count", 30 ) ) );
        final Map<String, Object> structured = successStructured( result );

        assertTrue( McpJson.length( structured ) <= 1000, () -> "length " + McpJson.length( structured ) );
        assertEquals( true, structured.get( "truncated" ) );
        final int returned = hits( structured ).size();
        assertTrue( returned > 0 && returned < 30 );
        final String text = (String) ( (Map<?, ?>) ( (List<?>) result.get( "content" ) ).get( 0 ) ).get( "text" );
        assertTrue( text.startsWith( "30 items found in myproject (draft); showing 1-" + returned +
                                         " of 30, continue with nextCursor. Result truncated; see hint.\n" ), text );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.endsWith( ". Narrow with text, types, path or filters, or use a smaller count. Continue with nextCursor." ),
                    hint );
        assertFalse( hint.contains( "fields," ), hint );
        final String next = (String) structured.get( "nextCursor" );
        as( ALICE, () -> search.execute( args( "count", 30, "cursor", next ) ) );
        assertEquals( returned, searches.get( 1 ).getFrom() );
    }

    @Test
    public void testCountZeroWithTermsAggregationLabelsContentKeys()
    {
        addFolders( 2 );
        final Content ada = content( "9ff03ff8-dbd6-4496-9243-d6808ee7c524", "/persons", "ada-lovelace", "Ada Lovelace" );
        aggregations = Aggregations.create()
            .add( BucketAggregation.bucketAggregation( "a0" )
                      .buckets( Buckets.create()
                                    .add( Bucket.create().key( "unknown" ).docCount( 1 ).build() )
                                    .add( Bucket.create().key( "22222222-2222-4222-8222-222222222222" ).docCount( 1 ).build() )
                                    .add( Bucket.create().key( ada.getId().toString() ).docCount( 8 ).build() )
                                    .build() )
                      .build() )
            .build();

        final Map<String, Object> args =
            args( "types", List.of( "article" ), "aggregate", List.of( args( "by", "data.author" ) ), "count", 0 );
        final Map<String, Object> structured = successStructured( as( ALICE, () -> search.execute( args ) ) );

        assertNull( structured.get( "hits" ) );
        assertNull( structured.get( "nextCursor" ) );
        assertEquals( 0, searches.get( 0 ).getSize() );
        final List<?> buckets = (List<?>) ( (Map<?, ?>) ( (List<?>) structured.get( "aggregations" ) ).get( 0 ) ).get( "buckets" );
        assertEquals( Map.of( "key", ada.getId().toString(), "count", 8, "label",
                              Map.of( "path", "/persons/ada-lovelace", "displayName", "Ada Lovelace" ) ), normalize( buckets.get( 0 ) ) );
        assertEquals( Map.of( "key", "22222222-2222-4222-8222-222222222222", "count", 1, "labelMissing", true ),
                      normalize( buckets.get( 1 ) ) );
        assertEquals( Map.of( "key", "unknown", "count", 1 ), normalize( buckets.get( 2 ) ) );

        assertCallerContext( labelContext.get() );
    }

    @Test
    public void testLookupsRunInCallerContext()
    {
        addFolders( 2 );

        as( ALICE, () -> search.execute( args() ) );

        assertCallerContext( searchContext.get() );
        assertCallerContext( workflowContext.get() );
    }

    @Test
    public void testLabelsBeyondTheCapAreNoted()
    {
        addFolders( 1 );
        final Buckets.Builder buckets = Buckets.create();
        for ( int i = 0; i < ContentGraphQl.MAX_LABELS + 5; i++ )
        {
            buckets.add( Bucket.create().key( "%08d-0000-4000-8000-000000000000".formatted( i ) ).docCount( 1 ).build() );
        }
        aggregations = Aggregations.create().add( BucketAggregation.bucketAggregation( "a0" ).buckets( buckets.build() ).build() ).build();

        final Map<String, Object> structured = successStructured(
            as( ALICE, () -> search.execute( args( "aggregate", List.of( args( "by", "data.author", "size", 100 ) ), "count", 0 ) ) ) );

        final Map<?, ?> aggregation = (Map<?, ?>) ( (List<?>) structured.get( "aggregations" ) ).get( 0 );
        assertEquals( "Labels resolved for the first 100 content-id buckets only.", aggregation.get( "note" ) );
    }

    @Test
    public void testEmptyAggregationsExplainWhy()
    {
        addFolders( 1 );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> search.execute(
            args( "aggregate", List.of( args( "by", "_allText" ), args( "by", "data.title", "interval", "1d" ) ), "count", 0 ) ) ) );

        final List<?> result = (List<?>) structured.get( "aggregations" );
        assertTrue( ( (String) ( (Map<?, ?>) result.get( 0 ) ).get( "note" ) ).contains( "cannot be aggregated" ) );
        assertTrue( ( (String) ( (Map<?, ?>) result.get( 1 ) ).get( "note" ) ).contains( "interval needs a date field" ) );
    }

    @Test
    public void testPrincipalFieldsAreRejectedForNonCmsCallers()
    {
        for ( final AuthenticationInfo caller : List.of( AuthenticationInfo.unAuthenticated(), ALICE ) )
        {
            for ( final Map<String, Object> args : List.of( args( "aggregate", List.of( args( "by", "modifier" ) ) ),
                                                            args( "filters", List.of( args( "field", "Owner", "equals", "x" ) ) ),
                                                            args( "filters", List.of( filter( "_permissions_read" ) ) ),
                                                            args( "sort", "creator DESC" ) ) )
            {
                final ToolError error = assertThrows( ToolError.class, () -> as( caller, () -> search.execute( args ) ) );
                assertTrue( error.userMessage().contains( "holds user keys" ), error.userMessage() );
            }
        }
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testPrincipalSubfieldsAreRejectedForNonCmsCallers()
    {
        for ( final Map<String, Object> args : List.of( args( "aggregate", List.of( args( "by", "owner._orderby" ) ) ),
                                                        args( "aggregate", List.of( args( "by", "Creator._orderby" ) ) ),
                                                        args( "filters", List.of( filter( "modifier._orderby" ) ) ),
                                                        args( "filters", List.of( filter( "archivedBy.x" ) ) ) ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> as( AuthenticationInfo.unAuthenticated(),
                                                                             () -> search.execute( args ) ), args::toString );
            assertTrue( error.userMessage().contains( "holds user keys" ), error.userMessage() );
        }
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testPrincipalFieldNames()
    {
        for ( final String field : List.of( "modifier", "OWNER", "creator._orderby", "owner^2", " owner", "mod*", "*", "_perm*",
                                            "_permissions_read", "archivedby" ) )
        {
            assertTrue( SearchQuery.isPrincipalField( field ), field );
        }
        for ( final String field : List.of( "modifiedTime", "ownership", "data.owner", "x.creator", "m?", "data*", "displayName" ) )
        {
            assertFalse( SearchQuery.isPrincipalField( field ), field );
        }
    }

    @Test
    public void testSearchRejectsPathsThatAreNotContentPaths()
    {
        for ( final String path : List.of( "/x/..", "/x/./y", "/articles/a\\b", "/x|y", "/a?b", "/a//b", "articles" ) )
        {
            final String message = invalid( args( "path", path ) );
            assertTrue( message.startsWith( "Invalid path: " ) && message.contains( "starts with /" ), message );
        }
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testGetRejectsKeysThatAreNotContentIdsOrPaths()
    {
        for ( final String key : List.of( "/articles/a\\b", "/articles/x|y", "/articles/a?b", "x y", "<script>", "Not-An-Id" ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> as( ALICE, () -> get.execute( args( "key", key ) ) ), key );
            assertTrue( error.userMessage().startsWith( "Invalid key: " ), error.userMessage() );
            assertTrue( error.userMessage().endsWith( "Paths start with / relative to the project root, e.g. /articles/article-01; or " +
                                                          "pass the _id returned by search." ), error.userMessage() );
        }
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testListTypesSummaryCountsWhatIsReturned()
        throws Exception
    {
        final Buckets.Builder buckets = Buckets.create();
        for ( int i = 0; i < ListTypesTool.MAX_TYPES; i++ )
        {
            buckets.add( Bucket.create().key( "com.example.app:type-" + i ).docCount( ListTypesTool.MAX_TYPES - i ).build() );
        }
        aggregations = Aggregations.create().add( BucketAggregation.bucketAggregation( "types" ).buckets( buckets.build() ).build() ).build();
        searchable.add( "x" );
        doReturn( 3000 ).when( guillotineConfigService ).getMcpMaxResultChars();

        final Map<String, Object> result = as( ALICE, () -> listTypes.execute( args() ) );

        final Map<String, Object> structured = successStructured( result );
        final int listed = ( (List<?>) structured.get( "types" ) ).size();
        assertTrue( listed > 0 && listed < ListTypesTool.MAX_TYPES, () -> McpJson.write( result ) );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( "Only the 1000 most used content types are listed.", structured.get( "note" ) );
        assertTrue( text( result ).startsWith( "At least 1000 content types in use in myproject (draft); " + listed + " listed here.\n" ),
                    text( result ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-types", structured ) );
    }

    @Test
    public void testListTypesThatCannotFitKeepTheRequiredFields()
        throws Exception
    {
        final ContentGraphQl graphQl = mock( ContentGraphQl.class );
        final List<Object> errors = IntStream.range( 0, 10 )
            .mapToObj( i -> (Object) Map.of( "message", "e".repeat( 290 ), "path", List.of( "guillotine", "queryDslConnection" ) ) )
            .toList();
        final Map<String, Object> connection = Map.of( "totalCount", 4, "aggregationsAsJson", Map.of( "types", Map.of( "buckets", List.of(
            Map.of( "key", "base:folder", "docCount", 4 ) ) ) ) );
        when( graphQl.execute( any(), any(), any() ) ).thenReturn(
            Map.of( "data", Map.of( "guillotine", Map.of( "queryDslConnection", connection ) ), "errors", errors ) );
        final ProjectBranchResolver resolver = mock( ProjectBranchResolver.class );
        when( resolver.resolve( any() ) ).thenReturn( new ProjectBranch( "myproject", "draft" ) );
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        final ListTypesTool tool = new ListTypesTool( graphQl, resolver, guillotineConfigService, mock( ContentTypeService.class ) );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> tool.execute( args() ) ) );

        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-types", structured ) );
        assertTrue( McpJson.length( structured ) <= 1000, String.valueOf( McpJson.length( structured ) ) );
        assertEquals( "myproject", structured.get( "project" ) );
        assertEquals( "draft", structured.get( "branch" ) );
        assertEquals( List.of(), structured.get( "types" ) );
        assertEquals( true, structured.get( "truncated" ) );
    }

    @Test
    public void testPrincipalFieldsAreAllowedForCmsCallers()
    {
        addFolders( 1 );

        successStructured( as( ADMIN, () -> search.execute(
            args( "aggregate", List.of( args( "by", "modifier" ) ), "filters", List.of( args( "field", "owner", "equals", "x" ) ), "sort",
                  "creator DESC" ) ) ) );

        assertEquals( List.of( "creator DESC", "_path ASC" ), sort( searches.get( 0 ) ) );
    }

    @Test
    public void testMineStaysAllowedWithoutCmsRole()
    {
        addFolders( 1 );

        successStructured( as( ALICE, () -> search.execute( args( "mine", true ) ) ) );

        assertEquals( 1, searches.size() );
    }

    @Test
    public void testInvalidDatesNameTheArgument()
    {
        assertTrue( invalid( args( "filters", List.of( args( "field", "modifiedTime", "from", "2026-13-45" ) ) ) ).startsWith(
            "Invalid filters[0].from: \"2026-13-45\" is not a valid date." ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "x", "equals", 1 ), args( "field", "modifiedTime", "to",
                                                                                              "2026-02-30T10:00:00Z" ) ) ) ).startsWith(
            "Invalid filters[1].to:" ) );
    }

    @Test
    public void testIntegerAndDecimalRangeBoundsAreBothDoubles()
    {
        addFolders( 1 );

        successStructured( as( ALICE, () -> search.execute( args( "filters", List.of( args( "field", "data.rating", "from", 1, "to", 4.5 ) ) ) ) ) );

        assertEquals( Map.of( "boolean", Map.of( "must", Map.of( "range", Map.of( "field", "data.rating", "gte", 1.0, "lt", 4.5 ) ) ) ),
                      dsl( searches.get( 0 ) ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "data.rating", "from", 1, "to", "2026-01-01" ) ) ) ).contains(
            "from and to must be of the same kind" ) );
    }

    @Test
    public void testRichTextWithUnclosedTagsIsLinear()
    {
        final List<String> inputs = List.of( "<a href=x>t ".repeat( 40000 ), "<script>x ".repeat( 40000 ), "<!-- x".repeat( 40000 ),
                                             "<".repeat( 200000 ), "<a ".repeat( 100000 ), "<li ".repeat( 100000 ),
                                             "<img src=x ".repeat( 40000 ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 5 ), () -> inputs.forEach( RichText::convert ) );

        final Map<String, Object> converted =
            RichText.convert( "<p><a href=\"https://a.example\">one</a> <a href=x>open <a href=\"https://b.example\">two</a></p>" +
                                  "<script>s<p>x</p></script><!-- c --><p>tail</p>" );
        assertEquals( "one open two\ntail", converted.get( "text" ) );
        assertEquals( List.of( Map.of( "type", "external", "url", "https://a.example", "text", "one" ),
                               Map.of( "type", "external", "url", "https://b.example", "text", "two" ) ), converted.get( "links" ) );
    }

    @Test
    public void testManyLinksAreCapped()
    {
        final String html = "<p>" + "<a href=\"https://example.com\">x</a>".repeat( RichText.MAX_LINKS + 3 ) + "</p>";

        final Map<String, Object> converted = RichText.convert( html );

        assertEquals( RichText.MAX_LINKS, ( (List<?>) converted.get( "links" ) ).size() );
        assertEquals( true, converted.get( "linksTruncated" ) );
        assertNull( RichText.convert( "<a href=\"https://example.com\">x</a>" ).get( "linksTruncated" ) );
    }

    @Test
    public void testInvalidArgumentsSayWhatToFix()
    {
        assertTrue( invalid( args( "sort", "rating DESC" ) ).contains( "Invalid sort field: \"rating\"" ) );
        assertTrue( invalid( args( "sort", "modifiedTime UP" ) ).contains( "Use ASC or DESC" ) );
        assertEquals( "Invalid count: 51. Use an integer from 0 to 50 (default 10); 0 returns only total and aggregations.",
                      invalid( args( "count", 51 ) ) );
        assertTrue( invalid( args( "branch", "live" ) ).contains( "Invalid branch" ) );
        assertTrue( invalid( args( "types", List.of( "a b" ) ) ).contains( "list-types" ) );
        assertTrue( invalid( args( "aggregate", List.of( args( "by", "type", "size", 101 ) ) ) ).contains( "1 to 100" ) );
        assertTrue( invalid( args( "aggregate", List.of( args( "by", "modifiedTime", "interval", "daily" ) ) ) ).contains( "\"1M\"" ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "x", "equals", 1, "from", 2 ) ) ) ).contains( "either equals" ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "a b", "equals", 1 ) ) ) ).contains( "describe-type" ) );
        assertTrue( invalid( args( "path", "articles" ) ).contains( "starts with /" ) );
        assertTrue( invalid( args( "workflow", "DONE" ) ).contains( "IN_PROGRESS" ) );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testPrincipalsOnlyForCmsUsers()
    {
        addFolders( 1 );

        assertNull( hits( successStructured( as( ALICE, () -> search.execute( args() ) ) ) ).get( 0 ).get( "modifier" ) );
        final Map<String, Object> hit = hits( successStructured( as( ADMIN, () -> search.execute( args() ) ) ) ).get( 0 );
        assertEquals( "user:system:alice", hit.get( "modifier" ) );
        assertEquals( "user:system:owner", hit.get( "owner" ) );
    }

    @Test
    public void testListTypesCountsTypesInUse()
        throws Exception
    {
        aggregations = Aggregations.create()
            .add( BucketAggregation.bucketAggregation( "types" )
                      .buckets( Buckets.create()
                                    .add( Bucket.create().key( "base:folder" ).docCount( 4 ).build() )
                                    .add( Bucket.create().key( LANDING.toString() ).docCount( 2 ).build() )
                                    .build() )
                      .build() )
            .build();
        searchable.add( "x" );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> listTypes.execute( args() ) ) );

        assertEquals( List.of( Map.of( "name", "base:folder", "displayName", "Folder", "count", 4 ),
                               Map.of( "name", LANDING.toString(), "displayName", "Landing", "count", 2 ) ),
                      normalize( structured.get( "types" ) ) );
        assertEquals( 0, searches.get( 0 ).getSize() );
        assertTrue( aggregationNames( searches.get( 0 ) ).contains( "types" ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-types", structured ) );
    }

    @Test
    public void testGetConvertsRichTextAndResolvesLinks()
    {
        final Content target = content( "11111111-1111-4111-8111-111111111111", "/articles", "a-1", "Article 1" );
        final PropertyTree data = new PropertyTree();
        data.addString( "title", "Hello" );
        data.addString( "body", "<h1>Hi &amp; welcome</h1><p>See <a href=\"content://" + target.getId() + "\">this</a> and " +
            "<a href=\"content://00000000-0000-4000-8000-00000000dead\">gone</a> or <a href=\"https://example.com\">site</a>.</p>" +
            "<figure><img alt=\"pic\" src=\"image://" + target.getId() + "?scale=block(768,432)\"></figure>" );
        store.put( "landing", Content.create()
            .id( ContentId.from( "landing" ) )
            .name( "l-1" )
            .parentPath( ContentPath.from( "/landings" ) )
            .type( LANDING )
            .displayName( "Landing 1" )
            .modifier( ALICE_KEY )
            .modifiedTime( Instant.ofEpochSecond( 0 ) )
            .data( data )
            .build() );

        final Map<?, ?> content =
            (Map<?, ?>) successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1" ) ) ) ).get( "content" );

        assertEquals( "/landings/l-1", content.get( "_path" ) );
        final Map<?, ?> body = (Map<?, ?>) ( (Map<?, ?>) content.get( "data" ) ).get( "body" );
        assertEquals( "Hi & welcome\nSee this and gone or site.", body.get( "text" ) );
        assertEquals( List.of( Map.of( "type", "content", "id", target.getId().toString(), "text", "this", "path", "/articles/a-1",
                                       "displayName", "Article 1" ),
                               Map.of( "type", "content", "id", "00000000-0000-4000-8000-00000000dead", "text", "gone", "missing", true ),
                               Map.of( "type", "external", "url", "https://example.com", "text", "site" ),
                               Map.of( "type", "image", "id", target.getId().toString(), "text", "pic", "path", "/articles/a-1",
                                       "displayName", "Article 1" ) ), normalize( body.get( "links" ) ) );
        assertEquals( "Hello", ( (Map<?, ?>) content.get( "data" ) ).get( "title" ) );
    }

    @Test
    public void testGetProjectsFieldsAndReportsMissingOnes()
    {
        final PropertyTree data = new PropertyTree();
        data.addString( "title", "Hello" );
        data.addString( "body", "<p>x</p>" );
        store.put( "landing", Content.create()
            .id( ContentId.from( "landing" ) )
            .name( "l-1" )
            .parentPath( ContentPath.from( "/landings" ) )
            .type( LANDING )
            .displayName( "Landing 1" )
            .data( data )
            .build() );

        final Map<String, Object> structured =
            successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1", "fields", List.of( "data.title", "nope" ) ) ) ) );

        assertEquals( Map.of( "title", "Hello" ), ( (Map<?, ?>) structured.get( "content" ) ).get( "data" ) );
        assertEquals( List.of( "nope" ), structured.get( "missingFields" ) );

        final Map<String, Object> all = successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1", "fields", List.of() ) ) ) );
        assertEquals( Set.of( "title", "body" ), ( (Map<?, ?>) ( (Map<?, ?>) all.get( "content" ) ).get( "data" ) ).keySet() );
    }

    @Test
    public void testGetReplacesOneOversizedFieldAndKeepsTheRest()
    {
        final String large = "x".repeat( 7000 );
        final PropertyTree data = new PropertyTree();
        data.addString( "title", large );
        data.addString( "body", "<p>Short body</p>" );
        landing( data );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1" ) ) ) );

        final Map<?, ?> fields = (Map<?, ?>) ( (Map<?, ?>) structured.get( "content" ) ).get( "data" );
        assertEquals( Map.of( "truncated", true, "chars", 7002 ), fields.get( "title" ) );
        assertEquals( Map.of( "text", "Short body" ), fields.get( "body" ) );
        assertEquals( "/landings/l-1", ( (Map<?, ?>) structured.get( "content" ) ).get( "_path" ) );
        assertEquals( true, structured.get( "truncated" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.contains( "replaced 'content.data.title' (7002 chars, over the 6000-char limit for one value)" ), hint );
        assertTrue( hint.endsWith( "Pass fields with the names of the replaced data fields to read them in full." ), hint );

        final Map<String, Object> asked =
            successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1", "fields", List.of( "title" ) ) ) ) );
        assertEquals( Map.of( "title", large ), ( (Map<?, ?>) asked.get( "content" ) ).get( "data" ) );
        assertNull( asked.get( "truncated" ) );
    }

    @Test
    public void testGetOmittedFieldsAreExplainedInTheStructure()
    {
        doReturn( 10000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        final PropertyTree data = new PropertyTree();
        data.addString( "title", "y".repeat( 9000 ) );
        data.addString( "body", "<p>Short body</p>" );
        landing( data );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1" ) ) ) );

        assertEquals( List.of( "title" ), structured.get( "omittedFields" ) );
        assertEquals( "Large fields omitted: title; pass fields to read them.", structured.get( "note" ) );
    }

    @Test
    public void testGetFieldInsideRepeatedItemSetListsEveryOccurrence()
    {
        final PropertyTree data = new PropertyTree();
        data.addString( "title", "Hello" );
        for ( final String body : List.of( "first", "second" ) )
        {
            final PropertySet set = data.newSet();
            set.addString( "body", body );
            data.addSet( "set", set );
        }
        landing( data );

        final Map<String, Object> structured =
            successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1", "fields", List.of( "set.body", "set.nope" ) ) ) ) );

        assertEquals( Map.of( "set.body", List.of( "first", "second" ) ), ( (Map<?, ?>) structured.get( "content" ) ).get( "data" ) );
        assertEquals( List.of( "set.nope" ), structured.get( "missingFields" ) );
    }

    @Test
    public void testSearchCapsLargeBucketsNextToHitsButNotAlone()
    {
        addFolders( 3 );
        final Buckets.Builder buckets = Buckets.create();
        for ( int i = 0; i < 100; i++ )
        {
            final String key = "/a/fairly/long/path/used/as/a/bucket/key/to/fill/the/aggregation/" + i;
            buckets.add( Bucket.create().key( key ).docCount( 1 ).build() );
        }
        aggregations = Aggregations.create().add( BucketAggregation.bucketAggregation( "a0" ).buckets( buckets.build() ).build() ).build();
        final List<Map<String, Object>> aggregate = List.of( args( "by", "_path", "size", 100 ) );

        final Map<String, Object> withHits = successStructured( as( ALICE, () -> search.execute( args( "aggregate", aggregate, "count", 3 ) ) ) );

        assertEquals( 3, hits( withHits ).size() );
        final Map<?, ?> aggregation = (Map<?, ?>) ( (List<?>) withHits.get( "aggregations" ) ).get( 0 );
        assertEquals( "_path", aggregation.get( "by" ) );
        assertEquals( true, ( (Map<?, ?>) aggregation.get( "buckets" ) ).get( "truncated" ) );
        final String hint = (String) withHits.get( "hint" );
        assertTrue( hint.contains( "replaced 'aggregations[0].buckets'" ) && hint.contains( "over the 6000-char limit" ), hint );
        assertTrue( hint.endsWith( "pass count 0 to get aggregation buckets alone." ), hint );

        final Map<String, Object> alone = successStructured( as( ALICE, () -> search.execute( args( "aggregate", aggregate, "count", 0 ) ) ) );
        assertEquals( 100, ( (List<?>) ( (Map<?, ?>) ( (List<?>) alone.get( "aggregations" ) ).get( 0 ) ).get( "buckets" ) ).size() );
        assertNull( alone.get( "truncated" ) );
    }

    @Test
    public void testSearchRejectsAForgedDeepCursorBeforeQuerying()
    {
        addFolders( 3 );
        final Map<String, Object> args = args( "count", 10 );
        final Map<String, Object> resolved = new LinkedHashMap<>( args );
        resolved.put( "project", "myproject" );
        resolved.put( "branch", "draft" );
        args.put( "cursor", Cursors.encode( 9995, Cursors.queryHash( resolved ) ) );

        final ToolError error = assertThrows( ToolError.class, () -> as( ALICE, () -> search.execute( args ) ) );

        assertEquals( "Invalid cursor: this listing can page through at most 10000 items. Repeat the call without cursor and narrow it " +
                          "with path or types.", error.userMessage() );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testSearchStructureCarriesWhatTheSummarySays()
    {
        addFolders( 9 );
        final String cursor = (String) successStructured( as( ALICE, () -> search.execute( args( "count", 4 ) ) ) ).get( "nextCursor" );

        final Map<String, Object> result = as( ALICE, () -> search.execute( args( "count", 4, "cursor", cursor ) ) );
        final Map<String, Object> structured = successStructured( result );

        final String text = (String) ( (Map<?, ?>) ( (List<?>) result.get( "content" ) ).get( 0 ) ).get( "text" );
        assertTrue( text.startsWith( "9 items found in myproject (draft); showing 5-8 of 9, continue with nextCursor.\n" ), text );
        assertEquals( 9, structured.get( "total" ) );
        assertEquals( 4, structured.get( "offset" ) );
        assertEquals( 4, hits( structured ).size() );
        assertTrue( structured.get( "nextCursor" ) instanceof String );
        assertNull( successStructured( as( ALICE, () -> search.execute( args( "count", 4 ) ) ) ).get( "offset" ) );
    }

    @Test
    public void testSearchTruncatedBucketsAloneHintAtAggregateArguments()
    {
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        addFolders( 3 );
        final Buckets.Builder buckets = Buckets.create();
        for ( int i = 0; i < 100; i++ )
        {
            buckets.add( Bucket.create().key( "/a/fairly/long/path/used/as/a/bucket/key/" + i ).docCount( 1 ).build() );
        }
        aggregations = Aggregations.create().add( BucketAggregation.bucketAggregation( "a0" ).buckets( buckets.build() ).build() ).build();

        final Map<String, Object> structured = successStructured(
            as( ALICE, () -> search.execute( args( "aggregate", List.of( args( "by", "_path", "size", 100 ) ), "count", 0 ) ) ) );

        assertEquals( true, structured.get( "truncated" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.endsWith( ". Narrow with types, path or filters, or use fewer aggregate entries or a smaller size." ), hint );
        assertTrue( McpJson.length( structured ) <= 1000 );
    }

    @Test
    public void testGetWithFieldsTruncationHintAsksForFewerFields()
    {
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        final PropertyTree data = new PropertyTree();
        data.addString( "title", "x".repeat( 3000 ) );
        landing( data );

        final Map<String, Object> structured =
            successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1", "fields", List.of( "title" ) ) ) ) );

        assertEquals( true, structured.get( "truncated" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.endsWith( ". Request fewer fields." ), hint );
    }

    @Test
    public void testGetKeepsMostOfTheBudgetForData()
    {
        final PropertyTree data = new PropertyTree();
        for ( int i = 0; i < 4; i++ )
        {
            data.addString( "field" + i, String.valueOf( i ).repeat( 3500 ) );
        }
        landing( data );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> get.execute( args( "key", "/landings/l-1" ) ) ) );

        assertNull( structured.get( "omittedFields" ) );
        assertNull( structured.get( "truncated" ) );
        assertEquals( 4, ( (Map<?, ?>) ( (Map<?, ?>) structured.get( "content" ) ).get( "data" ) ).size() );
    }

    @Test
    public void testEmptySearchNamesUnknownTypes()
    {
        final Map<String, Object> unknown = successStructured(
            as( ALICE, () -> search.execute( args( "types", List.of( "nope", "landing", "myapplication:nada", "base:FOLDER" ) ) ) ) );

        assertEquals( 0L, ( (Number) unknown.get( "total" ) ).longValue() );
        assertEquals( "No installed content type named nope, myapplication:nada; call list-types for the type names.",
                      unknown.get( "note" ) );

        final Map<String, Object> known =
            successStructured( as( ALICE, () -> search.execute( args( "types", List.of( "Landing", "myapplication:landing" ) ) ) ) );
        assertNull( known.get( "note" ) );

        addFolders( 1 );
        assertNull( successStructured( as( ALICE, () -> search.execute( args( "types", List.of( "nope" ) ) ) ) ).get( "note" ) );
    }

    @Test
    public void testEmptySearchExplainsThePath()
    {
        content( "folder-landings", "/", "landings", "Landings" );

        assertEquals( "No content at path /nope on draft; call search without path to find paths.",
                      successStructured( as( ALICE, () -> search.execute( args( "path", "/nope" ) ) ) ).get( "note" ) );
        assertEquals( "path matches only content below /landings, not the item itself; read it with get.",
                      successStructured( as( ALICE, () -> search.execute( args( "path", "/landings/" ) ) ) ).get( "note" ) );
    }

    @Test
    public void testPathNoteOnlyWhenPathAloneRestrictsTheSearch()
    {
        content( "folder-landings", "/", "landings", "Landings" );
        content( "child-1", "/landings", "child-1", "Child 1" );

        for ( final Map<String, Object> args : List.of( args( "path", "/landings", "text", "zzz" ),
                                                       args( "path", "/landings", "types", List.of( "landing" ) ),
                                                       args( "path", "/landings", "workflow", "IN_PROGRESS" ),
                                                       args( "path", "/landings", "mine", true ),
                                                       args( "path", "/landings", "filters", List.of( filter( "data.title" ) ) ) ) )
        {
            assertNull( successStructured( as( ALICE, () -> search.execute( args ) ) ).get( "note" ), args::toString );
        }
        assertEquals( "No installed content type named nope; call list-types for the type names.",
                      successStructured( as( ALICE, () -> search.execute( args( "path", "/landings", "types", List.of( "nope" ) ) ) ) ).get(
                          "note" ) );
        assertEquals( "No content at path /nope on draft; call search without path to find paths.",
                      successStructured( as( ALICE, () -> search.execute( args( "path", "/nope", "text", "zzz" ) ) ) ).get( "note" ) );
    }

    @Test
    public void testSubtreeCutOnTheLastPageDoesNotPointAtNextCursor()
    {
        store.put( "big", Content.create()
            .id( ContentId.from( "big" ) )
            .name( "big" )
            .parentPath( ContentPath.from( "/articles" ) )
            .type( ContentTypeName.folder() )
            .displayName( "d".repeat( 1500 ) )
            .build() );
        searchable.add( "big" );
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();

        final Map<String, Object> structured = successStructured( as( ALICE, () -> search.execute( args() ) ) );

        assertEquals( true, structured.get( "truncated" ) );
        assertNull( structured.get( "nextCursor" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.endsWith( ". Narrow with text, types, path or filters, or use a smaller count." ), hint );
    }

    @Test
    public void testEmptyInProgressSearchOnMasterSaysWhy()
    {
        assertEquals( "master holds published content, which is normally not in progress; search draft as a signed-in user.",
                      successStructured( as( ALICE, () -> search.execute( args( "workflow", "IN_PROGRESS", "branch", "master" ) ) ) ).get(
                          "note" ) );
        assertNull( successStructured( as( ALICE, () -> search.execute( args( "workflow", "IN_PROGRESS" ) ) ) ).get( "note" ) );
        assertNull( successStructured( as( ALICE, () -> search.execute( args( "workflow", "READY", "branch", "master" ) ) ) ).get( "note" ) );
    }

    @Test
    public void testDateFieldsRejectValuesThatAreNotIsoDates()
    {
        for ( final Object value : List.of( "yesterday", "2026-13", "1700000000", 1700000000, true ) )
        {
            final String message = invalid( args( "filters", List.of( args( "field", "modifiedTime", "from", value ) ) ) );
            assertEquals( "Invalid filters[0].from: " + McpJson.write( value ) + " is not an ISO 8601 date, and modifiedTime is a date " +
                              "field. Use ISO 8601, e.g. 2026-01-31 or 2026-01-31T12:00:00Z.", message );
        }
        assertTrue( invalid( args( "filters", List.of( args( "field", "publish.from", "equals", "last week" ) ) ) ).startsWith(
            "Invalid filters[0].equals: \"last week\" is not an ISO 8601 date" ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "createdtime", "to", "now" ) ) ) ).startsWith(
            "Invalid filters[0].to: \"now\" is not an ISO 8601 date, and createdtime is a date field." ) );
        assertTrue( searches.isEmpty() );

        assertTrue( invalid( args( "filters", List.of( args( "field", "modifiedTime", "from", "2026-01-31T12:00:00" ) ) ) ).contains(
            "is not an ISO 8601 date" ) );

        as( ALICE, () -> search.execute( args( "filters", List.of( args( "field", "modifiedTime", "from", "2026-01-31", "to",
                                                                          "2026-02-01T12:00:00Z" ),
                                                                    args( "field", "data.title", "from", "a", "to", "m" ) ) ) ) );
        as( ALICE, () -> search.execute( args( "filters", List.of( args( "field", "publish.from", "from", "2026-01-31T12:00:00.123+02:00",
                                                                          "to", "2026-01-31T23:59:59.5-05:30" ),
                                                                    args( "field", "createdTime", "equals", "2026-01-31T12:00Z" ) ) ) ) );
        assertEquals( 2, searches.size() );
    }

    private void landing( final PropertyTree data )
    {
        store.put( "landing", Content.create()
            .id( ContentId.from( "landing" ) )
            .name( "l-1" )
            .parentPath( ContentPath.from( "/landings" ) )
            .type( LANDING )
            .displayName( "Landing 1" )
            .data( data )
            .build() );
    }

    @Test
    public void testGetUnknownKeySaysHowToFindContent()
    {
        final ToolError error = assertThrows( ToolError.class, () -> as( ALICE, () -> get.execute( args( "key", "/nothing" ) ) ) );

        assertTrue( error.userMessage().contains( "not found" ) && error.userMessage().contains( "search" ), error.userMessage() );
    }

    @Test
    public void testSearchQueryPrefersFullTypeNamesAndSingleClauses()
    {
        final Map<?, ?> bool = (Map<?, ?>) SearchQuery.from( args( "types", List.of( "a:b" ) ), null, true ).query.get( "boolean" );
        assertEquals( Map.of( "in", Map.of( "field", "type", "stringValues", List.of( "a:b" ) ) ),
                      ( (List<?>) bool.get( "must" ) ).get( 0 ) );
        assertEquals( List.of( Map.of( "name", "a0", "terms", Map.of( "field", "type", "size", 10, "order", "_count DESC" ) ) ),
                      SearchQuery.from( args( "aggregate", List.of( args( "by", "type" ) ) ), null, true ).aggregations );
        assertEquals( List.of( Map.of( "field", "_score", "direction", "DESC" ), Map.of( "field", "_path", "direction", "ASC" ) ),
                      SearchQuery.from( args( "text", "x" ), null, true ).sort );
    }

    private static Map<String, Object> filter( final String field )
    {
        return args( "field", field, "equals", "x" );
    }

    private static void assertCallerContext( final Context context )
    {
        assertEquals( "com.enonic.cms.myproject", context.getRepositoryId().toString() );
        assertEquals( ContentConstants.BRANCH_DRAFT, context.getBranch() );
        assertEquals( ALICE_KEY, context.getAuthInfo().getUser().getKey() );
        assertFalse( context.getAuthInfo().hasRole( RoleKeys.ADMIN ) );
    }

    private String invalid( final Map<String, Object> args )
    {
        return assertThrows( ToolError.class, () -> as( ALICE, () -> search.execute( args ) ) ).userMessage();
    }

    private FindContentIdsByQueryResult find( final ContentQuery query )
    {
        final Map<String, Object> dsl = dsl( query );
        if ( dsl.get( "in" ) instanceof Map<?, ?> in && "_id".equals( in.get( "field" ) ) )
        {
            labelContext.set( ContextAccessor.current() );
            final List<?> values = in.get( "values" ) instanceof List<?> list ? list : List.of( in.get( "values" ) );
            final String[] ids = values.stream().map( String::valueOf ).filter( store::containsKey ).toArray( String[]::new );
            return FindContentIdsByQueryResult.create().contents( ContentIds.from( ids ) ).totalHits( ids.length ).build();
        }

        searches.add( query );
        searchContext.set( ContextAccessor.current() );
        final List<String> page = searchable.stream().skip( query.getFrom() ).limit( query.getSize() ).toList();
        return FindContentIdsByQueryResult.create()
            .contents( ContentIds.from( page.toArray( String[]::new ) ) )
            .totalHits( searchable.size() )
            .aggregations( aggregations )
            .build();
    }

    private void addFolders( final int count )
    {
        for ( int i = 0; i < count; i++ )
        {
            final String id = "id-" + i;
            store.put( id, Content.create()
                .id( ContentId.from( id ) )
                .name( "item-" + i )
                .parentPath( ContentPath.from( "/articles" ) )
                .type( ContentTypeName.folder() )
                .displayName( "Item " + i + " with a reasonably long display name to fill the page" )
                .modifier( ALICE_KEY )
                .owner( PrincipalKey.ofUser( IdProviderKey.system(), "owner" ) )
                .modifiedTime( Instant.ofEpochSecond( 1000 - i ) )
                .workflowInfo( i == 0 ? WorkflowInfo.inProgress() : null )
                .build() );
            searchable.add( id );
        }
    }

    private Content content( final String id, final String parent, final String name, final String displayName )
    {
        final Content content = Content.create()
            .id( ContentId.from( id ) )
            .name( name )
            .parentPath( ContentPath.from( parent ) )
            .type( ContentTypeName.folder() )
            .displayName( displayName )
            .build();
        store.put( id, content );
        return content;
    }

    /**
     * The query DSL as a plain map. {@link PropertyTree#toMap()} wraps every value in a list, so single-item lists are unwrapped; use
     * {@link #unwrap(Object)} on the expected value too.
     */
    private static Map<String, Object> dsl( final ContentQuery query )
    {
        return unwrap( ( (DslExpr) query.getQueryExpr().getConstraint() ).getExpression().toMap() );
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrap( final Object value )
    {
        final Object normalized = normalize( value );
        if ( normalized instanceof Map<?, ?> map )
        {
            final Map<String, Object> result = new LinkedHashMap<>();
            map.forEach( ( key, item ) -> result.put( (String) key, unwrap( item ) ) );
            return (T) result;
        }
        if ( normalized instanceof List<?> list )
        {
            return list.size() == 1 ? unwrap( list.get( 0 ) ) : (T) list.stream().map( ContentToolsIntegrationTest::unwrap ).toList();
        }
        return (T) normalized;
    }

    private static List<String> sort( final ContentQuery query )
    {
        return query.getQueryExpr().getOrderList().stream().map( ContentToolsIntegrationTest::order ).toList();
    }

    private static String order( final OrderExpr expr )
    {
        return ( (DslOrderExpr) expr ).getField() + " " + expr.getDirection();
    }

    private static Set<String> aggregationNames( final ContentQuery query )
    {
        final Set<String> names = new HashSet<>();
        for ( final AggregationQuery aggregation : query.getAggregationQueries() )
        {
            names.add( aggregation.getName() );
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private static <T> T normalize( final Object value )
    {
        return (T) McpJson.readObject( McpJson.write( Map.of( "v", value ) ) ).get( "v" );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> hits( final Map<String, Object> structured )
    {
        return (List<Map<String, Object>>) structured.get( "hits" );
    }

    private static <T> T as( final AuthenticationInfo authInfo, final Callable<T> callable )
    {
        return ContextBuilder.copyOf( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( callable );
    }
}
