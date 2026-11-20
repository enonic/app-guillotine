package com.enonic.app.guillotine.mcp;

import java.util.function.Supplier;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class McpToolsBean
    implements ScriptBean
{
    private Supplier<ServiceFacade> serviceFacadeSupplier;

    private Supplier<GuillotineConfigService> configServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.serviceFacadeSupplier = context.getService( ServiceFacade.class );
        this.configServiceSupplier = context.getService( GuillotineConfigService.class );
    }

    /**
     * Runs the tool {@code name} of {@code group} with the JSON object {@code argsJson} and returns the MCP result as JSON.
     */
    public String run( final String group, final String name, final String argsJson )
    {
        return McpJson.write( new McpTools( serviceFacadeSupplier.get(), configServiceSupplier.get() ).run( group, name, argsJson ) );
    }
}
