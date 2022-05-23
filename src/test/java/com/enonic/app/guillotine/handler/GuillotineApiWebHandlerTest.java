package com.enonic.app.guillotine.handler;

import org.junit.jupiter.api.Test;

import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.WebRequest;

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

        GuillotineApiWebHandler instance = new GuillotineApiWebHandler( scriptFactory );

        WebRequest webRequest = mock( WebRequest.class );
        when( webRequest.getMethod() ).thenReturn( HttpMethod.POST );
        when( webRequest.isWebSocket() ).thenReturn( false );

        when( webRequest.getPath() ).thenReturn( "/admin/site/preview/hmdb/draft/" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getPath() ).thenReturn( "/admin/site/preview/hmdb/draft" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getPath() ).thenReturn( "/site/hmdb/draft" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getPath() ).thenReturn( "/site/hmdb/draft/" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getPath() ).thenReturn( "/site/hmdb/draft/hmdb/_graphql" );
        assertFalse( instance.canHandle( webRequest ) );

        when( webRequest.getPath() ).thenReturn( "/site/sample-blog/draft" );
        assertTrue( instance.canHandle( webRequest ) );

        when( webRequest.getPath() ).thenReturn( "/something" );
        assertFalse( instance.canHandle( webRequest ) );
    }
}
