package com.enonic.app.guillotine.handler;

import java.util.function.Function;

public class CreationCallbackExecutor
{
    public void execute( Function<Object[], Object> function, Object[] params )
    {
        if ( function != null )
        {
            function.apply( params );
        }
    }
}
