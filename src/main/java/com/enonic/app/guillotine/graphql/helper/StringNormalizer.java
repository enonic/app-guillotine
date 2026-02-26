package com.enonic.app.guillotine.graphql.helper;

import com.enonic.xp.name.NamePrettyfier;

public class StringNormalizer
{

    public static String create( final String value )
    {
        if ( value == null || value.isBlank() )
        {
            return "";
        }
        String sanitizedValue = sanitize( value );
        if ( sanitizedValue.isEmpty() )
        {
            return "";
        }
        if ( Character.isDigit( sanitizedValue.charAt( 0 ) ) )
        {
            sanitizedValue = "_" + sanitizedValue;
        }
        return sanitizedValue.replaceAll( "[^0-9A-Za-z]+", "_" );
    }

    private static String sanitize( final String value )
    {
        StringBuilder sanitizedString = new StringBuilder();

        for ( char originalChar : value.toCharArray() )
        {
            if ( originalChar == '_' || originalChar == '-' || originalChar == '.' )
            {
                sanitizedString.append( originalChar );
            }
            else if ( originalChar == '+' || originalChar == ' ' )
            {
                sanitizedString.append( "-" );
            }
            else
            {
                String sanitizedChar = NamePrettyfier.create( String.valueOf( originalChar ) );
                if ( !"page".equals( sanitizedChar ) )
                {
                    sanitizedString.append(
                        originalChar == Character.toUpperCase( originalChar ) ? sanitizedChar.toUpperCase() : sanitizedChar );
                }
            }
        }

        return sanitizedString.toString();
    }

}
