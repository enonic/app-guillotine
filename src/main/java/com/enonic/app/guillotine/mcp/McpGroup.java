package com.enonic.app.guillotine.mcp;

/**
 * Guillotine's MCP tool groups, one descriptor and controller each under {@code mcp/<name>/}. Every group declares
 * {@code allow: role:system.everyone}; {@link McpTools} refuses anonymous callers in every group when {@code mcp.allowAnonymous} is false.
 */
public enum McpGroup
{
    CONTENT( "content" ),
    GRAPHQL( "graphql" ),
    REFERENCES( "references" );

    private final String groupName;

    McpGroup( final String groupName )
    {
        this.groupName = groupName;
    }

    public String groupName()
    {
        return groupName;
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
