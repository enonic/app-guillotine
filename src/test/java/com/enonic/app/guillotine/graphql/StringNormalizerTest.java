package com.enonic.app.guillotine.graphql;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.graphql.helper.StringNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringNormalizerTest
{
    @Test
    public void nullOrBlankInput_returnsEmpty()
    {
        assertEquals( "", StringNormalizer.create( null ) );
        assertEquals( "", StringNormalizer.create( "" ) );
        assertEquals( "", StringNormalizer.create( "   " ) );
    }

    @Test
    public void allCharsDroppedBySanitizer_returnsEmpty()
    {
        assertEquals( "", StringNormalizer.create( "??" ) );
        assertEquals( "", StringNormalizer.create( "?&*^%" ) );
    }

    @Test
    public void digitPrefixedName_doesNotCrashOnCamelCase()
    {
        assertEquals( "_123fieldName", StringNormalizer.create( "123fieldName" ) );
    }

    @Test
    public void test()
    {
        assertEquals( "field_name", StringNormalizer.create( "field+name" ) );
        assertEquals( "field_name", StringNormalizer.create( "field+name" ) );
        assertEquals( "field_name", StringNormalizer.create( "field name" ) );
        assertEquals( "field_name", StringNormalizer.create( "field_name" ) );
        assertEquals( "field_name", StringNormalizer.create( "field-name" ) );
        assertEquals( "field_Name", StringNormalizer.create( "field_Name" ) );
        assertEquals( "field_Name", StringNormalizer.create( "field-Name" ) );
        assertEquals( "fieldName", StringNormalizer.create( "fieldName" ) );
        assertEquals( "fieldname", StringNormalizer.create( "fieldname" ) );
        assertEquals( "_123fieldName", StringNormalizer.create( "123fieldName" ) );
        assertEquals( "fieldName123", StringNormalizer.create( "fieldName123" ) );
        assertEquals( "fieldName_123", StringNormalizer.create( "fieldName-123" ) );
        assertEquals( "fieldName_123", StringNormalizer.create( "fieldName_123" ) );
        assertEquals( "fieldName_123", StringNormalizer.create( "fieldName 123" ) );
        assertEquals( "nazvaniePolJA", StringNormalizer.create( "названиеПолЯ" ) );
        assertEquals( "_12nazvaniePolja", StringNormalizer.create( "#12названиеПоля?&*^%" ) );
        assertEquals( "com_enonic_app", StringNormalizer.create( "com.enonic.app" ) );
    }
}
