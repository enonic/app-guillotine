package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.project.Project;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.project.Projects;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ListProjectsToolTest
{
    private static final AuthenticationInfo ALICE = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).displayName( "Alice" ).build() )
        .principals( RoleKeys.AUTHENTICATED )
        .build();

    private ProjectService projectService;

    private GuillotineConfigService configService;

    private ProjectBranchResolver resolver;

    private ListProjectsTool tool;

    private final Map<String, Long> counts = new HashMap<>();

    private final List<String> countedIn = new ArrayList<>();

    private final List<String> forbidden = new ArrayList<>();

    @BeforeEach
    public void setUp()
    {
        projectService = mock( ProjectService.class );
        configService = mock( GuillotineConfigService.class );
        when( configService.getMcpDefaultProject() ).thenReturn( "" );
        when( configService.getMcpMaxResultChars() ).thenReturn( 20000 );
        resolver = mock( ProjectBranchResolver.class );

        final ContentService contentService = mock( ContentService.class );
        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> {
            final ContentQuery query = invocation.getArgument( 0 );
            assertEquals( 0, query.getSize() );
            final String repository = ContextAccessor.current().getRepositoryId().toString();
            final String where = repository + "@" + ContextAccessor.current().getBranch();
            countedIn.add( where );
            if ( forbidden.contains( where ) )
            {
                throw new ForbiddenAccessException( ContextAccessor.current().getAuthInfo().getUser() );
            }
            return FindContentIdsByQueryResult.create()
                .contents( ContentIds.empty() )
                .totalHits( counts.getOrDefault( repository.substring( "com.enonic.cms.".length() ), 0L ) )
                .build();
        } );
        tool = new ListProjectsTool( projectService, contentService, configService, resolver );
    }

    @Test
    public void testNoProjects()
    {
        when( projectService.list() ).thenReturn( Projects.empty() );

        assertEquals( Map.of( "caller", Map.of( "anonymous", true ), "defaultBranch", "master", "projects", List.of(), "note",
                              ListProjectsTool.note( List.of(), false ) ), execute() );
        assertTrue( ListProjectsTool.note( List.of(), false ).contains( "Ask the user for the project name" ) );
    }

    @Test
    public void testSingleProjectIsDefaultAndCarriesParentLanguageAndCount()
    {
        when( projectService.list() ).thenReturn(
            Projects.from( List.of( layer( "blog-no", "Blog (Norwegian)", "Company blog", Locale.forLanguageTag( "nb-NO" ), "blog" ) ) ) );
        counts.put( "blog-no", 42L );

        final Map<String, Object> structured = asAlice( this::execute );

        assertEquals( "blog-no", structured.get( "defaultProject" ) );
        assertEquals( List.of( Map.of( "name", "blog-no", "displayName", "Blog (Norwegian)", "description", "Company blog", "language",
                                       "nb-NO", "parent", "blog", "itemCount", 42 ) ), structured.get( "projects" ) );
        assertEquals( List.of( "com.enonic.cms.blog-no@draft" ), countedIn );
    }

    @Test
    public void testManyProjectsAreSortedWithoutDefaultAndCountedOnTheCallersBranch()
    {
        when( projectService.list() ).thenReturn(
            Projects.from( List.of( project( "zeta", "Zeta", null, null ), project( "alpha", "Alpha", null, null ) ) ) );
        counts.put( "zeta", 3L );

        final Map<String, Object> structured = execute();

        assertFalse( structured.containsKey( "defaultProject" ) );
        assertEquals( List.of( Map.of( "name", "alpha", "displayName", "Alpha", "itemCount", 0 ),
                               Map.of( "name", "zeta", "displayName", "Zeta", "itemCount", 3 ) ), structured.get( "projects" ) );
        assertEquals( List.of( "com.enonic.cms.alpha@master", "com.enonic.cms.zeta@master" ), countedIn );
        assertEquals( "There is no default project. Unless the user named the project, ask which of these projects to use.",
                      structured.get( "note" ) );
    }

    @Test
    public void testDefaultFromConfig()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "zeta" );
        when( projectService.list() ).thenReturn(
            Projects.from( List.of( project( "alpha", "Alpha", null, null ), project( "zeta", "Zeta", null, null ) ) ) );

        final Map<String, Object> structured = execute();
        assertEquals( "zeta", structured.get( "defaultProject" ) );
        assertNull( structured.get( "note" ) );
    }

    @Test
    public void testConfiguredDefaultIsIgnoredWhenNotReadable()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "hidden" );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "alpha", "Alpha", null, null ) ) ) );

        final Map<String, Object> structured = execute();

        assertNull( structured.get( "defaultProject" ) );
        assertEquals( 1, projects( structured ).size() );
    }

    @Test
    public void testReadableDefaultIsListedEvenWhenListOmitsIt()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "public" );
        when( projectService.list() ).thenReturn( Projects.empty() );
        when( resolver.readableProject( "public", "master" ) ).thenReturn( project( "public", "Public site", null, Locale.ENGLISH ) );
        counts.put( "public", 7L );

        final Map<String, Object> structured = execute();

        assertEquals( "public", structured.get( "defaultProject" ) );
        assertEquals( List.of( Map.of( "name", "public", "displayName", "Public site", "language", "en", "itemCount", 7 ) ),
                      structured.get( "projects" ) );
        assertTrue( ( (String) structured.get( "note" ) ).contains( "When the user means another project, pass its name as project" ),
                    structured.toString() );
    }

    @Test
    public void testPublicDefaultIsLeftOutForASignedInCallerWithoutAProjectRole()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "public" );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "alpha", "Alpha", null, null ) ) ) );
        when( resolver.readableProject( "public", "draft" ) ).thenReturn( project( "public", "Public site", null, null ) );
        forbidden.add( "com.enonic.cms.public@draft" );

        final Map<String, Object> structured = asAlice( this::execute );

        assertEquals( List.of( Map.of( "name", "alpha", "displayName", "Alpha", "itemCount", 0 ) ), structured.get( "projects" ) );
        assertFalse( structured.containsKey( "defaultProject" ) );
        assertEquals( List.of( "com.enonic.cms.alpha@draft", "com.enonic.cms.public@draft" ), countedIn );
    }

    @Test
    public void testListedDefaultIsNotReadAgain()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "alpha" );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "alpha", "Alpha", null, null ) ) ) );

        assertEquals( "alpha", execute().get( "defaultProject" ) );
        verifyNoInteractions( resolver );
    }

    @Test
    public void testMissingDisplayNameFallsBackToTheName()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "alpha", null, null, null ) ) ) );

        assertEquals( "alpha", projects( execute() ).get( 0 ).get( "displayName" ) );
    }

    @Test
    public void testCallerAndDefaultBranchAreStated()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "blog", "Blog", null, null ) ) ) );

        final Map<String, Object> signedIn = asAlice( this::execute );
        final Map<String, Object> anonymous = execute();

        assertEquals( "draft", signedIn.get( "defaultBranch" ) );
        assertEquals( Map.of( "anonymous", false, "key", "user:system:alice", "displayName", "Alice" ), signedIn.get( "caller" ) );
        assertEquals( "master", anonymous.get( "defaultBranch" ) );
        assertEquals( Map.of( "anonymous", true ), anonymous.get( "caller" ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-projects", signedIn ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-projects", anonymous ) );
    }

    @Test
    public void testManyProjectsAreTruncatedByDroppingTrailingProjects()
    {
        final List<Project> all = new ArrayList<>();
        for ( int i = 0; i < 200; i++ )
        {
            all.add( project( String.format( "project-%03d", i ), "Project " + i, "d".repeat( 300 ), Locale.ENGLISH ) );
        }
        when( projectService.list() ).thenReturn( Projects.from( all ) );

        final Map<String, Object> structured = execute();

        assertTrue( McpJson.length( structured ) <= 20000, String.valueOf( McpJson.length( structured ) ) );
        assertEquals( true, structured.get( "truncated" ) );
        assertFalse( structured.containsKey( "nextCursor" ) );
        final List<Map<String, Object>> kept = projects( structured );
        assertTrue( kept.size() > 10 && kept.size() < 200, String.valueOf( kept.size() ) );
        assertEquals( String.format( "project-%03d", kept.size() - 1 ), kept.get( kept.size() - 1 ).get( "name" ) );
        assertEquals( "Result truncated to fit 20000 characters: dropped " + ( 200 - kept.size() ) + " trailing item(s) of " +
                          "'projects'. Pass a project name directly to the tools that need it.", structured.get( "hint" ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-projects", structured ) );
    }

    @Test
    public void testLongCallerNameIsShortenedSoTheCallerAlwaysFits()
    {
        when( configService.getMcpMaxResultChars() ).thenReturn( 1000 );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "blog", "Blog", "d".repeat( 2000 ), null ) ) ) );
        final AuthenticationInfo longName = AuthenticationInfo.create()
            .user( User.create()
                       .key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) )
                       .login( "alice" )
                       .displayName( "A".repeat( 5000 ) )
                       .build() )
            .build();

        final Map<String, Object> structured = ContextBuilder.create().authInfo( longName ).build().callWith( this::execute );

        assertTrue( McpJson.length( structured ) <= 1000, String.valueOf( McpJson.length( structured ) ) );
        assertEquals( List.of(), structured.get( "projects" ) );
        assertEquals( "draft", structured.get( "defaultBranch" ) );
        final Map<?, ?> caller = (Map<?, ?>) structured.get( "caller" );
        assertEquals( "user:system:alice", caller.get( "key" ) );
        assertEquals( McpJson.PREVIEW_CHARS - 2, ( (String) caller.get( "displayName" ) ).length() );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-projects", structured ) );
    }

    @Test
    public void testDisplayNameIsBoundedAsJsonEscapesIncluded()
    {
        final String control = ListProjectsTool.displayName( "\u0001".repeat( 500 ) );

        assertTrue( McpJson.length( control ) <= McpJson.PREVIEW_CHARS, String.valueOf( McpJson.length( control ) ) );
        assertTrue( control.endsWith( "…" ) && control.length() > 10, control );
        assertEquals( "Alice", ListProjectsTool.displayName( "Alice" ) );
        assertNull( ListProjectsTool.displayName( null ) );
    }

    @Test
    public void testReadableDefaultLayerKeepsItsParent()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "public-no" );
        when( projectService.list() ).thenReturn( Projects.empty() );
        when( resolver.readableProject( "public-no", "master" ) ).thenReturn( layer( "public-no", "Public (NO)", null, null, "public" ) );

        assertEquals( "public", projects( execute() ).get( 0 ).get( "parent" ) );
    }

    @Test
    public void testOutputSchemaRejectsWhatTheToolNeverReturns()
    {
        final List<String> errors = ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-projects",
                                                                       Map.of( "projects", List.of( Map.of( "name", "x" ) ), "extra",
                                                                               1 ) );

        for ( final String expected : List.of( "displayName", "itemCount", "extra" ) )
        {
            assertTrue( errors.stream().anyMatch( error -> error.contains( expected ) ), () -> expected + " in " + errors );
        }
    }

    private Map<String, Object> execute()
    {
        return structured( tool.execute( Map.of() ) );
    }

    private static <T> T asAlice( final Callable<T> callable )
    {
        return ContextBuilder.create().authInfo( ALICE ).build().callWith( callable );
    }

    private static Project project( final String name, final String displayName, final String description, final Locale language )
    {
        return Project.create()
            .name( ProjectName.from( name ) )
            .displayName( displayName )
            .description( description )
            .language( language )
            .build();
    }

    private static Project layer( final String name, final String displayName, final String description, final Locale language,
                                  final String parent )
    {
        return Project.create()
            .name( ProjectName.from( name ) )
            .displayName( displayName )
            .description( description )
            .language( language )
            .parent( ProjectName.from( parent ) )
            .build();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> projects( final Map<String, Object> structured )
    {
        return (List<Map<String, Object>>) structured.get( "projects" );
    }
}
