package com.enonic.app.guillotine.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class McpToolsTest
{
    private static final AuthenticationInfo EDITOR = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "editor" ) ).login( "editor" ).build() )
        .build();

    private static final String ANONYMOUS_REFUSAL = "This tool needs a signed-in caller, and this call is anonymous. Do not retry; " +
        "ask the user to sign in to Enonic XP, or ask the administrator to set mcp.allowAnonymous=true in " +
        "com.enonic.app.guillotine.cfg to let anonymous callers read published content.";

    private GuillotineConfigService configService;

    private final List<Map<String, Object>> calls = new ArrayList<>();

    private McpTools tools;

    @BeforeEach
    public void setUp()
    {
        configService = mock( GuillotineConfigService.class );
        when( configService.isMcpEnabled() ).thenReturn( true );
        tools = new McpTools( configService, List.of( new EchoTool( McpGroup.CONTENT, "echo" ), new EchoTool( McpGroup.GRAPHQL, "echo" ),
                                                      new EchoTool( McpGroup.REFERENCES, "scan" ), new FailingTool() ) );
    }

    @Test
    public void testDispatchesToTheToolOfTheGroupWithParsedArgs()
    {
        final Map<String, Object> result = asEditor( () -> tools.run( "graphql", "echo", "{\"query\":\"news\",\"limit\":2}" ) );

        assertEquals( Map.of( "group", "graphql", "args", Map.of( "query", "news", "limit", 2 ) ), structured( result ) );
        assertEquals( 1, calls.size() );
    }

    @Test
    public void testUnknownToolOrGroupIsAnErrorWithoutCallingAnything()
    {
        assertEquals( "Unknown Guillotine MCP tool: content/drop.", errorText( asEditor( () -> tools.run( "content", "drop", "{}" ) ) ) );
        assertEquals( "Unknown Guillotine MCP tool: content/scan.", errorText( asEditor( () -> tools.run( "content", "scan", "{}" ) ) ) );
        assertEquals( "Unknown Guillotine MCP tool: admin/echo.", errorText( asEditor( () -> tools.run( "admin", "echo", "{}" ) ) ) );
        assertTrue( calls.isEmpty() );
    }

    @Test
    public void testDisabledByConfigBeforeAnythingElse()
    {
        when( configService.isMcpEnabled() ).thenReturn( false );

        final String expected = "Guillotine MCP tools are disabled by configuration. Do not retry; ask the administrator to set " +
            "mcp.enabled=true in com.enonic.app.guillotine.cfg.";
        assertEquals( expected, errorText( tools.run( "content", "echo", "{}" ) ) );
        assertEquals( expected, errorText( tools.run( "content", "drop", "{}" ) ) );
        assertTrue( calls.isEmpty() );
    }

    @Test
    public void testAnonymousCallerIsRefusedUnlessTheOperatorOptsIn()
    {
        assertEquals( ANONYMOUS_REFUSAL, errorText( tools.run( "content", "echo", "{}" ) ) );
        assertEquals( ANONYMOUS_REFUSAL, errorText( tools.run( "graphql", "echo", "{}" ) ) );
        assertTrue( calls.isEmpty() );

        when( configService.isMcpAllowAnonymous() ).thenReturn( true );

        assertEquals( "content", structured( tools.run( "content", "echo", "{}" ) ).get( "group" ) );
        assertEquals( "graphql", structured( tools.run( "graphql", "echo", "{}" ) ).get( "group" ) );
    }

    @Test
    public void testReferencesAreNeverOpenToAnonymousCallers()
    {
        when( configService.isMcpAllowAnonymous() ).thenReturn( true );

        assertEquals( "This tool needs a signed-in caller, and this call is anonymous. Do not retry; ask the user to sign in to " +
                          "Enonic XP. Anonymous callers cannot use the references tools.",
                      errorText( tools.run( "references", "scan", "{}" ) ) );
        assertTrue( calls.isEmpty() );
        assertEquals( "references", structured( asEditor( () -> tools.run( "references", "scan", "{}" ) ) ).get( "group" ) );
    }

    @Test
    public void testArgsThatAreNotAJsonObjectAreAnError()
    {
        assertEquals( "Invalid arguments for echo: expected a JSON object.",
                      errorText( asEditor( () -> tools.run( "content", "echo", "[1,2]" ) ) ) );
        assertEquals( "Invalid arguments for echo: expected a JSON object.",
                      errorText( asEditor( () -> tools.run( "content", "echo", "{not json" ) ) ) );
        assertEquals( "Invalid arguments for echo: expected a JSON object.",
                      errorText( asEditor( () -> tools.run( "content", "echo", "null" ) ) ) );
        assertTrue( calls.isEmpty() );
    }

    @Test
    public void testToolErrorBecomesItsMessageAndHint()
    {
        final Map<String, Object> result = asEditor( () -> tools.run( "content", "fail", "{\"kind\":\"tool\"}" ) );

        assertEquals( "Bad type. Call list-types.", errorText( result ) );
        assertFalse( result.containsKey( "structuredContent" ) );
    }

    @Test
    public void testUnexpectedExceptionHidesItsTextAndClass()
    {
        final String error = errorText( asEditor( () -> tools.run( "content", "fail", "{\"kind\":\"bug\"}" ) ) );

        assertTrue( error.matches( "Internal error in fail \\(error id [0-9a-f]{8}\\)\\. Retry with narrower arguments, or ask the " +
                                       "administrator to look up the error id in the server log\\." ), error );
        assertFalse( error.contains( "secret" ) || error.contains( "IllegalStateException" ), error );
    }

    @Test
    public void testRealRegistryHasListProjectsInContentAndQueryInGraphql()
    {
        final McpTools real = new McpTools( mock( com.enonic.app.guillotine.ServiceFacade.class ),
                                            mock( com.enonic.app.guillotine.graphql.SchemaProvider.class ), configService );

        assertEquals( Map.of( McpGroup.CONTENT, List.of( "list-projects" ), McpGroup.GRAPHQL, List.of( "query" ) ), real.names() );
    }

    private static String errorText( final Map<String, Object> result )
    {
        assertEquals( true, result.get( "isError" ), result::toString );
        return text( result );
    }

    private static <T> T asEditor( final Callable<T> callable )
    {
        return ContextBuilder.create().authInfo( EDITOR ).build().callWith( callable );
    }

    private final class EchoTool
        implements McpTool
    {
        private final McpGroup group;

        private final String name;

        EchoTool( final McpGroup group, final String name )
        {
            this.group = group;
            this.name = name;
        }

        @Override
        public McpGroup group()
        {
            return group;
        }

        @Override
        public String name()
        {
            return name;
        }

        @Override
        public Map<String, Object> execute( final Map<String, Object> args )
        {
            calls.add( args );
            return McpResult.success( Map.of( "group", group.groupName(), "args", args ) );
        }
    }

    private static final class FailingTool
        implements McpTool
    {
        @Override
        public McpGroup group()
        {
            return McpGroup.CONTENT;
        }

        @Override
        public String name()
        {
            return "fail";
        }

        @Override
        public Map<String, Object> execute( final Map<String, Object> args )
        {
            if ( "tool".equals( args.get( "kind" ) ) )
            {
                throw new ToolError( "Bad type.", "Call list-types." );
            }
            throw new IllegalStateException( "secret internal detail" );
        }
    }
}
