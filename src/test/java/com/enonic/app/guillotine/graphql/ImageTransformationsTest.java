package com.enonic.app.guillotine.graphql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageTransformationsTest
{
    @AfterEach
    public void tearDown()
    {
        PortalRequestAccessor.remove();
    }

    @Test
    public void autoAllowsTheApiEndpointAndTheAdmin()
    {
        assertEquals( ImageTransformations.ALLOWED, resolve( "/api/com.enonic.app.guillotine:graphql", null ) );
        assertEquals( ImageTransformations.ALLOWED, resolve( "/admin/com.enonic.app.contentstudio/main/_/com.enonic.app.guillotine:graphql", null ) );
        assertEquals( ImageTransformations.ALLOWED, resolve( null, null ) );
    }

    @Test
    public void autoDeniesSiteAndWebappMounts()
    {
        assertEquals( ImageTransformations.DENIED, resolve( "/site/myproject/master/mysite/_/com.enonic.app.guillotine:graphql", null ) );
        assertEquals( ImageTransformations.DENIED, resolve( "/webapp/com.example.app/_/com.enonic.app.guillotine:graphql", "auto" ) );
    }

    @Test
    public void settingOverridesAuto()
    {
        assertEquals( ImageTransformations.ALLOWED, resolve( "/site/myproject/master/mysite/_/com.enonic.app.guillotine:graphql", "allow" ) );
        assertEquals( ImageTransformations.DENIED, resolve( "/api/com.enonic.app.guillotine:graphql", "deny" ) );
        assertEquals( ImageTransformations.DENIED, resolve( "/api/com.enonic.app.guillotine:graphql", " Deny " ) );
    }

    private static ImageTransformations resolve( final String baseUri, final String setting )
    {
        PortalRequestAccessor.remove();
        if ( baseUri != null )
        {
            final PortalRequest portalRequest = new PortalRequest();
            portalRequest.setBaseUri( baseUri );
            PortalRequestAccessor.set( portalRequest );
        }
        final ContextBuilder context = ContextBuilder.copyOf( ContextAccessor.current() );
        if ( setting != null )
        {
            context.attribute( ImageTransformations.CONTEXT_ATTRIBUTE, setting );
        }
        return context.build().callWith( ImageTransformations::ofRequest );
    }
}
