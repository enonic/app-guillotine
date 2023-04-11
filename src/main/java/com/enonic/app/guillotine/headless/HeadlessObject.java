package com.enonic.app.guillotine.headless;

import com.enonic.xp.annotation.PublicApi;

@PublicApi
public class HeadlessObject
{
    private String name;

    private String description;

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }
}
