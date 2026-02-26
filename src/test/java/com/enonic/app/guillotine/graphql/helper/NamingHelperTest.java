package com.enonic.app.guillotine.graphql.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NamingHelperTest
{
    @Test
    public void camelCase_normalInput()
    {
        assertEquals( "Field", NamingHelper.camelCase( "field" ) );
        assertEquals( "FieldName", NamingHelper.camelCase( "field_name" ) );
        assertEquals( "FieldNameFoo", NamingHelper.camelCase( "field_name_foo" ) );
    }

    @Test
    public void camelCase_emptyString_returnsEmpty()
    {
        assertEquals( "", NamingHelper.camelCase( "" ) );
    }

    @Test
    public void camelCase_leadingUnderscore_skipsEmptySegment()
    {
        assertEquals( "1option", NamingHelper.camelCase( "_1option" ) );
        assertEquals( "Foo", NamingHelper.camelCase( "_foo" ) );
    }

    @Test
    public void camelCase_consecutiveUnderscores_skipsEmptySegments()
    {
        assertEquals( "FooBar", NamingHelper.camelCase( "foo__bar" ) );
    }

    @Test
    public void camelCase_onlyUnderscores_returnsEmpty()
    {
        assertEquals( "", NamingHelper.camelCase( "_" ) );
        assertEquals( "", NamingHelper.camelCase( "__" ) );
    }
}
