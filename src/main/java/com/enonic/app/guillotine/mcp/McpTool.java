package com.enonic.app.guillotine.mcp;

import java.util.Map;

public interface McpTool
{
    String name();

    Map<String, Object> execute( Map<String, Object> args );
}
