package com.enonic.app.guillotine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class GuillotineConfigServiceTest
{
    @Test
    public void testNothingAllowedByDefault()
    {
        final GuillotineConfigService service = activate( "" );

        assertFalse( service.isBaseUrlAllowed( "https://media.example.com/" ) );
        assertFalse( service.isBaseUrlAllowed( "/" ) );
        assertFalse( service.isBaseUrlAllowed( "" ) );
        assertFalse( service.isBaseUrlAllowed( null ) );
    }

    @Test
    public void testExactValueAllowedWithOrWithoutTrailingSlash()
    {
        final GuillotineConfigService service = activate( "https://media.example.com/, https://www.example.com" );

        assertTrue( service.isBaseUrlAllowed( "https://media.example.com" ) );
        assertTrue( service.isBaseUrlAllowed( "https://media.example.com/" ) );
        assertTrue( service.isBaseUrlAllowed( "https://www.example.com" ) );
        assertTrue( service.isBaseUrlAllowed( "https://www.example.com/" ) );

        assertFalse( service.isBaseUrlAllowed( "https://evil.example.com/" ) );
        assertFalse( service.isBaseUrlAllowed( "https://media.example.com/sub" ) );
    }

    @Test
    public void testWildcardAllowsEverything()
    {
        final GuillotineConfigService service = activate( "*" );

        assertTrue( service.isBaseUrlAllowed( "https://anything.example.com/" ) );
        assertTrue( service.isBaseUrlAllowed( "/" ) );

        assertFalse( service.isBaseUrlAllowed( null ) );
    }

    private static GuillotineConfigService activate( final String allowedBaseUrls )
    {
        final GuillotineConfigService service = new GuillotineConfigService();
        service.activate( mock( GuillotineConfig.class, invocation -> "allowedBaseUrls".equals( invocation.getMethod().getName() )
            ? allowedBaseUrls
            : invocation.getMethod().getDefaultValue() ) );
        return service;
    }
}
