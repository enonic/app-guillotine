package com.enonic.app.guillotine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GuillotineConfigServiceTest
{
    @Test
    public void testMcpDefaults()
    {
        final GuillotineConfigService service = activate( defaults() );

        assertTrue( service.isMcpEnabled() );
        assertEquals( "", service.getMcpDefaultProject() );
        assertTrue( service.isMcpAllowAnonymous() );
        assertEquals( 20000, service.getMcpMaxResultChars() );
    }

    @Test
    public void testDefaultProjectIsTrimmed()
    {
        final GuillotineConfig config = defaults();
        when( config.mcp_defaultProject() ).thenReturn( "  blog " );

        assertEquals( "blog", activate( config ).getMcpDefaultProject() );
    }

    @Test
    public void testAllowAnonymousCanBeTurnedOff()
    {
        final GuillotineConfig config = defaults();
        when( config.mcp_allowAnonymous() ).thenReturn( false );

        assertFalse( activate( config ).isMcpAllowAnonymous() );
    }

    @Test
    public void testMaxResultCharsIsClampedToTheDocumentedRange()
    {
        assertEquals( 20000, maxResultChars( 20000 ) );
        assertEquals( 1000, maxResultChars( 999 ) );
        assertEquals( 1000, maxResultChars( -5 ) );
        assertEquals( 1_000_000, maxResultChars( 1_000_001 ) );
        assertEquals( 1_000_000, maxResultChars( Integer.MAX_VALUE ) );
    }

    private static int maxResultChars( final int configured )
    {
        final GuillotineConfig config = defaults();
        when( config.mcp_maxResultChars() ).thenReturn( configured );
        return activate( config ).getMcpMaxResultChars();
    }

    private static GuillotineConfig defaults()
    {
        return mock( GuillotineConfig.class, invocation -> invocation.getMethod().getDefaultValue() );
    }

    private static GuillotineConfigService activate( final GuillotineConfig config )
    {
        final GuillotineConfigService service = new GuillotineConfigService();
        service.activate( config );
        return service;
    }
}
