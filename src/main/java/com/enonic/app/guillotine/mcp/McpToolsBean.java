package com.enonic.app.guillotine.mcp;

import java.util.function.Supplier;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class McpToolsBean
    implements ScriptBean
{
    private Supplier<ServiceFacade> serviceFacadeSupplier;

    private Supplier<GuillotineConfigService> configServiceSupplier;

    private Supplier<SchemaProvider> schemaProviderSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.serviceFacadeSupplier = context.getService( ServiceFacade.class );
        this.configServiceSupplier = context.getService( GuillotineConfigService.class );
        this.schemaProviderSupplier = context.getService( SchemaProvider.class );
    }

    public String run( final String name, final Object args )
    {
        final McpTools tools = new McpTools( serviceFacadeSupplier.get(), configServiceSupplier.get(), schemaProviderSupplier.get() );
        return McpJson.write( tools.run( name, argsJson( args ) ) );
    }

    /**
     * {@code args} is the JSON of a JS object, or a scalar script value holding a Java map the engine left unconverted.
     */
    static String argsJson( final Object args )
    {
        return args instanceof String json ? json : McpJson.write( ( (ScriptValue) args ).getValue() );
    }
}
