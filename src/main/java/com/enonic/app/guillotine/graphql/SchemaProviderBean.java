package com.enonic.app.guillotine.graphql;

import java.util.Map;
import java.util.function.Supplier;

import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class SchemaProviderBean
    implements ScriptBean
{
    private Supplier<SchemaProvider> schemaProviderSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.schemaProviderSupplier = context.getService( SchemaProvider.class );
    }

    public Object execute( final String query, final ScriptValue variables )
    {
        return schemaProviderSupplier.get().execute( query, variables == null ? Map.of() : variables.getMap() );
    }

    public void invalidate()
    {
        schemaProviderSupplier.get().invalidate();
    }
}
