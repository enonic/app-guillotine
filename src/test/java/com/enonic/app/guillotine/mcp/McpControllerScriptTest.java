package com.enonic.app.guillotine.mcp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfig;
import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.Project;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.project.Projects;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.testing.ScriptTestSupport;

import static com.enonic.app.guillotine.mcp.McpResults.read;
import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static com.enonic.app.guillotine.mcp.McpResults.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Calls the group controllers the way XP's MCP endpoint does: {@code execute(name, args, invocation)} with {@code args} a plain script
 * object, and reads the result with {@code getMap()}. Runs on the default engine (Nashorn); a subclass repeats it on GraalJS.
 */
public class McpControllerScriptTest
    extends ScriptTestSupport
{
    private static final String CONTENT_CONTROLLER = "/mcp/content/content.js";

    private static final AuthenticationInfo ALICE = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).displayName( "Alice" ).build() )
        .principals( RoleKeys.AUTHENTICATED )
        .build();

    private GuillotineConfigService configService;

    private ServiceFacade serviceFacade;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        configService = spy( new GuillotineConfigService() );
        configService.activate( mock( GuillotineConfig.class, invocation -> invocation.getMethod().getDefaultValue() ) );

        serviceFacade = mock( ServiceFacade.class );
        when( serviceFacade.getProjectService() ).thenReturn( projectService );
        when( serviceFacade.getContentService() ).thenReturn( contentService );
        when( serviceFacade.getNodeService() ).thenReturn( mock( NodeService.class ) );

        when( projectService.list() ).thenReturn( Projects.from(
            List.of( Project.create().name( ProjectName.from( "blog" ) ).displayName( "Blog" ).parent( ProjectName.from( "base" ) ).build() ) ) );
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.empty() ).totalHits( 12 ).build() );

        addService( ServiceFacade.class, serviceFacade );
        addService( GuillotineConfigService.class, configService );
    }

    @Test
    public void testListProjectsThroughTheControllerIsTheToolResult()
    {
        final Map<String, Object> result = asAlice( () -> execute( CONTENT_CONTROLLER, "list-projects", Map.of() ) );

        final Map<String, Object> structured = structured( result );
        assertEquals( Map.of( "caller", Map.of( "anonymous", false, "key", "user:system:alice", "displayName", "Alice" ),
                              "defaultProject", "blog", "defaultBranch", "draft", "projects",
                              List.of( Map.of( "name", "blog", "displayName", "Blog", "parent", "base", "itemCount", 12 ) ) ),
                      structured );
        assertEquals( normalized( asAlice( () -> new McpTools( serviceFacade, configService ).run( "content", "list-projects", "{}" ) ) ),
                      normalized( result ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-projects", structured ) );
    }

    @Test
    public void testAnonymousCallerGetsTheRefusalAsAnErrorResultUntilTheOperatorOptsIn()
    {
        final Map<String, Object> refused = execute( CONTENT_CONTROLLER, "list-projects", Map.of() );

        assertEquals( true, refused.get( "isError" ) );
        assertTrue( text( refused ).contains( "mcp.allowAnonymous=true" ), () -> text( refused ) );

        doReturn( true ).when( configService ).isMcpAllowAnonymous();

        final Map<String, Object> structured = structured( execute( CONTENT_CONTROLLER, "list-projects", Map.of() ) );
        assertEquals( Map.of( "anonymous", true ), structured.get( "caller" ) );
        assertEquals( "master", structured.get( "defaultBranch" ) );
    }

    @Test
    public void testDisabledToolsAnswerWithAnErrorResult()
    {
        doReturn( false ).when( configService ).isMcpEnabled();

        final Map<String, Object> result = asAlice( () -> execute( CONTENT_CONTROLLER, "list-projects", Map.of() ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( text( result ).startsWith( "Guillotine MCP tools are disabled by configuration." ), () -> text( result ) );
    }

    @Test
    public void testArgsArriveAsAPlainObjectThatStringifiesToTheirJson()
    {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put( "n", 3 );
        nested.put( "none", null );
        final Map<String, Object> args = new LinkedHashMap<>();
        args.put( "text", "news" );
        args.put( "rating", 5.5 );
        args.put( "types", Arrays.asList( "article", null ) );
        args.put( "variables", nested );

        final Map<String, Object> result = execute( "/mcp/probe/stringify.js", "search", args );

        assertEquals( read( write( args ) ), read( (String) result.get( "json" ) ) );
    }

    private Map<String, Object> execute( final String controller, final String name, final Map<String, ?> args )
    {
        final ScriptValue result = runScript( controller ).executeMethod( "execute", name, new McpArguments( args ), new Object() );
        assertTrue( result != null && result.isObject() && !result.isArray() );
        return result.getMap();
    }

    private static Map<String, Object> normalized( final Object value )
    {
        return read( write( value ) );
    }

    private static <T> T asAlice( final Callable<T> callable )
    {
        return ContextBuilder.create().authInfo( ALICE ).build().callWith( callable );
    }
}
