package com.enonic.app.guillotine.mcp;

/**
 * Guillotine's MCP tool groups, one descriptor and controller each under {@code mcp/<name>/}. Every group declares
 * {@code allow: role:system.authenticated}. Until XP enforces {@code allow}, {@link McpTools} refuses anonymous callers itself, except
 * in groups an operator may open to them with {@code mcp.allowAnonymous}.
 */
public enum McpGroup
{
    CONTENT( "content", true ),
    GRAPHQL( "graphql", true ),
    REFERENCES( "references", false );

    private final String groupName;

    private final boolean anonymousOptIn;

    McpGroup( final String groupName, final boolean anonymousOptIn )
    {
        this.groupName = groupName;
        this.anonymousOptIn = anonymousOptIn;
    }

    public String groupName()
    {
        return groupName;
    }

    public boolean anonymousOptIn()
    {
        return anonymousOptIn;
    }

    public static McpGroup from( final String name )
    {
        for ( final McpGroup group : values() )
        {
            if ( group.groupName.equals( name ) )
            {
                return group;
            }
        }
        return null;
    }
}
