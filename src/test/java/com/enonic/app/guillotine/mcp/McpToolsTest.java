package com.enonic.app.guillotine.mcp;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ToolError;

import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class McpToolsTest
{
    private GuillotineConfigService configService;

    private McpTools tools;

    @BeforeEach
    public void setUp()
    {
        configService = mock( GuillotineConfigService.class );
        when( configService.isMcpEnabled() ).thenReturn( true );
        tools = new McpTools( configService, List.of( new EchoArgsTool() ) );
    }

    @Test
    public void testDispatchesToToolWithParsedArgs()
    {
        final Map<String, Object> result = tools.run( "echo_args", "{\"query\":\"news\",\"limit\":2}" );

        assertFalse( result.containsKey( "isError" ) );
        assertEquals( Map.of( "query", "news", "limit", 2 ), result.get( "structuredContent" ) );
    }

    @Test
    public void testNullOrBlankArgsAreEmptyObject()
    {
        assertEquals( Map.of(), tools.run( "echo_args", null ).get( "structuredContent" ) );
        assertEquals( Map.of(), tools.run( "echo_args", " " ).get( "structuredContent" ) );
    }

    @Test
    public void testUnknownToolIsErrorWithHint()
    {
        final Map<String, Object> result = tools.run( "drop_database", "{}" );

        assertEquals( true, result.get( "isError" ) );
        assertFalse( result.containsKey( "structuredContent" ) );
        assertEquals( "Unknown Guillotine MCP tool: drop_database. Available tools: echo_args.", text( result ) );
    }

    @Test
    public void testMalformedArgsIsError()
    {
        final Map<String, Object> result = tools.run( "echo_args", "{not json" );

        assertEquals( true, result.get( "isError" ) );
        assertEquals( "Invalid arguments for echo_args: expected a JSON object.", text( result ) );
    }

    @Test
    public void testNonObjectArgsIsError()
    {
        final Map<String, Object> result = tools.run( "echo_args", "[1,2]" );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( text( result ).startsWith( "Invalid arguments for echo_args" ) );
    }

    @Test
    public void testDisabledByConfig()
    {
        when( configService.isMcpEnabled() ).thenReturn( false );

        final Map<String, Object> result = tools.run( "echo_args", "{}" );

        assertEquals( true, result.get( "isError" ) );
        assertEquals( "Guillotine MCP tools are disabled by configuration. Do not retry; ask the administrator to set " +
                          "mcp.enabled=true in com.enonic.app.guillotine.cfg.", text( result ) );
    }

    @Test
    public void testToolErrorBecomesErrorResultWithHint()
    {
        final Map<String, Object> result = new McpTools( configService, List.of( new RejectingTool() ) ).run( "reject", "{}" );

        assertEquals( true, result.get( "isError" ) );
        assertEquals( "Bad input. Fix it.", text( result ) );
    }

    @Test
    public void testUnexpectedExceptionIsGenericErrorWithoutClassNames()
    {
        final McpTool failing = new McpTool()
        {
            @Override
            public String name()
            {
                return "fail";
            }

            @Override
            public Map<String, Object> execute( final Map<String, Object> args )
            {
                throw new IllegalStateException( "com.enonic.xp.node.NodeNotFoundException: boom" );
            }
        };

        final Map<String, Object> result = new McpTools( configService, List.of( failing ) ).run( "fail", "{}" );

        assertEquals( true, result.get( "isError" ) );
        assertEquals( "Internal error in fail; see the server log for details. " +
                          "Retry with narrower arguments, or ask the administrator to check the log.", text( result ) );
        assertFalse( text( result ).contains( "Exception" ) );
    }

    private static final class EchoArgsTool
        implements McpTool
    {
        @Override
        public String name()
        {
            return "echo_args";
        }

        @Override
        public Map<String, Object> execute( final Map<String, Object> args )
        {
            return McpResult.success( "Echo", args );
        }
    }

    private static final class RejectingTool
        implements McpTool
    {
        @Override
        public String name()
        {
            return "reject";
        }

        @Override
        public Map<String, Object> execute( final Map<String, Object> args )
        {
            throw new ToolError( "Bad input.", "Fix it." );
        }
    }
}
