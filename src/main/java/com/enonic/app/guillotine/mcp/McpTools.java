package com.enonic.app.guillotine.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.app.guillotine.mcp.tools.ListProjectsTool;
import com.enonic.app.guillotine.mcp.tools.QueryTool;

public final class McpTools
{
    private static final Logger LOG = LoggerFactory.getLogger( McpTools.class );

    private final GuillotineConfigService configService;

    private final Map<String, McpTool> tools = new LinkedHashMap<>();

    public McpTools( final ServiceFacade serviceFacade, final GuillotineConfigService configService,
                     final SchemaProvider schemaProvider )
    {
        this( configService, tools( serviceFacade, configService, schemaProvider ) );
    }

    McpTools( final GuillotineConfigService configService, final List<McpTool> tools )
    {
        this.configService = configService;
        tools.forEach( tool -> this.tools.put( tool.name(), tool ) );
    }

    private static List<McpTool> tools( final ServiceFacade serviceFacade, final GuillotineConfigService configService,
                                        final SchemaProvider schemaProvider )
    {
        final ProjectBranchResolver resolver =
            new ProjectBranchResolver( serviceFacade.getProjectService(), serviceFacade.getNodeService(), configService );
        return List.of( new ListProjectsTool( serviceFacade.getProjectService(), configService, resolver ),
                        new QueryTool( schemaProvider, configService, resolver ) );
    }

    public Map<String, Object> run( final String name, final String argsJson )
    {
        if ( !configService.isMcpEnabled() )
        {
            return McpResult.error( "Guillotine MCP tools are disabled by configuration. Do not retry; ask the administrator to set " +
                                        "mcp.enabled=true in com.enonic.app.guillotine.cfg." );
        }

        final McpTool tool = tools.get( name );
        if ( tool == null )
        {
            return McpResult.error(
                "Unknown Guillotine MCP tool: " + name + ". Available tools: " + String.join( ", ", tools.keySet() ) + "." );
        }

        final Map<String, Object> args;
        try
        {
            args = McpJson.readObject( argsJson );
        }
        catch ( IllegalArgumentException e )
        {
            return McpResult.error( "Invalid arguments for " + name + ": expected a JSON object." );
        }

        try
        {
            return tool.execute( args );
        }
        catch ( ToolError e )
        {
            LOG.debug( "MCP tool {} rejected the call: {}", name, e.getMessage() );
            return McpResult.error( e );
        }
        catch ( RuntimeException e )
        {
            LOG.error( "MCP tool {} failed", name, e );
            return McpResult.error( "Internal error in " + name + "; see the server log for details. " +
                                        "Retry with narrower arguments, or ask the administrator to check the log." );
        }
    }
}
