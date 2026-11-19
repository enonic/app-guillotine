package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;
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
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.Media;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.Value;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.project.Projects;
import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.expr.CompareExpr;
import com.enonic.xp.query.expr.ConstraintExpr;
import com.enonic.xp.query.expr.LogicalExpr;
import com.enonic.xp.query.expr.ValueExpr;
import com.enonic.xp.query.filter.BooleanFilter;
import com.enonic.xp.query.filter.ExistsFilter;
import com.enonic.xp.query.filter.Filter;
import com.enonic.xp.query.filter.ValueFilter;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.util.Reference;

import static com.enonic.app.guillotine.mcp.McpResults.args;
import static com.enonic.app.guillotine.mcp.McpResults.successStructured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * find-references and check-links against an in-memory index: {@code _references}, {@code _id}, {@code type} and {@code _path} are
 * evaluated the way the tools build their queries, and content in {@link #hidden} is invisible to non-admin callers, as XP's
 * permission filter makes it.
 */
public class ReferenceToolsTest
{
    private static final AuthenticationInfo ALICE = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).build() )
        .principals( RoleKeys.AUTHENTICATED )
        .build();

    private static final AuthenticationInfo ADMIN = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "admin" ) ).login( "admin" ).build() )
        .principals( RoleKeys.AUTHENTICATED, RoleKeys.ADMIN )
        .build();

    private static final ContentTypeName ARTICLE = ContentTypeName.from( "myapp:article" );

    private static final ContentTypeName LANDING = ContentTypeName.from( "myapp:landing" );

    private final Map<String, Content> store = new LinkedHashMap<>();

    private final Map<String, List<String>> index = new LinkedHashMap<>();

    private final Set<String> hidden = new HashSet<>();

    private final List<ContentQuery> queries = new ArrayList<>();

    private boolean aggregationsUnavailable;

    private ContentService contentService;

    private FindReferencesTool findReferences;

    private CheckLinksTool checkLinks;

    private ProjectBranchResolver resolver;

    private GuillotineConfigService configService;

    @BeforeEach
    public void setUp()
    {
        configService = mock( GuillotineConfigService.class );
        when( configService.getMcpDefaultProject() ).thenReturn( "myproject" );
        when( configService.getMcpMaxResultChars() ).thenReturn( 20000 );

        final NodeService nodeService = mock( NodeService.class );
        when( nodeService.getByPath( ContentConstants.CONTENT_ROOT_PATH ) ).thenReturn(
            Node.create().name( "content" ).parentPath( NodePath.ROOT ).data( new PropertyTree() ).build() );
        final ProjectService projectService = mock( ProjectService.class );
        when( projectService.list() ).thenReturn( Projects.empty() );
        resolver = new ProjectBranchResolver( projectService, nodeService, configService );

        contentService = mock( ContentService.class );
        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> find( invocation.getArgument( 0 ) ) );
        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenAnswer( invocation -> Contents.from(
            ( (GetContentByIdsParams) invocation.getArgument( 0 ) ).getIds()
                .stream()
                .map( id -> visible( id.toString() ) )
                .filter( content -> content != null )
                .toList() ) );
        when( contentService.getById( any( ContentId.class ) ) ).thenAnswer( invocation -> {
            final Content content = visible( invocation.getArgument( 0 ).toString() );
            if ( content == null )
            {
                throw ContentNotFoundException.create().contentId( invocation.getArgument( 0 ) ).build();
            }
            return content;
        } );
        when( contentService.getByPath( any( ContentPath.class ) ) ).thenAnswer( invocation -> store.values()
            .stream()
            .filter( content -> content.getPath().equals( invocation.getArgument( 0 ) ) && visible( content.getId().toString() ) != null )
            .findFirst()
            .orElseThrow( () -> ContentNotFoundException.create().contentPath( invocation.getArgument( 0 ) ).build() ) );

        findReferences = new FindReferencesTool( contentService, resolver, configService );
        checkLinks = new CheckLinksTool( contentService, resolver, configService );
    }

    @Test
    public void testInboundListsReferrersByPathAndExcludesTheTargetItself()
    {
        image( "img-1", "a.png" );
        article( "art-2", "b-article", "img-1", "" );
        article( "art-1", "a-article", "img-1", "" );
        article( "art-3", "c-article", null, "" );
        index.get( "img-1" ).add( "img-1" );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> findReferences.execute(
            args( "key", "/images/a.png", "direction", "inbound" ) ) ) );

        assertEquals( Map.of( "_id", "img-1", "_path", "/images/a.png", "displayName", "a.png", "type", "media:image" ),
                      structured.get( "target" ) );
        assertEquals( 2, ( (Number) structured.get( "total" ) ).intValue() );
        assertEquals( List.of( "/articles/a-article", "/articles/b-article" ), paths( list( structured, "inbound" ) ) );
        assertNull( structured.get( "outbound" ) );
        assertNull( structured.get( "nextCursor" ) );
        assertEquals( "myproject", structured.get( "project" ) );
        assertEquals( "draft", structured.get( "branch" ) );
    }

    @Test
    public void testOutboundResolvesSelectorsAndRichTextAndFlagsMissingTargets()
    {
        image( "img-1", "a.png" );
        article( "art-1", "a-article", "img-1", "" );
        landing( "landing-1", "with-links",
                 "<p><a href=\"content://art-1\">Article</a> <a href=\"content://gone-1\">Gone</a> <a href=\"https://example.com\">x</a>" +
                     "<img src=\"image://img-1?scale=block(2,1)\" alt=\"Photo\"><img src=\"image://dead-1\"></p>",
                 List.of( "art-1", "gone-1", "img-1", "dead-1" ) );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> findReferences.execute(
            args( "key", "landing-1", "direction", "outbound" ) ) ) );

        final List<Map<String, Object>> outbound = list( structured, "outbound" );
        assertEquals( 4, outbound.size(), outbound::toString );
        assertEquals( Map.of( "field", "data.body", "kind", "content", "_id", "art-1", "_path", "/articles/a-article", "displayName",
                              "a-article", "type", "myapp:article", "text", "Article" ), outbound.get( 0 ) );
        assertEquals( Map.of( "field", "data.body", "kind", "content", "_id", "gone-1", "missing", true, "text", "Gone" ),
                      outbound.get( 1 ) );
        assertEquals( "/images/a.png", outbound.get( 2 ).get( "_path" ) );
        assertEquals( "image", outbound.get( 2 ).get( "kind" ) );
        assertEquals( "Photo", outbound.get( 2 ).get( "text" ) );
        assertEquals( Map.of( "field", "data.body", "kind", "image", "_id", "dead-1", "missing", true ), outbound.get( 3 ) );
        assertNull( structured.get( "inbound" ) );
        assertNull( structured.get( "total" ) );
    }

    @Test
    public void testBothDirectionsAndSelectorKindFromTargetType()
    {
        person( "person-1", "ada" );
        image( "img-1", "a.png" );
        article( "art-1", "a-article", "img-1", "" );
        addReference( "art-1", "author", "person-1" );
        landing( "landing-1", "home", "<a href=\"content://art-1\">Read</a>", List.of( "art-1" ) );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> findReferences.execute( args( "key", "art-1" ) ) ) );

        assertEquals( List.of( "/landings/home" ), paths( list( structured, "inbound" ) ) );
        final List<Map<String, Object>> outbound = list( structured, "outbound" );
        assertEquals( List.of( "data.cover", "data.author" ), outbound.stream().map( entry -> entry.get( "field" ) ).toList() );
        assertEquals( List.of( "image", "content" ), outbound.stream().map( entry -> entry.get( "kind" ) ).toList() );
        assertEquals( List.of( "/images/a.png", "/persons/ada" ), paths( outbound ) );
    }

    @Test
    public void testUnreadableReferrerIsNeitherListedNorCounted()
    {
        image( "img-1", "a.png" );
        article( "art-1", "a-article", "img-1", "" );
        article( "art-2", "secret-article", "img-1", "" );
        hidden.add( "art-2" );

        final Map<String, Object> asAlice = successStructured( as( ALICE, () -> findReferences.execute(
            args( "key", "img-1", "direction", "inbound" ) ) ) );
        assertEquals( 1, ( (Number) asAlice.get( "total" ) ).intValue() );
        assertEquals( List.of( "/articles/a-article" ), paths( list( asAlice, "inbound" ) ) );
        assertFalse( McpJson.write( asAlice ).contains( "art-2" ) );
        assertFalse( McpJson.write( asAlice ).contains( "secret" ) );

        final Map<String, Object> asAdmin = successStructured( as( ADMIN, () -> findReferences.execute(
            args( "key", "img-1", "direction", "inbound" ) ) ) );
        assertEquals( 2, ( (Number) asAdmin.get( "total" ) ).intValue() );
    }

    @Test
    public void testUnreadableTargetLooksExactlyLikeAMissingOne()
    {
        image( "img-1", "a.png" );
        image( "img-2", "secret.png" );
        hidden.add( "img-2" );
        landing( "landing-1", "home", "<img src=\"image://img-2\"><img src=\"image://never-1\">", List.of( "img-2", "never-1" ) );

        final List<Map<String, Object>> outbound = list( successStructured( as( ALICE, () -> findReferences.execute(
            args( "key", "/landings/home", "direction", "outbound" ) ) ) ), "outbound" );

        assertEquals( Map.of( "field", "data.body", "kind", "image", "_id", "img-2", "missing", true ), outbound.get( 0 ) );
        assertEquals( Map.of( "field", "data.body", "kind", "image", "_id", "never-1", "missing", true ), outbound.get( 1 ) );

        final ToolError error = assertThrows( ToolError.class, () -> as( ALICE, () -> findReferences.execute(
            args( "key", "/images/secret.png" ) ) ) );
        assertTrue( error.userMessage().contains( "not found" ), error.userMessage() );
    }

    @Test
    public void testDeletedIdStillAnswersInbound()
    {
        landing( "landing-1", "home", "<a href=\"content://gone-1\">Gone</a>", List.of( "gone-1" ) );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> findReferences.execute( args( "key", "gone-1" ) ) ) );

        assertEquals( true, ( (Map<?, ?>) structured.get( "target" ) ).get( "missing" ) );
        assertEquals( List.of( "/landings/home" ), paths( list( structured, "inbound" ) ) );
        assertNull( structured.get( "outbound" ) );

        assertThrows( ToolError.class, () -> as( ALICE, () -> findReferences.execute(
            args( "key", "gone-1", "direction", "outbound" ) ) ) );
    }

    @Test
    public void testInboundPagination()
    {
        image( "img-1", "a.png" );
        for ( int i = 0; i < 5; i++ )
        {
            article( "art-" + i, "article-" + i, "img-1", "" );
        }

        final List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do
        {
            final Map<String, Object> args = args( "key", "img-1", "direction", "inbound", "count", 2 );
            if ( cursor != null )
            {
                args.put( "cursor", cursor );
            }
            final Map<String, Object> structured = successStructured( as( ALICE, () -> findReferences.execute( args ) ) );
            seen.addAll( paths( list( structured, "inbound" ) ) );
            cursor = (String) structured.get( "nextCursor" );
            pages++;
        }
        while ( cursor != null && pages < 10 );

        assertEquals( 3, pages );
        assertEquals( List.of( "/articles/article-0", "/articles/article-1", "/articles/article-2", "/articles/article-3",
                               "/articles/article-4" ), seen );
    }

    @Test
    public void testUnusedMediaIsTheSetDifferenceWithStablePages()
    {
        for ( int i = 1; i <= 7; i++ )
        {
            image( "img-" + i, "img-0" + i + ".png" );
        }
        article( "art-1", "a", "img-2", "" );
        article( "art-2", "b", "img-5", "" );
        landing( "landing-1", "home", "<img src=\"image://img-7\">", List.of( "img-7" ) );
        assertUnused( List.of( "/images/img-01.png", "/images/img-03.png", "/images/img-04.png", "/images/img-06.png" ) );

        aggregationsUnavailable = true;
        assertUnused( List.of( "/images/img-01.png", "/images/img-03.png", "/images/img-04.png", "/images/img-06.png" ) );
    }

    @Test
    public void testUnusedMediaCountsOnlyReadableReferrersAndOnlyReadableMedia()
    {
        image( "img-1", "a.png" );
        image( "img-2", "b.png" );
        image( "img-3", "secret.png" );
        hidden.add( "img-3" );
        article( "art-1", "secret", "img-1", "" );
        hidden.add( "art-1" );

        final Map<String, Object> asAlice =
            successStructured( as( ALICE, () -> findReferences.execute( args( "unusedMedia", true, "types", List.of( "image" ) ) ) ) );
        assertEquals( List.of( "/images/a.png", "/images/b.png" ), paths( list( asAlice, "unused" ) ) );
        assertEquals( 2, asAlice.get( "mediaChecked" ) );
        assertEquals( "Unused means not referenced by any content readable to the current user.", asAlice.get( "note" ) );

        final Map<String, Object> asAdmin = successStructured( as( ADMIN, () -> findReferences.execute( args( "unusedMedia", true ) ) ) );
        assertEquals( List.of( "/images/b.png", "/images/secret.png" ), paths( list( asAdmin, "unused" ) ) );
    }

    @Test
    public void testArgumentErrors()
    {
        assertTrue( error( args() ).contains( "key is required" ) );
        assertTrue( error( args( "key", "x", "direction", "sideways" ) ).contains( "Invalid direction" ) );
        assertTrue( error( args( "key", "x", "unusedMedia", true ) ).contains( "either key or unusedMedia" ) );
        assertTrue( error( args( "unusedMedia", "yes" ) ).contains( "unusedMedia" ) );
        assertTrue( error( args( "unusedMedia", true, "types", List.of( "myapp:article" ) ) ).contains( "not a media type" ) );
        assertTrue( error( args( "unusedMedia", true, "types", List.of( "article" ) ) ).contains( "media types like media:image" ) );
        assertTrue( error( args( "key", "/images/../x" ) ).contains( "Invalid key" ) );
        assertTrue( error( args( "key", "", "direction", "inbound" ) ).contains( "Invalid key" ) );
        for ( final String key : List.of( "/articles/a\\b", "/articles/x|y", "/articles/a?b", "x y" ) )
        {
            for ( final String direction : List.of( "outbound", "inbound" ) )
            {
                final String message = error( args( "key", key, "direction", direction ) );
                assertTrue( message.startsWith( "Invalid key: " ) && message.contains( "Paths start with /" ), message );
            }
        }
        image( "img-1", "a.png" );
        final String cursor = (String) successStructured( as( ALICE, () -> findReferences.execute( args( "unusedMedia", true, "count", 0 ) ) ) )
            .get( "nextCursor" );
        assertNull( cursor );
        final String foreign = Cursors.encode( 1, "0000000000000000" );
        assertTrue( error( args( "unusedMedia", true, "cursor", foreign ) ).contains( "does not match" ) );
        assertTrue( queries.stream().noneMatch( query -> query.getSize() > FindReferencesTool.MAX_MEDIA ) );
    }

    @Test
    public void testAccessDeniedBecomesBranchHint()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenThrow( new ForbiddenAccessException( null ) );

        final ToolError error = assertThrows( ToolError.class, () -> as( AuthenticationInfo.unAuthenticated(),
                                                                         () -> checkLinks.execute( args( "branch", "draft" ) ) ) );
        assertTrue( error.userMessage().contains( "master" ), error.userMessage() );
    }

    @Test
    public void testCheckLinksReportsBrokenRichTextAndSelectorTargets()
    {
        image( "img-1", "a.png" );
        article( "art-1", "a-article", "img-1", "" );
        article( "art-2", "b-article", "img-gone", "" );
        landing( "landing-1", "deleted-link", "<a href=\"content://art-1\">Fine</a><a href=\"content://gone-1\">Link to gone</a>",
                 List.of( "art-1", "gone-1" ) );
        landing( "landing-2", "missing-image", "<img src=\"image://dead-1\" alt=\"missing\">", List.of( "dead-1" ) );
        content( "folder-1", "/", "landings", ContentTypeName.folder(), new PropertyTree() );

        final Map<String, Object> landings = successStructured( as( ALICE, () -> checkLinks.execute( args( "path", "/landings" ) ) ) );
        assertEquals( 2, ( (Number) landings.get( "total" ) ).intValue() );
        assertEquals( 2, landings.get( "scanned" ) );
        assertEquals( 2, landings.get( "brokenOnPage" ) );
        assertEquals( true, landings.get( "complete" ) );
        assertFalse( landings.containsKey( "nextCursor" ) );
        assertEquals( List.of( Map.of( "source", "/landings/deleted-link", "sourceId", "landing-1", "field", "data.body", "kind", "content",
                                       "targetId", "gone-1", "text", "Link to gone", "reason", ContentLinks.MISSING_REASON ),
                               Map.of( "source", "/landings/missing-image", "sourceId", "landing-2", "field", "data.body", "kind", "image",
                                       "targetId", "dead-1", "text", "missing", "reason", ContentLinks.MISSING_REASON ) ),
                      list( landings, "broken" ) );

        final Map<String, Object> articles = successStructured( as( ALICE, () -> checkLinks.execute( args( "types", List.of( "article" ) ) ) ) );
        assertEquals( 2, ( (Number) articles.get( "total" ) ).intValue() );
        assertEquals( List.of( Map.of( "source", "/articles/b-article", "sourceId", "art-2", "field", "data.cover", "kind", "content",
                                       "targetId", "img-gone", "reason", ContentLinks.MISSING_REASON ) ), list( articles, "broken" ) );
    }

    @Test
    public void testCheckLinksPagesOverSourcesAndSkipsUnreadableSources()
    {
        for ( int i = 0; i < 5; i++ )
        {
            landing( "landing-" + i, "page-" + i, "<a href=\"content://gone-" + i + "\">x</a>", List.of( "gone-" + i ) );
        }
        hidden.add( "landing-3" );
        folder( "landings" );
        folder( "articles" );
        image( "img-1", "a.png" );
        image( "img-2", "secret.png" );
        hidden.add( "img-2" );
        article( "art-1", "a", "img-2", "" );

        final List<String> sources = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        long total = -1;
        do
        {
            final Map<String, Object> args = args( "path", "/landings", "count", 3 );
            if ( cursor != null )
            {
                args.put( "cursor", cursor );
            }
            final Map<String, Object> structured = successStructured( as( ALICE, () -> checkLinks.execute( args ) ) );
            total = ( (Number) structured.get( "total" ) ).longValue();
            list( structured, "broken" ).forEach( entry -> sources.add( (String) entry.get( "source" ) ) );
            cursor = (String) structured.get( "nextCursor" );
            pages++;
        }
        while ( cursor != null && pages < 10 );

        assertEquals( 4, total );
        assertEquals( 2, pages );
        assertEquals( List.of( "/landings/page-0", "/landings/page-1", "/landings/page-2", "/landings/page-4" ), sources );

        final Map<String, Object> article = successStructured( as( ALICE, () -> checkLinks.execute( args( "path", "/articles" ) ) ) );
        assertEquals( "img-2", list( article, "broken" ).get( 0 ).get( "targetId" ) );
        assertEquals( ContentLinks.MISSING_REASON, list( article, "broken" ).get( 0 ).get( "reason" ) );
    }

    @Test
    public void testCheckLinksReportsOnlyInternalTargets()
    {
        landing( "landing-1", "home", "<a href=\"https://example.com/broken\">x</a><a href=\"content://gone-1\">y</a>",
                 List.of( "gone-1" ) );
        landing( "landing-2", "external-only", "<a href=\"https://example.com/also-broken\">x</a>", List.of() );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> checkLinks.execute( args() ) ) );
        assertEquals( 1, ( (Number) structured.get( "total" ) ).intValue() );
        assertEquals( 1, structured.get( "linksChecked" ) );
        assertEquals( List.of( "gone-1" ), list( structured, "broken" ).stream().map( entry -> entry.get( "targetId" ) ).toList() );
        assertFalse( McpJson.write( structured ).contains( "example.com" ) );
    }

    @Test
    public void testCheckLinksRejectsBadAndUnknownPaths()
    {
        folder( "landings" );
        for ( final String path : List.of( "landings*", "/landings/..", "/landings/./x", "landings/x", "/a//b", "/landings\\x",
                                           "/landings|x" ) )
        {
            final ToolError error = assertThrows( ToolError.class, () -> as( ALICE, () -> checkLinks.execute( args( "path", path ) ) ),
                                                  path );
            assertTrue( error.userMessage().contains( "Invalid path" ), error.userMessage() );
        }
        queries.clear();
        final ToolError missing =
            assertThrows( ToolError.class, () -> as( ALICE, () -> checkLinks.execute( args( "path", "/no/such/path" ) ) ) );
        assertTrue( missing.userMessage().contains( "not found or not readable" ) && missing.userMessage().contains( "search" ),
                    missing.userMessage() );
        assertTrue( queries.isEmpty() );

        content( "secret-folder", "/", "secret", ContentTypeName.folder(), new PropertyTree() );
        hidden.add( "secret-folder" );
        assertThrows( ToolError.class, () -> as( ALICE, () -> checkLinks.execute( args( "path", "/secret" ) ) ) );
        assertEquals( 0, ( (Number) successStructured( as( ALICE, () -> checkLinks.execute( args( "path", "/landings/" ) ) ) ).get( "total" ) )
            .intValue() );
    }

    @Test
    public void testForgedDeepCursorIsRejectedBeforeQuerying()
    {
        folder( "landings" );
        final Map<String, Object> inbound = args( "key", "img-1", "direction", "inbound" );
        final String inboundCursor =
            Cursors.encode( Integer.MAX_VALUE, Cursors.queryHash( "find-references", withTarget( inbound ) ) );
        inbound.put( "cursor", inboundCursor );
        final Map<String, Object> links = args( "path", "/landings" );
        links.put( "cursor", Cursors.encode( 9995, Cursors.queryHash( "check-links", withTarget( links ) ) ) );

        queries.clear();
        final String inboundError = error( inbound );
        assertTrue( inboundError.contains( "at most " + Cursors.MAX_WINDOW ), inboundError );
        final ToolError linksError = assertThrows( ToolError.class, () -> as( ALICE, () -> checkLinks.execute( links ) ) );
        assertTrue( linksError.userMessage().contains( "without cursor" ), linksError.userMessage() );
        assertTrue( queries.isEmpty() );
    }

    @Test
    public void testOutboundCapIsExplicit()
    {
        final List<String> targets = new ArrayList<>();
        final StringBuilder body = new StringBuilder();
        for ( int i = 0; i < 250; i++ )
        {
            targets.add( "gone-" + i );
            body.append( "<a href=\"content://gone-" ).append( i ).append( "\">").append( i ).append( "</a>" );
        }
        landing( "landing-1", "many", body.toString(), targets );

        final Map<String, Object> result =
            as( ALICE, () -> findReferences.execute( args( "key", "landing-1", "direction", "outbound", "count", 0 ) ) );
        final Map<String, Object> structured = successStructured( result );
        assertEquals( 250, structured.get( "outboundTotal" ) );
        assertEquals( true, structured.get( "outboundTruncated" ) );
        assertTrue( String.valueOf( structured.get( "outboundHint" ) ).contains( "check-links" ) );
        final int listed = list( structured, "outbound" ).size();
        assertTrue( listed > 0 && listed <= FindReferencesTool.MAX_OUTBOUND, String.valueOf( listed ) );
        assertTrue( text( result ).startsWith( "/landings/many: listing the first " + listed + " of 250 items it references" ),
                    text( result ) );
        assertTrue( McpJson.length( structured ) <= 20000 );
    }

    @Test
    public void testCheckLinksPageBreaksLoseNothing()
    {
        when( configService.getMcpMaxResultChars() ).thenReturn( 3000 );
        final List<String> all = new ArrayList<>();
        for ( int i = 0; i < 5; i++ )
        {
            final List<String> targets = new ArrayList<>();
            final StringBuilder body = new StringBuilder();
            for ( int j = 0; j < 4; j++ )
            {
                final String id = "gone-" + i + "-" + j;
                targets.add( id );
                all.add( id );
                body.append( "<a href=\"content://" ).append( id ).append( "\">A rather long link text number " ).append( j )
                    .append( "</a>" );
            }
            landing( "landing-" + i, "page-" + i, body.toString(), targets );
        }

        final List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do
        {
            final Map<String, Object> args = args( "count", 50 );
            if ( cursor != null )
            {
                args.put( "cursor", cursor );
            }
            final Map<String, Object> result = as( ALICE, () -> checkLinks.execute( args ) );
            final Map<String, Object> structured = successStructured( result );
            assertTrue( McpJson.length( structured ) <= 3000, () -> String.valueOf( McpJson.length( structured ) ) );
            assertNull( structured.get( "truncated" ) );
            assertEquals( list( structured, "broken" ).size(), structured.get( "brokenOnPage" ) );
            assertTrue( text( result ).contains( ": " + structured.get( "brokenOnPage" ) + " broken link" ), text( result ) );
            assertEquals( structured.get( "nextCursor" ) == null, structured.get( "complete" ) );
            list( structured, "broken" ).forEach( entry -> seen.add( (String) entry.get( "targetId" ) ) );
            cursor = (String) structured.get( "nextCursor" );
            pages++;
        }
        while ( cursor != null && pages < 20 );

        assertTrue( pages > 1, String.valueOf( pages ) );
        assertEquals( all, seen );
    }

    @Test
    public void testSourceWithTooManyBrokenLinksIsNamedNotSilentlyCut()
    {
        when( configService.getMcpMaxResultChars() ).thenReturn( 3000 );
        final List<String> targets = new ArrayList<>();
        final StringBuilder body = new StringBuilder();
        for ( int j = 0; j < 40; j++ )
        {
            targets.add( "gone-" + j );
            body.append( "<a href=\"content://gone-" ).append( j ).append( "\">link ").append( j ).append( "</a>" );
        }
        landing( "landing-0", "huge", body.toString(), targets );
        landing( "landing-1", "next", "<a href=\"content://gone-x\">x</a>", List.of( "gone-x" ) );

        final Map<String, Object> result = as( ALICE, () -> checkLinks.execute( args() ) );
        final Map<String, Object> structured = successStructured( result );
        assertTrue( McpJson.length( structured ) <= 3000 );
        assertNull( structured.get( "truncated" ) );
        assertEquals( 1, structured.get( "scanned" ) );
        final int listed = list( structured, "broken" ).size();
        assertTrue( listed > 0 && listed < 40, String.valueOf( listed ) );
        assertEquals( listed, structured.get( "brokenOnPage" ) );
        assertEquals( false, structured.get( "complete" ) );
        assertEquals( List.of( Map.of( "source", "/landings/huge", "sourceId", "landing-0", "brokenTotal", 40, "listed", listed ) ),
                      list( structured, "truncatedSources" ) );
        assertTrue( text( result ).contains( "find-references" ), text( result ) );
        assertTrue( ( (String) structured.get( "note" ) ).contains( "find-references with their sourceId" ) );
        assertTrue( structured.get( "nextCursor" ) != null );
    }

    @Test
    public void testInvalidIdIsSkippedNotFatal()
    {
        image( "img-1", "a.png" );
        final ContentLinks links = new ContentLinks( contentService );

        final Map<String, Content> readable =
            as( ALICE, () -> links.readable( new ProjectBranch( "myproject", "draft" ), List.of( "Not A Valid Id!", "img-1" ) ) );

        assertEquals( List.of( "img-1" ), List.copyOf( readable.keySet() ) );
        assertFalse( new ContentLinks.Ref( "Not A Valid Id!", "data.x", null, null ).valid() );
        assertTrue( new ContentLinks.Ref( "img-1", "data.x", null, null ).valid() );
    }

    @Test
    public void testSaturatedAggregationIsSplitWithinBounds()
    {
        for ( int i = 1; i <= 8; i++ )
        {
            image( "img-" + i, "img-0" + i + ".png" );
        }
        for ( int i = 1; i <= 6; i++ )
        {
            article( "art-" + i, "article-" + i, "img-" + i, "" );
        }

        final FindReferencesTool roomy = new FindReferencesTool( new ContentLinks( contentService ), resolver, configService, 2, 200 );
        final Map<String, Object> complete = successStructured( as( ALICE, () -> roomy.execute( args( "unusedMedia", true ) ) ) );
        assertEquals( List.of( "/images/img-07.png", "/images/img-08.png" ), paths( list( complete, "unused" ) ) );
        assertNull( complete.get( "incomplete" ) );

        queries.clear();
        final FindReferencesTool tight = new FindReferencesTool( new ContentLinks( contentService ), resolver, configService, 2, 4 );
        final Map<String, Object> result = as( ALICE, () -> tight.execute( args( "unusedMedia", true ) ) );
        final Map<String, Object> partial = successStructured( result );
        assertEquals( true, partial.get( "incomplete" ) );
        assertTrue( ( (Number) partial.get( "unchecked" ) ).intValue() > 0 );
        assertTrue( text( result ).contains( "Incomplete" ), text( result ) );
        assertTrue( ( (String) partial.get( "note" ) ).contains( "Incomplete: " + partial.get( "unchecked" ) +
                                                                     " media items could not be checked" ), partial::toString );
        assertTrue( ( (String) partial.get( "note" ) ).endsWith( "narrow with types." ) );
        final List<String> unused = paths( list( partial, "unused" ) );
        assertTrue( List.of( "/images/img-07.png", "/images/img-08.png" ).containsAll( unused ), unused::toString );
        // one media query, one chunk query and at most 4 extra halving queries
        assertTrue( queries.size() <= 6, String.valueOf( queries.size() ) );
    }

    @Test
    public void testFindReferencesTruncationHintNamesOnlyItsOwnArguments()
    {
        when( configService.getMcpMaxResultChars() ).thenReturn( 1000 );
        image( "img-1", "a.png" );
        for ( int i = 0; i < 30; i++ )
        {
            article( "art-" + i, "article-with-a-rather-long-name-" + i, "img-1", "" );
            image( "unused-" + i, "unused-image-with-a-rather-long-name-" + i + ".png" );
        }

        final Map<String, Object> inbound =
            successStructured( as( ALICE, () -> findReferences.execute( args( "key", "img-1", "direction", "inbound", "count", 50 ) ) ) );
        final Map<String, Object> unused = successStructured( as( ALICE, () -> findReferences.execute( args( "unusedMedia", true, "count", 50 ) ) ) );

        for ( final Map<String, Object> structured : List.of( inbound, unused ) )
        {
            assertEquals( true, structured.get( "truncated" ) );
            assertNotNull( structured.get( "nextCursor" ) );
            final String hint = (String) structured.get( "hint" );
            assertTrue( hint.endsWith( ". Use a smaller count or narrow with types. Continue with nextCursor." ), hint );
            assertFalse( hint.contains( "fields" ) || hint.contains( "path" ), hint );
            assertTrue( McpJson.length( structured ) <= 1000 );
        }
    }

    @Test
    public void testOutboundTruncationHintDoesNotAskForASmallerCount()
    {
        when( configService.getMcpMaxResultChars() ).thenReturn( 1000 );
        final List<String> targets = new ArrayList<>();
        final StringBuilder body = new StringBuilder();
        for ( int i = 0; i < 30; i++ )
        {
            targets.add( "gone-" + i );
            body.append( "<a href=\"content://gone-" ).append( i ).append( "\">a fairly long link text ").append( i ).append( "</a>" );
        }
        landing( "landing-1", "many", body.toString(), targets );

        final Map<String, Object> structured =
            successStructured( as( ALICE, () -> findReferences.execute( args( "key", "landing-1", "direction", "outbound" ) ) ) );

        assertEquals( true, structured.get( "truncated" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.endsWith( ". Narrow with types." ), hint );
    }

    @Test
    public void testFindReferencesCountSaysWhatZeroReturns()
    {
        final ToolError error =
            assertThrows( ToolError.class, () -> as( ALICE, () -> findReferences.execute( args( "unusedMedia", true, "count", 51 ) ) ) );

        assertEquals( "Invalid count: 51. Use an integer from 0 to 50 (default 10); 0 returns only the total.", error.userMessage() );
    }

    @Test
    public void testCheckLinksRejectsCountZero()
    {
        landing( "landing-1", "page", "<a href=\"content://gone-1\">x</a>", List.of( "gone-1" ) );

        final ToolError error = assertThrows( ToolError.class, () -> as( ALICE, () -> checkLinks.execute( args( "count", 0 ) ) ) );

        assertEquals( "Invalid count: 0. Use an integer from 1 to 50 (default 50).", error.userMessage() );
    }

    @Test
    public void testCheckLinksPageWithLongTextsStaysConsistentAfterTextsAreCut()
    {
        for ( int i = 0; i < 3; i++ )
        {
            landing( "landing-" + i, "page-" + i, "<a href=\"content://gone-" + i + "\">" + "t".repeat( 7000 ) + "</a>",
                     List.of( "gone-" + i ) );
        }

        final Map<String, Object> first = successStructured( as( ALICE, () -> checkLinks.execute( args() ) ) );

        assertEquals( 2, first.get( "scanned" ) );
        assertEquals( 2, list( first, "broken" ).size() );
        assertEquals( 2, first.get( "brokenOnPage" ) );
        assertEquals( true, first.get( "truncated" ) );
        assertEquals( false, first.get( "complete" ) );
        assertEquals( Map.of( "truncated", true, "chars", 7002 ), list( first, "broken" ).get( 1 ).get( "text" ) );
        assertTrue( McpJson.length( first ) <= 20000 );

        final Map<String, Object> second = successStructured( as( ALICE, () -> checkLinks.execute( args( "cursor", first.get( "nextCursor" ) ) ) ) );
        assertEquals( List.of( "/landings/page-2" ), list( second, "broken" ).stream().map( entry -> entry.get( "source" ) ).toList() );
        assertEquals( true, second.get( "complete" ) );
    }

    @Test
    public void testCheckLinksTruncationHintNamesOnlyItsOwnArguments()
    {
        landing( "landing-1", "huge-text", "<a href=\"content://gone-1\">" + "t".repeat( 7000 ) + "</a>", List.of( "gone-1" ) );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> checkLinks.execute( args() ) ) );

        assertEquals( true, structured.get( "truncated" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.contains( "replaced 'broken[0].text'" ), hint );
        assertTrue( hint.endsWith( ". Narrow with path or types, or use a smaller count." ), hint );
    }

    @Test
    public void testCheckLinksScansFiftySourcesByDefaultAndSaysWhenItIsNotComplete()
    {
        for ( int i = 0; i < 60; i++ )
        {
            landing( "landing-" + i, "page-%02d".formatted( i ), "<a href=\"content://gone-" + i + "\">x</a>", List.of( "gone-" + i ) );
        }

        final Map<String, Object> first = as( ALICE, () -> checkLinks.execute( args() ) );
        final Map<String, Object> structured = successStructured( first );

        assertEquals( 60, ( (Number) structured.get( "total" ) ).intValue() );
        assertEquals( CheckLinksTool.DEFAULT_COUNT, structured.get( "scanned" ) );
        assertEquals( 50, structured.get( "brokenOnPage" ) );
        assertEquals( false, structured.get( "complete" ) );
        assertNotNull( structured.get( "nextCursor" ) );
        assertTrue( text( first ).contains( "Not complete: continue with nextCursor." ), text( first ) );

        final Map<String, Object> next = args( "cursor", structured.get( "nextCursor" ) );
        final Map<String, Object> last = successStructured( as( ALICE, () -> checkLinks.execute( next ) ) );
        assertEquals( 10, last.get( "scanned" ) );
        assertEquals( true, last.get( "complete" ) );
        assertNull( last.get( "nextCursor" ) );
    }

    private void assertUnused( final List<String> expected )
    {
        final List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do
        {
            final Map<String, Object> args = args( "unusedMedia", true, "count", 3 );
            if ( cursor != null )
            {
                args.put( "cursor", cursor );
            }
            final Map<String, Object> structured = successStructured( as( ALICE, () -> findReferences.execute( args ) ) );
            assertEquals( expected.size(), ( (Number) structured.get( "total" ) ).intValue() );
            assertEquals( 7, structured.get( "mediaChecked" ) );
            seen.addAll( paths( list( structured, "unused" ) ) );
            cursor = (String) structured.get( "nextCursor" );
            pages++;
        }
        while ( cursor != null && pages < 10 );
        assertEquals( 2, pages );
        assertEquals( expected, seen );
    }

    private String error( final Map<String, Object> args )
    {
        return assertThrows( ToolError.class, () -> as( ALICE, () -> findReferences.execute( args ) ) ).userMessage();
    }

    private void folder( final String name )
    {
        content( "folder-" + name, "/", name, ContentTypeName.folder(), new PropertyTree() );
    }

    private static Map<String, Object> withTarget( final Map<String, Object> args )
    {
        final Map<String, Object> resolved = new LinkedHashMap<>( args );
        resolved.put( "project", "myproject" );
        resolved.put( "branch", "draft" );
        return resolved;
    }

    private void image( final String id, final String name )
    {
        put( Media.create()
                 .id( ContentId.from( id ) )
                 .name( name )
                 .parentPath( ContentPath.from( "/images" ) )
                 .type( ContentTypeName.imageMedia() )
                 .displayName( name )
                 .data( new PropertyTree() )
                 .build() );
    }

    private void person( final String id, final String name )
    {
        content( id, "/persons", name, ContentTypeName.from( "myapp:person" ), new PropertyTree() );
    }

    private void article( final String id, final String name, final String cover, final String body )
    {
        final PropertyTree data = new PropertyTree();
        data.addString( "title", name );
        if ( cover != null )
        {
            data.addReference( "cover", Reference.from( cover ) );
        }
        data.addString( "body", body );
        content( id, "/articles", name, ARTICLE, data );
    }

    private void landing( final String id, final String name, final String body, final List<String> processed )
    {
        final PropertyTree data = new PropertyTree();
        data.addString( "body", body );
        final Content content = Content.create()
            .id( ContentId.from( id ) )
            .name( name )
            .parentPath( ContentPath.from( "/landings" ) )
            .type( LANDING )
            .displayName( name )
            .data( data )
            .processedReferences( ContentIds.from( processed.toArray( String[]::new ) ) )
            .build();
        put( content );
    }

    private void addReference( final String id, final String name, final String target )
    {
        final Content content = store.get( id );
        final PropertyTree data = content.getData().copy();
        data.addReference( name, Reference.from( target ) );
        put( Content.create( content ).data( data ).build() );
    }

    private void content( final String id, final String parent, final String name, final ContentTypeName type, final PropertyTree data )
    {
        put( Content.create()
                 .id( ContentId.from( id ) )
                 .name( name )
                 .parentPath( ContentPath.from( parent ) )
                 .type( type )
                 .displayName( name )
                 .data( data )
                 .build() );
    }

    /**
     * Stores the content and indexes its {@code _references} the way XP does: every reference value plus the processed references.
     */
    private void put( final Content content )
    {
        final String id = content.getId().toString();
        store.put( id, content );
        final List<String> refs = new ArrayList<>();
        content.getData().getProperties( com.enonic.xp.data.ValueTypes.REFERENCE ).forEach( p -> refs.add( p.getReference().toString() ) );
        content.getProcessedReferences().forEach( ref -> refs.add( ref.toString() ) );
        index.put( id, refs );
    }

    private Content visible( final String id )
    {
        final boolean admin = ContextAccessor.current().getAuthInfo().hasRole( RoleKeys.ADMIN );
        return admin || !hidden.contains( id ) ? store.get( id ) : null;
    }

    private FindContentIdsByQueryResult find( final ContentQuery query )
    {
        queries.add( query );
        Predicate<Content> matches = content -> visible( content.getId().toString() ) != null;
        for ( final Filter filter : query.getQueryFilters() )
        {
            matches = matches.and( filter( filter ) );
        }
        if ( query.getQueryExpr() != null && query.getQueryExpr().getConstraint() != null )
        {
            matches = matches.and( constraint( query.getQueryExpr().getConstraint() ) );
        }
        final List<Content> hits =
            store.values().stream().filter( matches ).sorted( Comparator.comparing( content -> content.getPath().toString() ) ).toList();

        final Aggregations.Builder aggregations = Aggregations.create();
        for ( final AggregationQuery aggregation : query.getAggregationQueries() )
        {
            final Map<String, Long> counts = new LinkedHashMap<>();
            if ( !aggregationsUnavailable )
            {
                hits.forEach( hit -> index.get( hit.getId().toString() ).forEach( ref -> counts.merge( ref, 1L, Long::sum ) ) );
            }
            final Buckets.Builder buckets = Buckets.create();
            counts.forEach( ( key, count ) -> buckets.add( Bucket.create().key( key ).docCount( count ).build() ) );
            aggregations.add( BucketAggregation.bucketAggregation( aggregation.getName() ).buckets( buckets.build() ).build() );
        }

        final String[] page = hits.stream().skip( query.getFrom() ).limit( query.getSize() ).map( c -> c.getId().toString() )
            .toArray( String[]::new );
        return FindContentIdsByQueryResult.create()
            .contents( ContentIds.from( page ) )
            .totalHits( hits.size() )
            .aggregations( aggregations.build() )
            .build();
    }

    private Predicate<Content> filter( final Filter filter )
    {
        if ( filter instanceof BooleanFilter bool )
        {
            Predicate<Content> result = content -> true;
            for ( final Filter must : bool.getMust() )
            {
                result = result.and( filter( must ) );
            }
            for ( final Filter mustNot : bool.getMustNot() )
            {
                result = result.and( filter( mustNot ).negate() );
            }
            return result;
        }
        if ( filter instanceof ExistsFilter exists )
        {
            assertEquals( ContentLinks.REFERENCES, exists.getFieldName() );
            return content -> !index.get( content.getId().toString() ).isEmpty();
        }
        final ValueFilter value = (ValueFilter) filter;
        final Set<String> values = new HashSet<>( value.getValues().stream().map( Value::asString ).toList() );
        return switch ( value.getFieldName() )
        {
            case ContentLinks.REFERENCES -> content -> index.get( content.getId().toString() ).stream().anyMatch( values::contains );
            case "_id" -> content -> values.contains( content.getId().toString() );
            default -> throw new AssertionError( "Unexpected filter field " + value.getFieldName() );
        };
    }

    private Predicate<Content> constraint( final ConstraintExpr expr )
    {
        if ( expr instanceof LogicalExpr logical )
        {
            final Predicate<Content> left = constraint( logical.getLeft() );
            final Predicate<Content> right = constraint( logical.getRight() );
            return logical.getOperator() == LogicalExpr.Operator.AND ? left.and( right ) : left.or( right );
        }
        final CompareExpr compare = (CompareExpr) expr;
        final String field = compare.getField().getFieldPath();
        final List<String> values = compare.getValues().stream().map( ValueExpr::getValue ).map( Value::asString ).toList();
        return content -> {
            final String actual = switch ( field )
            {
                case "type" -> content.getType().toString();
                case "_path" -> "/content" + content.getPath();
                default -> throw new AssertionError( "Unexpected field " + field );
            };
            return switch ( compare.getOperator() )
            {
                case EQ, IN -> values.contains( actual );
                case LIKE -> actual.matches( values.get( 0 ).replace( ".", "\\." ).replace( "*", ".*" ) );
                default -> throw new AssertionError( "Unexpected operator " + compare.getOperator() );
            };
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list( final Map<String, Object> structured, final String key )
    {
        return (List<Map<String, Object>>) structured.get( key );
    }

    private static List<String> paths( final List<Map<String, Object>> items )
    {
        return items.stream().map( item -> (String) item.get( "_path" ) ).toList();
    }

    private static <T> T as( final AuthenticationInfo authInfo, final Callable<T> callable )
    {
        return ContextBuilder.copyOf( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( callable );
    }
}
