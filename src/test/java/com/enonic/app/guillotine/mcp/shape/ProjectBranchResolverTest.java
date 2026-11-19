package com.enonic.app.guillotine.mcp.shape;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.Project;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.project.Projects;
import com.enonic.xp.repository.BranchNotFoundException;
import com.enonic.xp.repository.RepositoryNotFoundException;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectBranchResolverTest
{
    private static final AuthenticationInfo EDITOR = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "editor" ) ).login( "editor" ).build() )
        .build();

    private ProjectService projectService;

    private NodeService nodeService;

    private GuillotineConfigService configService;

    private ProjectBranchResolver resolver;

    private final Map<String, Node> readableRoots = new HashMap<>();

    private final AtomicReference<Context> lastReadContext = new AtomicReference<>();

    @BeforeEach
    public void setUp()
    {
        projectService = mock( ProjectService.class );
        nodeService = mock( NodeService.class );
        configService = mock( GuillotineConfigService.class );
        when( configService.getMcpDefaultProject() ).thenReturn( "" );
        when( projectService.list() ).thenReturn( Projects.empty() );
        when( nodeService.getByPath( ContentConstants.CONTENT_ROOT_PATH ) ).thenAnswer( invocation -> {
            final Context context = ContextAccessor.current();
            lastReadContext.set( context );
            final String repo = context.getRepositoryId().toString();
            if ( repo.endsWith( "missing" ) )
            {
                throw new RepositoryNotFoundException( context.getRepositoryId() );
            }
            if ( repo.endsWith( "nobranch" ) )
            {
                throw new BranchNotFoundException( context.getBranch() );
            }
            return readableRoots.get( ProjectName.from( context.getRepositoryId() ) + "/" + context.getBranch() );
        } );
        resolver = new ProjectBranchResolver( projectService, nodeService, configService );
    }

    @Test
    public void testExplicitProjectReadableByAnonymousIsResolvedOnMaster()
    {
        readable( "public", "master" );

        final ProjectBranch resolved = anonymous( () -> resolver.resolve( Map.of( "project", "public" ) ) );

        assertEquals( new ProjectBranch( "public", "master" ), resolved );
        assertEquals( "com.enonic.cms.public", lastReadContext.get().getRepositoryId().toString() );
        assertFalse( lastReadContext.get().getAuthInfo().isAuthenticated() );
        verify( projectService, never() ).list();
    }

    @Test
    public void testAuthenticatedDefaultsToDraft()
    {
        readable( "blog", "draft" );

        final ProjectBranch resolved = asEditor( () -> resolver.resolve( Map.of( "project", "blog" ) ) );

        assertEquals( new ProjectBranch( "blog", "draft" ), resolved );
        assertSame( EDITOR, lastReadContext.get().getAuthInfo() );
    }

    @Test
    public void testExplicitBranchWins()
    {
        readable( "blog", "draft" );
        readable( "blog", "master" );

        assertEquals( "draft", anonymous( () -> resolver.resolve( Map.of( "project", "blog", "branch", "draft" ) ) ).branch() );
        assertEquals( "master", asEditor( () -> resolver.resolve( Map.of( "project", "blog", "branch", "master" ) ) ).branch() );
    }

    @Test
    public void testInvalidBranchIsToolError()
    {
        final ToolError error = assertThrows( ToolError.class, () -> resolver.resolve( Map.of( "project", "blog", "branch", "live" ) ) );

        assertEquals( "Invalid branch: \"live\". Use \"draft\" (latest changes) or \"master\" (published), or omit branch.",
                      error.userMessage() );
    }

    @Test
    public void testExplicitProjectUnreadableIsToolError()
    {
        readable( "blog", "draft" );

        for ( final String project : List.of( "blog", "missing", "nobranch", "Not A Valid Name!" ) )
        {
            final ToolError error =
                assertThrows( ToolError.class, () -> anonymous( () -> resolver.resolve( Map.of( "project", project ) ) ) );
            assertEquals( "Project '" + project + "' not found or not readable. Call list-projects to see available projects.",
                          error.userMessage() );
        }
    }

    @Test
    public void testNonStringProjectIsToolError()
    {
        final ToolError error = assertThrows( ToolError.class, () -> resolver.resolve( Map.of( "project", 5 ) ) );

        assertTrue( error.userMessage().startsWith( "Invalid project: 5." ), error.userMessage() );
    }

    @Test
    public void testConfiguredDefaultIsUsedWithoutBeingListed()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "public" );
        readable( "public", "master" );

        assertEquals( new ProjectBranch( "public", "master" ), anonymous( () -> resolver.resolve( Map.of() ) ) );
        assertEquals( new ProjectBranch( "public", "master" ), anonymous( () -> resolver.resolve( Map.of( "project", "" ) ) ) );
    }

    @Test
    public void testConfiguredDefaultUnreadableIsToolError()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "secret" );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "other" ) ) ) );

        final ToolError error = assertThrows( ToolError.class, () -> anonymous( () -> resolver.resolve( Map.of() ) ) );

        assertEquals( "Project 'secret' not found or not readable. Call list-projects to see available projects.", error.userMessage() );
    }

    @Test
    public void testSingleListedProjectIsUsed()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "only" ) ) ) );

        assertEquals( new ProjectBranch( "only", "draft" ), asEditor( () -> resolver.resolve( Map.of() ) ) );
    }

    @Test
    public void testSeveralOrNoProjectsWithoutDefaultIsToolError()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "a" ), project( "b" ) ) ) );
        final ToolError several = assertThrows( ToolError.class, () -> asEditor( () -> resolver.resolve( Map.of() ) ) );
        assertEquals( "Project is required: pass the project argument. Call list-projects to see project names, or ask the " +
                          "administrator to set mcp.defaultProject in com.enonic.app.guillotine.cfg.", several.userMessage() );

        when( projectService.list() ).thenReturn( Projects.empty() );
        assertThrows( ToolError.class, () -> anonymous( () -> resolver.resolve( Map.of() ) ) );
    }

    @Test
    public void testMineRequiresAuthenticatedUser()
    {
        readable( "blog", "master" );
        readable( "blog", "draft" );

        final ToolError error =
            assertThrows( ToolError.class, () -> anonymous( () -> resolver.resolve( Map.of( "project", "blog", "mine", true ) ) ) );
        assertEquals( "mine requires an authenticated user; the current caller is anonymous. " +
                          "Remove mine, or call the tool as a signed-in user.", error.userMessage() );

        assertEquals( "blog", anonymous( () -> resolver.resolve( Map.of( "project", "blog", "mine", false ) ) ).project() );
        assertEquals( "blog", asEditor( () -> resolver.resolve( Map.of( "project", "blog", "mine", true ) ) ).project() );
    }

    @Test
    public void testRunInProjectContextKeepsCallerAndSetsRepoAndBranch()
    {
        final Context context = asEditor( () -> ProjectBranchResolver.runInProjectContext( new ProjectBranch( "blog", "master" ),
                                                                                           ContextAccessor::current ) );

        assertEquals( "com.enonic.cms.blog", context.getRepositoryId().toString() );
        assertEquals( "master", context.getBranch().toString() );
        assertSame( EDITOR, context.getAuthInfo() );
        assertNull( ContextAccessor.current().getRepositoryId() );
    }

    @Test
    public void testReadableProjectIsBuiltFromContentRoot()
    {
        final PropertyTree data = new PropertyTree();
        data.setString( "displayName", "Public site" );
        data.setString( "language", "nb-NO" );
        data.addSet( "data" ).setString( "description", "Open content" );
        readableRoots.put( "public/master", contentRoot( data ) );

        final Project project = anonymous( () -> resolver.readableProject( "public", "master" ) );

        assertEquals( "public", project.getName().toString() );
        assertEquals( "Public site", project.getDisplayName() );
        assertEquals( "Open content", project.getDescription() );
        assertEquals( Locale.forLanguageTag( "nb-NO" ), project.getLanguage() );

        readable( "bare", "master" );
        final Project bare = anonymous( () -> resolver.readableProject( "bare", "master" ) );
        assertNull( bare.getDescription() );
        assertNull( bare.getLanguage() );
        assertNull( resolver.readableProject( null, "master" ) );
    }

    @Test
    public void testProjectBranchPutsProjectAndBranch()
    {
        final Map<String, Object> structured = new HashMap<>();

        new ProjectBranch( "blog", "draft" ).putInto( structured );

        assertEquals( Map.of( "project", "blog", "branch", "draft" ), structured );
        assertEquals( "com.enonic.cms.blog", new ProjectBranch( "blog", "draft" ).repositoryId().toString() );
    }

    private void readable( final String project, final String branch )
    {
        readableRoots.put( project + "/" + branch, contentRoot( new PropertyTree() ) );
    }

    private static Node contentRoot( final PropertyTree data )
    {
        return Node.create().name( "content" ).parentPath( NodePath.ROOT ).data( data ).build();
    }

    private static Project project( final String name )
    {
        return Project.create().name( ProjectName.from( name ) ).build();
    }

    private static <T> T anonymous( final Callable<T> callable )
    {
        return ContextBuilder.create().authInfo( AuthenticationInfo.unAuthenticated() ).build().callWith( callable );
    }

    private static <T> T asEditor( final Callable<T> callable )
    {
        return ContextBuilder.create().authInfo( EDITOR ).build().callWith( callable );
    }
}
