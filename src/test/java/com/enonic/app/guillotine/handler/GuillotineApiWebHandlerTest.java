package com.enonic.app.guillotine.handler;

import org.junit.jupiter.api.Test;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalResponse;
import com.enonic.xp.portal.controller.ControllerScript;
import com.enonic.xp.portal.controller.ControllerScriptFactory;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.web.HttpMethod;
import com.enonic.xp.web.HttpStatus;
import com.enonic.xp.web.WebException;
import com.enonic.xp.web.WebRequest;
import com.enonic.xp.web.WebResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
	void testHandle()
		throws Exception
	{
		ControllerScript controllerScript = mock( ControllerScript.class );
		when( controllerScript.execute( any( PortalRequest.class ) ) ).thenReturn( PortalResponse.create().build() );

		ControllerScriptFactory scriptFactory = mock( ControllerScriptFactory.class );
		when( scriptFactory.fromScript( any( ResourceKey.class ) ) ).thenReturn( controllerScript );

		GuillotineApiWebHandler instance = new GuillotineApiWebHandler( scriptFactory );

		WebRequest webRequest = mock( WebRequest.class );
		when( webRequest.getRawPath() ).thenReturn( "/site/hmdb/draft" );

		when( webRequest.getMethod() ).thenReturn( HttpMethod.OPTIONS );
		WebResponse webResponse = instance.handle( webRequest, null, null );
		assertTrue( webResponse.getStatus().is2xxSuccessful() );

		when( webRequest.getMethod() ).thenReturn( HttpMethod.POST );
		webResponse = instance.handle( webRequest, null, null );
		assertTrue( webResponse.getStatus().is2xxSuccessful() );

		when( webRequest.getMethod() ).thenReturn( HttpMethod.GET );
		webResponse = instance.handle( webRequest, null, null );
		assertTrue( webResponse.getStatus().is2xxSuccessful() );

		when( webRequest.getMethod() ).thenReturn( HttpMethod.HEAD );
		WebException ex = assertThrows( WebException.class, () -> instance.handle( webRequest, null, null ) );
		assertEquals( HttpStatus.METHOD_NOT_ALLOWED, ex.getStatus() );
	}
}
