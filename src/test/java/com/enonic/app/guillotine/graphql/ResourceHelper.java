package com.enonic.app.guillotine.graphql;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ResourceHelper
{
    public static String readGraphQLQuery( final String fileName )
    {
        URL resource = Thread.currentThread().getContextClassLoader().getResource( fileName );
        if ( resource == null )
        {
            throw new IllegalArgumentException( "Resource [" + fileName + "] not found" );
        }
        try (InputStream stream = resource.openStream())
        {
            return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( "Failed to load test file: " + resource, e );
        }
    }
}
