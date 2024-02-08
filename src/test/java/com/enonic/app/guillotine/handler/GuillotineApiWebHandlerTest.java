package com.enonic.app.guillotine.handler;

import com.enonic.app.guillotine.GuillotineConfig;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GuillotineApiWebHandlerTest
{
    @Test
    public void testCanHandle()
    {
        ControllerScriptFactory scriptFactory = mock( ControllerScriptFactory.class );
        GuillotineConfig config = mock( GuillotineConfig.class );
        when( config.endpoint_postfix_regex() ).thenReturn( "" );

        GuillotineApiWebHandler instance = new GuillotineApiWebHandler( scriptFactory, config );

        WebRequest webRequest = mock( WebRequest.class );
        when( webRequest.getMethod() ).thenReturn( HttpMethod.POST );
        when( webRequest.isWebSocket() ).thenReturn( false );

        when( webRequest.getRawPath() ).thenReturn( "/admin/site/preview/hmdb/draft/" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/admin/site/preview/hmdb/draft" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/hmdb/draft" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/hmdb/draft/" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/hmdb/draft/hmdb/_graphql" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/sample-blog/draft" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/something" );
        assertFalse( instance.canHandle( webRequest ) );
    }

    @Test
    public void testCanHandlePostfix()
    {
        ControllerScriptFactory scriptFactory = mock( ControllerScriptFactory.class );
        GuillotineConfig config = mock( GuillotineConfig.class );
        when( config.endpoint_postfix_regex() ).thenReturn( "/postfix" );

        GuillotineApiWebHandler instance = new GuillotineApiWebHandler( scriptFactory, config );

        WebRequest webRequest = mock( WebRequest.class );
        when( webRequest.getMethod() ).thenReturn( HttpMethod.POST );
        when( webRequest.isWebSocket() ).thenReturn( false );

        when( webRequest.getRawPath() ).thenReturn( "/admin/site/preview/hmdb/draft/postfix/" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/admin/site/preview/hmdb/draft/postfix" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/hmdb/draft/postfix" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/hmdb/draft/postfix/" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/hmdb/draft/hmdb/_graphql" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/site/sample-blog/draft/postfix" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getRawPath() ).thenReturn( "/something/postfix" );
        assertFalse( instance.canHandle( webRequest ) );
    }
}
