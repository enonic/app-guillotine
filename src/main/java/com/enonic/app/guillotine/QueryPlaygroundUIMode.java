package com.enonic.app.guillotine;

import java.util.Objects;

public enum QueryPlaygroundUIMode
{
    ON, OFF, AUTO;

    public static QueryPlaygroundUIMode from( final String value )
    {
        return QueryPlaygroundUIMode.valueOf( Objects.requireNonNullElse( value, "auto" ).toUpperCase() );
    }
}
