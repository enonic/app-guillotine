package com.enonic.app.guillotine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GuillotineConfigServiceTest
{
    @Test
    public void testMaxResultCharsIsClampedToTheDocumentedRange()
    {
        assertEquals( 20000, maxResultChars( 20000 ) );
        assertEquals( 1000, maxResultChars( 999 ) );
        assertEquals( 1000, maxResultChars( -5 ) );
        assertEquals( 1_000_000, maxResultChars( 1_000_001 ) );
        assertEquals( 1_000_000, maxResultChars( Integer.MAX_VALUE ) );
    }

    @Test
    public void testDefaultMaxResultChars()
    {
        final GuillotineConfigService service = new GuillotineConfigService();
        service.activate( mock( GuillotineConfig.class, invocation -> invocation.getMethod().getDefaultValue() ) );

        assertEquals( 20000, service.getMcpMaxResultChars() );
    }

    private static int maxResultChars( final int configured )
    {
        final GuillotineConfig config = mock( GuillotineConfig.class, invocation -> invocation.getMethod().getDefaultValue() );
        when( config.mcp_maxResultChars() ).thenReturn( configured );
        final GuillotineConfigService service = new GuillotineConfigService();
        service.activate( config );
        return service.getMcpMaxResultChars();
    }
}
