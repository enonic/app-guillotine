package com.enonic.app.guillotine;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(immediate = true, service = GuillotineConfigService.class, configurationPid = "com.enonic.app.guillotine")
public class GuillotineConfigService
{
    static final int MIN_MCP_RESULT_CHARS = 1000;

    static final int MAX_MCP_RESULT_CHARS = 1_000_000;

    private ModifyUnknownFieldMode modifyUnknownFieldMode;

    private int maxQueryTokens;

    private boolean mcpEnabled;

    private String mcpDefaultProject;

    private int mcpMaxResultChars;

    private int mcpMaxDepth;

    private int mcpMaxComplexity;

    @Activate
    @Modified
    public void activate( final GuillotineConfig config )
    {
        this.modifyUnknownFieldMode = ModifyUnknownFieldMode.from( config.graphql_extensions_modifyUnknownField() );
        this.maxQueryTokens = config.maxQueryTokens();
        this.mcpEnabled = config.mcp_enabled();
        this.mcpDefaultProject = config.mcp_defaultProject().trim();
        this.mcpMaxResultChars = Math.clamp( config.mcp_maxResultChars(), MIN_MCP_RESULT_CHARS, MAX_MCP_RESULT_CHARS );
        this.mcpMaxDepth = config.mcp_maxDepth();
        this.mcpMaxComplexity = config.mcp_maxComplexity();
    }

    public ModifyUnknownFieldMode getModifyUnknownFieldMode()
    {
        return modifyUnknownFieldMode;
    }

    public int getMaxQueryTokens()
    {
        return maxQueryTokens;
    }

    public boolean isMcpEnabled()
    {
        return mcpEnabled;
    }

    public String getMcpDefaultProject()
    {
        return mcpDefaultProject;
    }

    public int getMcpMaxResultChars()
    {
        return mcpMaxResultChars;
    }

    public int getMcpMaxDepth()
    {
        return mcpMaxDepth;
    }

    public int getMcpMaxComplexity()
    {
        return mcpMaxComplexity;
    }
}
