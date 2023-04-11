package com.enonic.app.guillotine.headless;

import com.enonic.xp.annotation.PublicApi;

@PublicApi
public class HeadlessSchema
{
    private String identifier;

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier( final String identifier )
    {
        this.identifier = identifier;
    }
}
