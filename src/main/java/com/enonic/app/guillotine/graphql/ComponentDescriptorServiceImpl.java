package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationKeys;
import com.enonic.xp.macro.MacroDescriptorService;
import com.enonic.xp.macro.MacroDescriptors;
import com.enonic.xp.page.PageDescriptorService;
import com.enonic.xp.region.ComponentDescriptor;
import com.enonic.xp.region.LayoutDescriptorService;
import com.enonic.xp.region.PartDescriptorService;
import com.enonic.xp.schema.xdata.XDataService;
import com.enonic.xp.schema.xdata.XDatas;

@Component(immediate = true, service = ComponentDescriptorService.class)
public class ComponentDescriptorServiceImpl
    implements ComponentDescriptorService
{
    private final PartDescriptorService partDescriptorService;

    private final LayoutDescriptorService layoutDescriptorService;

    private final PageDescriptorService pageDescriptorService;

    private final MacroDescriptorService macroDescriptorService;

    private final XDataService xDataService;

    @Activate
    public ComponentDescriptorServiceImpl( final @Reference PartDescriptorService partDescriptorService,
                                           final @Reference LayoutDescriptorService layoutDescriptorService,
                                           final @Reference PageDescriptorService pageDescriptorService,
                                           final @Reference MacroDescriptorService macroDescriptorService,
                                           final @Reference XDataService xDataService )
    {
        this.partDescriptorService = partDescriptorService;
        this.layoutDescriptorService = layoutDescriptorService;
        this.pageDescriptorService = pageDescriptorService;
        this.macroDescriptorService = macroDescriptorService;
        this.xDataService = xDataService;
    }

    @Override
    public List<ComponentDescriptor> getComponentDescriptors( final String componentType, final String applicationKey )
    {
        switch ( componentType )
        {
            case "Page":
                return pageDescriptorService.getByApplication( ApplicationKey.from( applicationKey ) ).stream().map(
                    pageDescriptor -> (ComponentDescriptor) pageDescriptor ).collect( Collectors.toList() );
            case "Part":
                return partDescriptorService.getByApplication( ApplicationKey.from( applicationKey ) ).stream().map(
                    pageDescriptor -> (ComponentDescriptor) pageDescriptor ).collect( Collectors.toList() );
            case "Layout":
                return layoutDescriptorService.getByApplication( ApplicationKey.from( applicationKey ) ).stream().map(
                    pageDescriptor -> (ComponentDescriptor) pageDescriptor ).collect( Collectors.toList() );
            default:
                throw new IllegalArgumentException( String.format( "Unknown component type: %s", componentType ) );
        }
    }

    @Override
    public MacroDescriptors getMacroDescriptors( final List<String> applicationKeys )
    {
        List<ApplicationKey> keys =
            Objects.requireNonNullElse( applicationKeys, new ArrayList<String>() ).stream().map( ApplicationKey::from ).collect(
                Collectors.toList() );
        keys.add( ApplicationKey.SYSTEM );

        return macroDescriptorService.getByApplications( ApplicationKeys.from( keys ) );
    }

    @Override
    public XDatas getExtraData( final String applicationKey )
    {
        return xDataService.getByApplication( ApplicationKey.from( applicationKey ) );
    }
}
