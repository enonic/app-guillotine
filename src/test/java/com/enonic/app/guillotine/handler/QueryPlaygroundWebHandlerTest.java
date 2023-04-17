package com.enonic.app.guillotine.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfig;
import com.enonic.app.guillotine.QueryPlaygroundUIMode;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryPlaygroundWebHandlerTest
{

    ControllerScriptFactory scriptFactory;

    ApplicationService applicationService;

    @BeforeEach
    public void setUp()
    {
        this.scriptFactory = mock( ControllerScriptFactory.class );
        this.applicationService = mock( ApplicationService.class );
    }

    @Test
    public void testCanHandleInEnabledMode()
    {
        QueryPlaygroundWebHandler instance = new QueryPlaygroundWebHandler( scriptFactory, applicationService );
        instance.activate( mock( GuillotineConfig.class, invocation -> QueryPlaygroundUIMode.ON.toString() ) );

        WebRequest webRequest = mock( WebRequest.class );
        when( webRequest.getMethod() ).thenReturn( HttpMethod.GET );
        when( webRequest.isWebSocket() ).thenReturn( false );

        when( webRequest.getRawPath() ).thenReturn( "/site/repo" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/repo/" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/repo/_static/styles/main.css" );
        assertTrue( instance.canHandle( webRequest ) );
    }

    @Test
    public void testCanHandleInDisabledMode()
    {
        QueryPlaygroundWebHandler instance = new QueryPlaygroundWebHandler( scriptFactory, applicationService );
        instance.activate( mock( GuillotineConfig.class, invocation -> invocation.getMethod().getDefaultValue() ) );

        WebRequest webRequest = mock( WebRequest.class );
        when( webRequest.getMethod() ).thenReturn( HttpMethod.GET );
        when( webRequest.isWebSocket() ).thenReturn( false );

        when( webRequest.getRawPath() ).thenReturn( "/site/repo" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/repo/" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/repo/_static/styles/main.css" );
        assertFalse( instance.canHandle( webRequest ) );
    }
}
