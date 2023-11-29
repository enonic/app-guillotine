package com.enonic.app.guillotine;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.resource.ResourceKey;

@Component(service = GuillotineScriptService.class)
public class GuillotineScriptService
{
    private static final ResourceKey GUILLOTINE_RESOURCE_KEY = ResourceKey.from( ApplicationKey.from( "com.enonic.app.guillotine" ), "/" );

    private final PortalScriptService portalScriptService;

    @Activate
    public GuillotineScriptService( final @Reference PortalScriptService portalScriptService )
    {
        this.portalScriptService = portalScriptService;
    }

    public Object toNativeObject( final Object object )
    {
        return portalScriptService.toNativeObject( GUILLOTINE_RESOURCE_KEY, object );
    }
}
