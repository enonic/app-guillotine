package com.enonic.app.guillotine.headless;

import java.util.Map;

public class HeadlessDataFetchingEnvironment
{
    private Object source;

    private Map<String, Object> arguments;

    private Object context;

    public Object getSource()
    {
        return source;
    }

    public void setSource( final Object source )
    {
        this.source = source;
    }

    public Map<String, Object> getArguments()
    {
        return arguments;
    }

    public void setArguments( final Map<String, Object> arguments )
    {
        this.arguments = arguments;
    }

    public Object getContext()
    {
        return context;
    }

    public void setContext( final Object context )
    {
        this.context = context;
    }
}
