package com.enonic.app.guillotine.mcp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * {@link McpControllerScriptTest} on GraalJS.
 */
public class McpControllerGraalScriptTest
    extends McpControllerScriptTest
{
    private static final String ENGINE_PROPERTY = "xp.script-engine";

    private static String previousEngine;

    @BeforeAll
    public static void useGraal()
    {
        previousEngine = System.getProperty( ENGINE_PROPERTY );
        System.setProperty( ENGINE_PROPERTY, "GraalJS" );
    }

    @AfterAll
    public static void restoreEngine()
    {
        if ( previousEngine == null )
        {
            System.clearProperty( ENGINE_PROPERTY );
        }
        else
        {
            System.setProperty( ENGINE_PROPERTY, previousEngine );
        }
    }
}
