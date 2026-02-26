package com.enonic.app.guillotine.graphql.helper;

public class NamingHelper
{
    public static String camelCase( String input )
    {
        StringBuilder result = new StringBuilder();
        for ( String s : input.split( "_" ) )
        {
            if ( s.isEmpty() )
            {
                continue;
            }
            result.append( Character.toUpperCase( s.charAt( 0 ) ) );
            if ( s.length() > 1 )
            {
                result.append( s.substring( 1 ) );
            }
        }
        return result.toString();
    }

    public static String applicationConfigKey( String applicationKey )
    {
        return applicationKey.replaceAll( "\\.", "-" );
    }
}
