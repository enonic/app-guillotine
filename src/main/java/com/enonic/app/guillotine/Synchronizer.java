package com.enonic.app.guillotine;

import com.enonic.xp.script.ScriptValue;

public class Synchronizer
{
    public static synchronized void sync( final ScriptValue callbackScriptValue )
    {
        callbackScriptValue.call();
    }
}