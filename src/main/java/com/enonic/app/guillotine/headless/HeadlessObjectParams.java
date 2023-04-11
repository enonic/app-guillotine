package com.enonic.app.guillotine.headless;

import java.util.ArrayList;
import java.util.List;

import com.enonic.xp.annotation.PublicApi;

@PublicApi
public class HeadlessObjectParams
{
    private String typeName;

    private String description;

    private List<HeadlessFieldDefinition> fields = new ArrayList<>();

    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName( final String typeName )
    {
        this.typeName = typeName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }

    public List<HeadlessFieldDefinition> getFields()
    {
        return fields;
    }

    public void setFields( final List<HeadlessFieldDefinition> fields )
    {
        this.fields = fields;
    }
}
