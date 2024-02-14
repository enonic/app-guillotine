package com.enonic.app.guillotine.graphql.transformer;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.script.ScriptValue;

public class ContextualFieldResolver
{
    private final ApplicationKey applicationKey;

    private final ScriptValue resolveFunction;

    public ContextualFieldResolver( final ApplicationKey applicationKey, final ScriptValue resolveFunction )
    {
        this.applicationKey = applicationKey;
        this.resolveFunction = resolveFunction;
    }

    public ApplicationKey getApplicationKey()
    {
        return applicationKey;
    }

    public ScriptValue getResolveFunction()
    {
        return resolveFunction;
    }
}
