package com.enonic.app.guillotine.mcp;

import java.util.Map;

public interface McpTool
{
    McpGroup group();

    String name();

    Map<String, Object> execute( Map<String, Object> args );
}
