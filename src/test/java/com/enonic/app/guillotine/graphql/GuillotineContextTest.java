package com.enonic.app.guillotine.graphql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GuillotineContextTest
{
    @Test
    public void testUniqueName()
    {
        GuillotineContext context = GuillotineContext.create().build();

        Assertions.assertEquals( "ObjectTypeName", context.uniqueName( "ObjectTypeName" ) );
        Assertions.assertEquals( "ObjectTypeName_2", context.uniqueName( "ObjectTypeName" ) );
        Assertions.assertEquals( "ObjectTypeName_3", context.uniqueName( "ObjectTypeName" ) );
    }
}
