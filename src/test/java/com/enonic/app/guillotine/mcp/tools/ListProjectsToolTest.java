package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.xp.context.ContextBuilder;
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
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ListProjectsToolTest
{
    private ProjectService projectService;

    private GuillotineConfigService configService;

    private ProjectBranchResolver resolver;

    private ListProjectsTool tool;

    @BeforeEach
    public void setUp()
    {
        projectService = mock( ProjectService.class );
        configService = mock( GuillotineConfigService.class );
        when( configService.getMcpDefaultProject() ).thenReturn( "" );
        when( configService.getMcpMaxResultChars() ).thenReturn( 20000 );
        resolver = mock( ProjectBranchResolver.class );
        tool = new ListProjectsTool( projectService, configService, resolver );
    }

    @Test
    public void testNoProjects()
    {
        when( projectService.list() ).thenReturn( Projects.empty() );

        final Map<String, Object> result = tool.execute( Map.of() );

        assertEquals( Map.of( "projects", List.of(), "defaultBranch", "master", "caller", Map.of( "anonymous", true ) ),
                      structured( result ) );
        assertEquals( "No projects are visible to the current user.", summaryLine( result ) );
    }

    @Test
    public void testSingleProjectIsDefault()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "blog", "Blog", "Company blog", Locale.ENGLISH ) ) ) );

        final Map<String, Object> structured = structured( tool.execute( Map.of() ) );

        assertEquals( "blog", structured.get( "defaultProject" ) );
        assertEquals(
            List.of( Map.of( "name", "blog", "displayName", "Blog", "description", "Company blog", "language", "en", "isDefault", true ) ),
            structured.get( "projects" ) );
    }

    @Test
    public void testManyProjectsWithoutDefault()
    {
        when( projectService.list() ).thenReturn(
            Projects.from( List.of( project( "zeta", "Zeta", null, null ), project( "alpha", "Alpha", null, null ) ) ) );

        final Map<String, Object> result = tool.execute( Map.of() );
        final Map<String, Object> structured = structured( result );

        assertFalse( structured.containsKey( "defaultProject" ) );
        assertEquals( List.of( Map.of( "name", "alpha", "displayName", "Alpha", "isDefault", false ),
                               Map.of( "name", "zeta", "displayName", "Zeta", "isDefault", false ) ), structured.get( "projects" ) );
        assertEquals( "2 projects visible; no default project.", summaryLine( result ) );
    }

    @Test
    public void testDefaultFromConfig()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "zeta" );
        when( projectService.list() ).thenReturn(
            Projects.from( List.of( project( "alpha", "Alpha", null, null ), project( "zeta", "Zeta", null, null ) ) ) );

        final Map<String, Object> result = tool.execute( Map.of() );
        final Map<String, Object> structured = structured( result );

        assertEquals( "zeta", structured.get( "defaultProject" ) );
        assertEquals( List.of( false, true ), projects( structured ).stream().map( item -> item.get( "isDefault" ) ).toList() );
        assertEquals( "2 projects visible; default project: zeta.", summaryLine( result ) );
    }

    @Test
    public void testConfiguredDefaultIsIgnoredWhenNotVisible()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "hidden" );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "alpha", "Alpha", null, null ) ) ) );

        final Map<String, Object> structured = structured( tool.execute( Map.of() ) );

        assertNull( structured.get( "defaultProject" ) );
        assertEquals( false, projects( structured ).get( 0 ).get( "isDefault" ) );
    }

    @Test
    public void testReadableDefaultIsListedEvenWhenListOmitsIt()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "public" );
        when( projectService.list() ).thenReturn( Projects.empty() );
        when( resolver.readableProject( "public", "master" ) ).thenReturn( project( "public", "Public site", null, Locale.ENGLISH ) );

        final Map<String, Object> result = tool.execute( Map.of() );
        final Map<String, Object> structured = structured( result );

        assertEquals( "public", structured.get( "defaultProject" ) );
        assertEquals( List.of( Map.of( "name", "public", "displayName", "Public site", "language", "en", "isDefault", true ) ),
                      structured.get( "projects" ) );
        assertEquals( "1 project visible; default project: public.", summaryLine( result ) );
    }

    @Test
    public void testListedDefaultIsNotReadAgain()
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "alpha" );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "alpha", "Alpha", null, null ) ) ) );

        assertEquals( "alpha", structured( tool.execute( Map.of() ) ).get( "defaultProject" ) );
        verifyNoInteractions( resolver );
    }

    @Test
    public void testNoNullFieldsInStructuredContent()
    {
        when( projectService.list() ).thenReturn(
            Projects.from( List.of( project( "alpha", null, null, null ), project( "beta", "Beta", null, null ) ) ) );

        final Map<String, Object> structured = structured( tool.execute( Map.of() ) );

        assertFalse( structured.containsKey( "defaultProject" ) );
        projects( structured ).forEach( item -> assertFalse( item.containsValue( null ), item.toString() ) );
        assertEquals( "alpha", projects( structured ).get( 0 ).get( "displayName" ) );
    }

    @Test
    public void testTextContentIsJsonOfStructuredContent()
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "blog", "Blog", null, Locale.forLanguageTag( "nb-NO" ) ) ) ) );

        final Map<String, Object> result = tool.execute( Map.of() );
        final String text = text( result );

        assertEquals( "1 project visible; default project: blog.", summaryLine( result ) );
        assertEquals( McpJson.write( structured( result ) ), text.substring( text.indexOf( '\n' ) + 1 ) );
        assertEquals( structured( result ), McpJson.readObject( text.substring( text.indexOf( '\n' ) + 1 ) ) );
    }

    @Test
    public void testManyProjectsAreTruncatedByDroppingTrailingProjects()
        throws Exception
    {
        final List<Project> all = new ArrayList<>();
        for ( int i = 0; i < 200; i++ )
        {
            all.add( project( String.format( "project-%03d", i ), "Project " + i, "d".repeat( 300 ), Locale.ENGLISH ) );
        }
        when( projectService.list() ).thenReturn( Projects.from( all ) );

        final Map<String, Object> result = tool.execute( Map.of() );
        final Map<String, Object> structured = structured( result );

        assertTrue( McpJson.length( structured ) <= 20000, String.valueOf( McpJson.length( structured ) ) );
        assertEquals( result, McpJson.readObject( McpJson.write( result ) ) );
        assertEquals( true, structured.get( "truncated" ) );
        final List<Map<String, Object>> kept = projects( structured );
        assertTrue( kept.size() > 10 && kept.size() < 200, String.valueOf( kept.size() ) );
        assertEquals( "project-000", kept.get( 0 ).get( "name" ) );
        assertEquals( String.format( "project-%03d", kept.size() - 1 ), kept.get( kept.size() - 1 ).get( "name" ) );
        assertTrue( ( (String) structured.get( "hint" ) ).contains( "trailing item(s) of 'projects'" ), (String) structured.get( "hint" ) );
        assertEquals( "200 projects visible (" + kept.size() + " listed here); no default project.", summaryLine( result ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-projects", structured ) );
    }

    @Test
    public void testCallerAndDefaultBranchAreStated()
        throws Exception
    {
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "blog", "Blog", null, null ) ) ) );
        final AuthenticationInfo alice = AuthenticationInfo.create()
            .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).displayName( "Alice" )
                       .build() )
            .principals( RoleKeys.AUTHENTICATED )
            .build();

        final Map<String, Object> signedIn =
            structured( ContextBuilder.create().authInfo( alice ).build().callWith( () -> tool.execute( Map.of() ) ) );
        final Map<String, Object> anonymous = structured(
            ContextBuilder.create().authInfo( AuthenticationInfo.unAuthenticated() ).build().callWith( () -> tool.execute( Map.of() ) ) );

        assertEquals( "draft", signedIn.get( "defaultBranch" ) );
        assertEquals( Map.of( "anonymous", false, "key", "user:system:alice", "displayName", "Alice" ), signedIn.get( "caller" ) );
        assertEquals( "master", anonymous.get( "defaultBranch" ) );
        assertEquals( Map.of( "anonymous", true ), anonymous.get( "caller" ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-projects", signedIn ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-projects", anonymous ) );
    }

    @Test
    public void testCallerThatCannotFitKeepsTheRequiredFields()
    {
        when( configService.getMcpMaxResultChars() ).thenReturn( 1000 );
        when( projectService.list() ).thenReturn( Projects.from( List.of( project( "blog", "Blog", null, null ) ) ) );
        final AuthenticationInfo longName = AuthenticationInfo.create()
            .user( User.create()
                       .key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) )
                       .login( "alice" )
                       .displayName( "A".repeat( 5000 ) )
                       .build() )
            .principals( RoleKeys.AUTHENTICATED )
            .build();

        final Map<String, Object> result =
            ContextBuilder.create().authInfo( longName ).build().callWith( () -> tool.execute( Map.of() ) );
        final Map<String, Object> structured = structured( result );

        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-projects", structured ) );
        assertTrue( McpJson.length( structured ) <= 1000, String.valueOf( McpJson.length( structured ) ) );
        assertEquals( List.of(), structured.get( "projects" ) );
        assertEquals( "draft", structured.get( "defaultBranch" ) );
        assertEquals( "user:system:alice", ( (Map<?, ?>) structured.get( "caller" ) ).get( "key" ) );
        assertEquals( true, structured.get( "truncated" ) );
        assertTrue( ( (String) structured.get( "hint" ) ).startsWith( "Result does not fit 1000 characters." ),
                    (String) structured.get( "hint" ) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDescriptorArgumentsAreConsistentAcrossTools()
        throws Exception
    {
        final List<Map<String, Object>> tools = ToolDescriptor.tools();
        final java.util.Set<Object> branchDescriptions = new java.util.HashSet<>();
        final java.util.Set<Object> projectDescriptions = new java.util.HashSet<>();
        for ( final Map<String, Object> tool : tools )
        {
            final Map<String, Object> properties =
                (Map<String, Object>) ( (Map<String, Object>) tool.get( "inputSchema" ) ).get( "properties" );
            if ( properties.get( "types" ) instanceof Map<?, ?> types )
            {
                assertEquals( "20", String.valueOf( types.get( "maxItems" ) ), String.valueOf( tool.get( "name" ) ) );
            }
            if ( properties.get( "branch" ) instanceof Map<?, ?> branch )
            {
                branchDescriptions.add( branch.get( "description" ) );
            }
            if ( properties.get( "project" ) instanceof Map<?, ?> project )
            {
                projectDescriptions.add( project.get( "description" ) );
            }
            if ( "describe-type".equals( tool.get( "name" ) ) )
            {
                assertFalse( properties.containsKey( "project" ) || properties.containsKey( "branch" ) );
            }
        }
        assertEquals( 1, branchDescriptions.size(), branchDescriptions::toString );
        assertEquals( 1, projectDescriptions.size(), projectDescriptions::toString );
        assertFalse( branchDescriptions.contains( null ) );
    }

    @Test
    public void testResultsValidateAgainstDeclaredOutputSchema()
        throws Exception
    {
        when( configService.getMcpDefaultProject() ).thenReturn( "alpha" );
        when( projectService.list() ).thenReturn(
            Projects.from( List.of( project( "alpha", null, "About", Locale.ENGLISH ), project( "beta", "Beta", null, null ) ) ) );

        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-projects", structured( tool.execute( Map.of() ) ) ) );
        final List<String> errors = ToolDescriptor.outputSchemaErrors( "list-projects", Map.of( "projects", List.of( "x" ), "extra", 1 ) );
        assertEquals( 4, errors.size(), errors::toString );
        for ( final String expected : List.of( "/projects/0", "caller", "defaultBranch", "extra" ) )
        {
            assertTrue( errors.stream().anyMatch( error -> error.contains( expected ) ), () -> expected + " in " + errors );
        }
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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> projects( final Map<String, Object> structured )
    {
        return (List<Map<String, Object>>) structured.get( "projects" );
    }

    private static String summaryLine( final Map<String, Object> result )
    {
        final String text = text( result );
        return text.substring( 0, text.indexOf( '\n' ) );
    }
}
