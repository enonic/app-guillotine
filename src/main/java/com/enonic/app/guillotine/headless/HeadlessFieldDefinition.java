package com.enonic.app.guillotine.headless;

import java.util.Map;
import java.util.function.Function;

import com.enonic.xp.annotation.PublicApi;

@PublicApi
public class HeadlessFieldDefinition
{
    private String name;

    private String description;

    private String type;

    private Map<String, Object> arguments;

    private Function<HeadlessDataFetchingEnvironment, Object> resolveFunction = ( env ) -> null;

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

    public String getType()
    {
        return type;
    }

    public void setType( final String type )
    {
        this.type = type;
    }

    public Map<String, Object> getArguments()
    {
        return arguments;
    }

    public void setArguments( final Map<String, Object> arguments )
    {
        this.arguments = arguments;
    }

    public Function<HeadlessDataFetchingEnvironment, Object> getResolveFunction()
    {
        return resolveFunction;
    }

    public void setResolveFunction( final Function<HeadlessDataFetchingEnvironment, Object> resolveFunction )
    {
        this.resolveFunction = resolveFunction;
    }
}
