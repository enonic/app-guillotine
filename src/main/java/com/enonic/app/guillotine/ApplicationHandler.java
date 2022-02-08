package com.enonic.app.guillotine;

import java.util.function.Supplier;

import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class ApplicationHandler
    implements ScriptBean
{
    private Supplier<ApplicationService> applicationServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.applicationServiceSupplier = context.getService( ApplicationService.class );
    }

    public Object getInstalledApplicationKeys()
    {
        return new ApplicationsMapper(applicationServiceSupplier.get().getInstalledApplicationKeys());
    }
}
