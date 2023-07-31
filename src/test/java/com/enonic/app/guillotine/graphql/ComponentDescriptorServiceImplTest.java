package com.enonic.app.guillotine.graphql;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationKeys;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.macro.MacroDescriptorService;
import com.enonic.xp.macro.MacroDescriptors;
import com.enonic.xp.macro.MacroKey;
import com.enonic.xp.page.DescriptorKey;
import com.enonic.xp.page.PageDescriptor;
import com.enonic.xp.page.PageDescriptorService;
import com.enonic.xp.page.PageDescriptors;
import com.enonic.xp.region.ComponentDescriptor;
import com.enonic.xp.region.LayoutDescriptorService;
import com.enonic.xp.region.LayoutDescriptors;
import com.enonic.xp.region.PartDescriptorService;
import com.enonic.xp.region.PartDescriptors;
import com.enonic.xp.region.RegionDescriptor;
import com.enonic.xp.region.RegionDescriptors;
import com.enonic.xp.schema.xdata.XData;
import com.enonic.xp.schema.xdata.XDataName;
import com.enonic.xp.schema.xdata.XDataService;
import com.enonic.xp.schema.xdata.XDatas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComponentDescriptorServiceImplTest
{
    private PartDescriptorService partDescriptorService;

    private LayoutDescriptorService layoutDescriptorService;

    private PageDescriptorService pageDescriptorService;

    private MacroDescriptorService macroDescriptorService;

    private XDataService xDataService;

    ComponentDescriptorServiceImpl instance;

    @BeforeEach
    public void setUp()
    {
        this.layoutDescriptorService = Mockito.mock( LayoutDescriptorService.class );
        this.partDescriptorService = Mockito.mock( PartDescriptorService.class );
        this.pageDescriptorService = Mockito.mock( PageDescriptorService.class );
        this.macroDescriptorService = Mockito.mock( MacroDescriptorService.class );
        this.xDataService = Mockito.mock( XDataService.class );

        this.instance = new ComponentDescriptorServiceImpl( partDescriptorService, layoutDescriptorService, pageDescriptorService,
                                                            macroDescriptorService, xDataService );
    }

    @Test
    public void testGetComponentDescriptors()
    {
        PageDescriptors pageDescriptors = PageDescriptors.from( PageDescriptor.create().regions(
            RegionDescriptors.create().add( RegionDescriptor.create().name( "main" ).build() ).build() ).key(
            DescriptorKey.from( "custom:descriptor" ) ).config( Form.create().build() ).build() );

        Mockito.when( pageDescriptorService.getByApplication( Mockito.any() ) ).thenReturn( pageDescriptors );

        List<ComponentDescriptor> descriptors = instance.getComponentDescriptors( "Page", "applicationKey" );
        assertEquals( 1, descriptors.size() );
        assertEquals( "descriptor", descriptors.get( 0 ).getName() );

        Mockito.when( partDescriptorService.getByApplication( Mockito.any() ) ).thenReturn( PartDescriptors.empty() );
        descriptors = instance.getComponentDescriptors( "Part", "applicationKey" );
        assertTrue( descriptors.isEmpty() );

        Mockito.when( layoutDescriptorService.getByApplication( Mockito.any() ) ).thenReturn( LayoutDescriptors.empty() );
        descriptors = instance.getComponentDescriptors( "Layout", "applicationKey" );
        assertTrue( descriptors.isEmpty() );

        IllegalArgumentException exception =
            assertThrows( IllegalArgumentException.class, () -> instance.getComponentDescriptors( "Unknown", "applicationKey" ) );

        assertEquals( "Unknown component type: Unknown", exception.getMessage() );
    }

    @Test
    public void testGetMacroDescriptors()
    {
        MacroDescriptors macroDescriptors =
            MacroDescriptors.from( MacroDescriptor.create().key( MacroKey.from( "myapplication:mymacro" ) ).build() );

        Mockito.when( macroDescriptorService.getByApplications( Mockito.any( ApplicationKeys.class ) ) ).thenReturn( macroDescriptors );

        assertEquals( macroDescriptors, instance.getMacroDescriptors( List.of( "myapplication" ) ) );
    }

    @Test
    public void testGetExtraData()
    {
        ApplicationKey applicationKey = ApplicationKey.from( "myapplication" );

        XDatas xDatas = XDatas.create().add( XData.create().name( XDataName.from( applicationKey, "SoMe" ) ).form(
            Form.create().addFormItem(
                Input.create().inputType( InputTypeName.TEXT_LINE ).name( "twitter" ).label( "Twitter" ).minimumOccurrences(
                    0 ).maximumOccurrences( 1 ).build() ).build() ).build() ).build();

        Mockito.when( this.xDataService.getByApplication( Mockito.any( ApplicationKey.class ) ) ).thenReturn( xDatas );

        assertEquals( xDatas, instance.getExtraData( "myapplication" ) );
    }
}
