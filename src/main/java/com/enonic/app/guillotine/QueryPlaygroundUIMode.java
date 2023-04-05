package com.enonic.app.guillotine;

public enum QueryPlaygroundUIMode
{
    ON, OFF, AUTO;

    public static QueryPlaygroundUIMode from( final String value )
    {
        try
        {
            return QueryPlaygroundUIMode.valueOf( value.toUpperCase() );
        }
        catch ( IllegalArgumentException e )
        {
            return AUTO;
        }
    }
}
