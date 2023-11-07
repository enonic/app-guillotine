package com.enonic.app.guillotine.helper;

import com.enonic.xp.server.RunMode;

public class AppHelper
{
    public boolean isDevMode()
    {
        return RunMode.get() == RunMode.DEV;
    }
}
