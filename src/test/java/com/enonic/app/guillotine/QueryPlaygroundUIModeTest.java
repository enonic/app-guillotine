package com.enonic.app.guillotine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryPlaygroundUIModeTest
{
    @Test
    public void testValue()
    {
        Assertions.assertEquals( QueryPlaygroundUIMode.ON, QueryPlaygroundUIMode.from( "on" ) );
        Assertions.assertEquals( QueryPlaygroundUIMode.OFF, QueryPlaygroundUIMode.from( "off" ) );
        Assertions.assertEquals( QueryPlaygroundUIMode.AUTO, QueryPlaygroundUIMode.from( "auto" ) );
        try
        {
            QueryPlaygroundUIMode.from( "unknown" );
        }
        catch ( Exception ex )
        {
            Assertions.assertEquals( IllegalArgumentException.class, ex.getClass() );
        }
    }
}
