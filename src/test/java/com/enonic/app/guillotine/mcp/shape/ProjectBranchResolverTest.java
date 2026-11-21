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

    private GuillotineConfigService configService;

    private ProjectBranchResolver resolver;

    private final Map<String, Node> readableRoots = new HashMap<>();

    private final AtomicReference<Context> lastReadContext = new AtomicReference<>();

    @BeforeEach
    public void setUp()
    {
        projectService = mock( ProjectService.class );
        final NodeService nodeService = mock( NodeService.class );
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
    public void testAnonymousDefaultsToMasterAndReadsAsAnonymous()
    {
        readable( "public", "master" );

        final ProjectBranch resolved = anonymous( () -> resolver.resolve( Map.of( "project", "public" ) ) );

        assertEquals( new ProjectBranch( "public", "master" ), resolved );
        assertEquals( "com.enonic.cms.public", lastReadContext.get().getRepositoryId().toString() );
        assertFalse( lastReadContext.get().getAuthInfo().isAuthenticated() );
        verify( projectService, never() ).list();
    }

    @Test
    public void testSignedInDefaultsToDraft()
    {
        readable( "blog", "draft" );

        assertEquals( new ProjectBranch( "blog", "draft" ), asEditor( () -> resolver.resolve( Map.of( "project", "blog" ) ) ) );
        assertSame( EDITOR, lastReadContext.get().getAuthInfo() );
        assertEquals( "draft", asEditor( ProjectBranchResolver::defaultBranch ) );
        assertEquals( "master", anonymous( ProjectBranchResolver::defaultBranch ) );
    }

    @Test
    public void testBranchArgumentWins()
    {
        readable( "blog", "draft" );
        readable( "blog", "master" );

        assertEquals( "draft", anonymous( () -> resolver.resolve( Map.of( "project", "blog", "branch", "draft" ) ) ).branch() );
        assertEquals( "master", asEditor( () -> resolver.resolve( Map.of( "project", "blog", "branch", "master" ) ) ).branch() );
    }

    @Test
    public void testUnreadableProjectIsToolError()
    {
        readable( "blog", "draft" );

        for ( final String project : List.of( "blog", "missing", "nobranch", "Not A Valid Name!" ) )
        {
            final ToolError error =
                assertThrows( ToolError.class, () -> anonymous( () -> resolver.resolve( Map.of( "project", project ) ) ) );
            assertEquals( "Project '" + project + "' not found or not readable on master. Call list-projects to see available projects.",
                          error.userMessage() );
        }
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
    public void testUnreadableConfiguredDefaultIsToolError()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "secret" );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "other" ) ) ) );

        final ToolError error = assertThrows( ToolError.class, () -> anonymous( () -> resolver.resolve( Map.of() ) ) );

        assertEquals( "The default project 'secret' (mcp.defaultProject) is not readable on master. Pass the project argument; " +
                          "call list-projects to see available projects.", error.userMessage() );
    }

    @Test
    public void testOnlyListedProjectIsUsed()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "only" ) ) ) );
        readable( "only", "draft" );

        assertEquals( new ProjectBranch( "only", "draft" ), asEditor( () -> resolver.resolve( Map.of() ) ) );
    }

    @Test
    public void testOnlyListedProjectUnreadableOnBranchIsToolError()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "only" ) ) ) );
        readable( "only", "draft" );

        final ToolError error =
            assertThrows( ToolError.class, () -> asEditor( () -> resolver.resolve( Map.of( "branch", "master" ) ) ) );

        assertEquals( "Project 'only' not found or not readable on master. Call list-projects to see available projects.",
                      error.userMessage() );
    }

    @Test
    public void testSeveralOrNoProjectsWithoutDefaultIsToolError()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "a" ), project( "b" ) ) ) );
        final ToolError several = assertThrows( ToolError.class, () -> asEditor( () -> resolver.resolve( Map.of() ) ) );
        assertEquals( "Project is required: there is no default project, so pass the project argument. Call list-projects and " +
                          "pass one of its project names as project.", several.userMessage() );

        when( projectService.list() ).thenReturn( Projects.empty() );
        assertThrows( ToolError.class, () -> anonymous( () -> resolver.resolve( Map.of() ) ) );
    }

    @Test
    public void testCallWithKeepsCallerAndSetsRepositoryAndBranch()
    {
        final Context context = asEditor( () -> new ProjectBranch( "blog", "master" ).callWith( ContextAccessor::current ) );

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
        data.getSet( "data" ).addStrings( "parents", "base" );
        readableRoots.put( "public/master", contentRoot( data ) );
        readable( "base", "master" );

        final Project project = anonymous( () -> resolver.readableProject( "public", "master" ) );

        assertEquals( "public", project.getName().toString() );
        assertEquals( "Public site", project.getDisplayName() );
        assertEquals( "Open content", project.getDescription() );
        assertEquals( Locale.forLanguageTag( "nb-NO" ), project.getLanguage() );
        assertEquals( "base", project.getParent().toString() );
        assertNull( anonymous( () -> resolver.readableProject( "base", "master" ) ).getParent() );
        assertNull( resolver.readableProject( null, "master" ) );
    }

    @Test
    public void testProjectBranchIsEchoed()
    {
        final Map<String, Object> structured = new HashMap<>();

        new ProjectBranch( "blog", "draft" ).putInto( structured );

        assertEquals( Map.of( "project", "blog", "branch", "draft" ), structured );
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
