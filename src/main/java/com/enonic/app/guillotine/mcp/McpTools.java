package com.enonic.app.guillotine.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

/**
 * Runs one tool call: checks {@code mcp.enabled} and the caller, reads the arguments and turns every failure into an {@code isError}
 * result. A {@link ToolError} keeps its message; any other exception is logged and the model gets a generic text with an error id.
 */
public final class McpTools
{
    private static final Logger LOG = LoggerFactory.getLogger( McpTools.class );

    private final GuillotineConfigService configService;

    private final Map<McpGroup, Map<String, McpTool>> tools = new LinkedHashMap<>();

    public McpTools( final ServiceFacade serviceFacade, final SchemaProvider schemaProvider, final GuillotineConfigService configService )
    {
        this( configService, tools( serviceFacade, schemaProvider, configService ) );
    }

    McpTools( final GuillotineConfigService configService, final List<McpTool> tools )
    {
        this.configService = configService;
        tools.forEach( tool -> this.tools.computeIfAbsent( tool.group(), group -> new LinkedHashMap<>() ).put( tool.name(), tool ) );
    }

    private static List<McpTool> tools( final ServiceFacade serviceFacade, final SchemaProvider schemaProvider,
                                        final GuillotineConfigService configService )
    {
        final ProjectBranchResolver resolver =
            new ProjectBranchResolver( serviceFacade.getProjectService(), serviceFacade.getNodeService(), configService );
        return List.of(
            new ListProjectsTool( serviceFacade.getProjectService(), serviceFacade.getContentService(), configService, resolver ),
            new QueryTool( schemaProvider, configService, resolver ) );
    }

    /**
     * Local tool names per group, in registration order.
     */
    Map<McpGroup, List<String>> names()
    {
        final Map<McpGroup, List<String>> names = new LinkedHashMap<>();
        tools.forEach( ( group, byName ) -> names.put( group, List.copyOf( byName.keySet() ) ) );
        return names;
    }

    public Map<String, Object> run( final String groupName, final String name, final String argsJson )
    {
        if ( !configService.isMcpEnabled() )
        {
            return McpResult.error( "Guillotine MCP tools are disabled by configuration. Do not retry; ask the administrator to set " +
                                        "mcp.enabled=true in com.enonic.app.guillotine.cfg." );
        }

        final McpGroup group = McpGroup.from( groupName );
        final McpTool tool = group != null ? tools.getOrDefault( group, Map.of() ).get( name ) : null;
        if ( tool == null )
        {
            return McpResult.error( "Unknown Guillotine MCP tool: " + McpJson.shorten( groupName + "/" + name, McpJson.PREVIEW_CHARS ) +
                                        "." );
        }

        if ( !ProjectBranchResolver.isAuthenticated() && !( group.anonymousOptIn() && configService.isMcpAllowAnonymous() ) )
        {
            return McpResult.error( anonymousRefusal( group ) );
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
            final String errorId = UUID.randomUUID().toString().substring( 0, 8 );
            LOG.error( "MCP tool {} failed (error id {})", name, errorId, e );
            return McpResult.error( "Internal error in " + name + " (error id " + errorId + "). Retry with narrower arguments, or " +
                                        "ask the administrator to look up the error id in the server log." );
        }
    }

    private static ToolError anonymousRefusal( final McpGroup group )
    {
        final String message = "This tool needs a signed-in caller, and this call is anonymous.";
        if ( group.anonymousOptIn() )
        {
            return new ToolError( message, "Do not retry; ask the user to sign in to Enonic XP, or ask the administrator to set " +
                "mcp.allowAnonymous=true in com.enonic.app.guillotine.cfg to let anonymous callers read published content." );
        }
        return new ToolError( message, "Do not retry; ask the user to sign in to Enonic XP. Anonymous callers cannot use the " +
            group.groupName() + " tools." );
    }
}
