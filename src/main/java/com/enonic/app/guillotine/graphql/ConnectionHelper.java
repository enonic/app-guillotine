package com.enonic.app.guillotine.graphql;

import java.util.Base64;

public class ConnectionHelper
{
    public static String encodeCursor( String value )
    {
        return new String( Base64.getEncoder().encode( value.getBytes() ) );
    }

    public static String decodeCursor( String value )
    {
        return new String( Base64.getDecoder().decode( value.getBytes() ) );
    }
}
