package com.enonic.app.guillotine.graphql;

import java.util.List;

import com.enonic.xp.macro.MacroDescriptors;
import com.enonic.xp.region.ComponentDescriptor;
import com.enonic.xp.schema.mixin.MixinDescriptors;

public interface ComponentDescriptorService
{
    List<ComponentDescriptor> getComponentDescriptors( String componentType, String applicationKey );

    MacroDescriptors getMacroDescriptors( List<String> applicationKeys );

    MixinDescriptors getMixins( String applicationKey );
}
