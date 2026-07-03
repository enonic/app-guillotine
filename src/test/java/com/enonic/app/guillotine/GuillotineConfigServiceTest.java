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
        final GuillotineConfigService service = activate( "", "" );

        assertFalse( service.isPageBaseUrlAllowed( "https://www.example.com/" ) );
        assertFalse( service.isMediaBaseUrlAllowed( "https://media.example.com/" ) );
        assertFalse( service.isPageBaseUrlAllowed( "" ) );
        assertFalse( service.isMediaBaseUrlAllowed( null ) );
    }

    @Test
    public void testAllowListsAreIndependent()
    {
        final GuillotineConfigService service = activate( "https://www.example.com", "https://media.example.com" );

        assertTrue( service.isPageBaseUrlAllowed( "https://www.example.com" ) );
        assertFalse( service.isPageBaseUrlAllowed( "https://media.example.com" ) );

        assertTrue( service.isMediaBaseUrlAllowed( "https://media.example.com" ) );
        assertFalse( service.isMediaBaseUrlAllowed( "https://www.example.com" ) );
    }

    @Test
    public void testExactValueAllowedWithOrWithoutTrailingSlash()
    {
        final GuillotineConfigService service = activate( "https://www.example.com/, https://pages.example.com", "" );

        assertTrue( service.isPageBaseUrlAllowed( "https://www.example.com" ) );
        assertTrue( service.isPageBaseUrlAllowed( "https://www.example.com/" ) );
        assertTrue( service.isPageBaseUrlAllowed( "https://pages.example.com" ) );
        assertTrue( service.isPageBaseUrlAllowed( "https://pages.example.com/" ) );

        assertFalse( service.isPageBaseUrlAllowed( "https://evil.example.com/" ) );
        assertFalse( service.isPageBaseUrlAllowed( "https://www.example.com/sub" ) );
    }

    @Test
    public void testWildcardAllowsEverythingPerList()
    {
        final GuillotineConfigService service = activate( "*", "" );

        assertTrue( service.isPageBaseUrlAllowed( "https://anything.example.com/" ) );
        assertFalse( service.isPageBaseUrlAllowed( null ) );

        assertFalse( service.isMediaBaseUrlAllowed( "https://anything.example.com/" ) );
    }

    private static GuillotineConfigService activate( final String allowedPageBaseUrls, final String allowedMediaBaseUrls )
    {
        final GuillotineConfigService service = new GuillotineConfigService();
        service.activate( mock( GuillotineConfig.class, invocation -> {
            final String name = invocation.getMethod().getName();
            if ( "allowedPageBaseUrls".equals( name ) )
            {
                return allowedPageBaseUrls;
            }
            if ( "allowedMediaBaseUrls".equals( name ) )
            {
                return allowedMediaBaseUrls;
            }
            return invocation.getMethod().getDefaultValue();
        } ) );
        return service;
    }
}
