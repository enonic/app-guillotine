package com.enonic.app.guillotine.graphql;

import java.util.List;
import java.util.Map;

public class BaseCreationCallbackParams
{
    private String description;

    private Map<String, Map<String, Object>> addFields;

    private Map<String, Map<String, Object>> modifyFields;

    private List<String> removeFields;

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }

    public Map<String, Map<String, Object>> getAddFields()
    {
        return addFields;
    }

    public void addFields( final Map<String, Map<String, Object>> addFields )
    {
        this.addFields = addFields;
    }

    public void removeFields( final List<String> removeFields )
    {
        this.removeFields = removeFields;
    }

    public List<String> getRemoveFields()
    {
        return removeFields;
    }

    public Map<String, Map<String, Object>> getModifyFields()
    {
        return modifyFields;
    }

    public void modifyFields( final Map<String, Map<String, Object>> modifyFields )
    {
        this.modifyFields = modifyFields;
    }
}
