package com.enonic.app.guillotine.graphql.factory;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLObjectType;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.ComponentDescriptorService;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.macro.MacroDescriptors;
import com.enonic.xp.macro.MacroKey;
import com.enonic.xp.region.ComponentDescriptor;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.mixin.MixinService;
import com.enonic.xp.schema.xdata.XData;
import com.enonic.xp.schema.xdata.XDataName;
import com.enonic.xp.schema.xdata.XDatas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class FactoryResilienceTest
{
    private static final String APP_KEY = "com.enonic.app.testapp";

    private static final String BROKEN = "broken";

    private static final String GOOD_ONE = "goodOne";

    private static final String GOOD_TWO = "goodTwo";

    @Test
    public void testContentTypesFactorySkipsBrokenFormItem()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        ContentType contentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( APP_KEY + ":resilient_type" ).addFormItem(
                textLine( GOOD_ONE ) ).addFormItem( textLine( BROKEN ) ).addFormItem( textLine( GOOD_TWO ) ).build();

        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( contentType ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ContentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType dataType = context.getOutputType( "com_enonic_app_testapp_ResilientType_Data" );
        assertNotNull( dataType, "Data type for resilient_type must be registered even when one field fails" );

        assertNotNull( dataType.getFieldDefinition( GOOD_ONE ) );
        assertNotNull( dataType.getFieldDefinition( GOOD_TWO ) );
        assertNull( dataType.getFieldDefinition( BROKEN ) );

        assertNoFetcherFor( context, dataType.getName(), BROKEN );
    }

    @Test
    public void testComponentTypesFactorySkipsBrokenFormItem()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ComponentDescriptorService componentDescriptorService = mock( ComponentDescriptorService.class );
        when( serviceFacade.getComponentDescriptorService() ).thenReturn( componentDescriptorService );

        ComponentDescriptor partDescriptor = mock( ComponentDescriptor.class );
        when( partDescriptor.getName() ).thenReturn( "mypart" );
        when( partDescriptor.getConfig() ).thenReturn(
            Form.create().addFormItem( textLine( GOOD_ONE ) ).addFormItem( textLine( BROKEN ) ).addFormItem(
                textLine( GOOD_TWO ) ).build() );

        when( componentDescriptorService.getComponentDescriptors( anyString(), anyString() ) ).thenReturn( List.of() );
        when( componentDescriptorService.getComponentDescriptors( eq( "Part" ), eq( APP_KEY ) ) ).thenReturn( List.of( partDescriptor ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ComponentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType descriptorType = context.getOutputType( "Part_com_enonic_app_testapp_mypart" );
        assertNotNull( descriptorType, "Part descriptor config type must be registered even when one field fails" );

        assertNotNull( descriptorType.getFieldDefinition( GOOD_ONE ) );
        assertNotNull( descriptorType.getFieldDefinition( GOOD_TWO ) );
        assertNull( descriptorType.getFieldDefinition( BROKEN ) );

        assertNoFetcherFor( context, descriptorType.getName(), BROKEN );
    }

    @Test
    public void testMacroTypesFactorySkipsBrokenFormItem()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ComponentDescriptorService componentDescriptorService = mock( ComponentDescriptorService.class );
        when( serviceFacade.getComponentDescriptorService() ).thenReturn( componentDescriptorService );

        MacroDescriptor macroDescriptor = MacroDescriptor.create().key( MacroKey.from( APP_KEY + ":mymacro" ) ).form(
            Form.create().addFormItem( textLine( GOOD_ONE ) ).addFormItem( textLine( BROKEN ) ).addFormItem(
                textLine( GOOD_TWO ) ).build() ).build();

        when( componentDescriptorService.getMacroDescriptors( anyList() ) ).thenReturn( MacroDescriptors.from( macroDescriptor ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new MacroTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType macroDataConfigType = context.getOutputType( "Macro_com_enonic_app_testapp_mymacro_DataConfig" );
        assertNotNull( macroDataConfigType, "Macro DataConfig type must be registered even when one field fails" );

        assertNotNull( macroDataConfigType.getFieldDefinition( "body" ) );
        assertNotNull( macroDataConfigType.getFieldDefinition( GOOD_ONE ) );
        assertNotNull( macroDataConfigType.getFieldDefinition( GOOD_TWO ) );
        assertNull( macroDataConfigType.getFieldDefinition( BROKEN ) );

        assertNoFetcherFor( context, macroDataConfigType.getName(), BROKEN );
    }

    @Test
    public void testXDataTypesFactorySkipsBrokenFormItem()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ComponentDescriptorService componentDescriptorService = mock( ComponentDescriptorService.class );
        when( serviceFacade.getComponentDescriptorService() ).thenReturn( componentDescriptorService );

        XData mixinDescriptor = XData.create().name( XDataName.from( APP_KEY + ":resilientMixin" ) ).form(
            Form.create().addFormItem( textLine( GOOD_ONE ) ).addFormItem( textLine( BROKEN ) ).addFormItem(
                textLine( GOOD_TWO ) ).build() ).build();

        when( componentDescriptorService.getExtraData( anyString() ) ).thenReturn( XDatas.empty() );
        when( componentDescriptorService.getExtraData( APP_KEY ) ).thenReturn( XDatas.from( mixinDescriptor ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new XDataTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType mixinConfigType = context.getOutputType( "XData_com_enonic_app_testapp_resilientMixin_DataConfig" );
        assertNotNull( mixinConfigType, "XData config type must be registered even when one field fails" );

        assertNotNull( mixinConfigType.getFieldDefinition( GOOD_ONE ) );
        assertNotNull( mixinConfigType.getFieldDefinition( GOOD_TWO ) );
        assertNull( mixinConfigType.getFieldDefinition( BROKEN ) );

        assertNoFetcherFor( context, mixinConfigType.getName(), BROKEN );
    }

    @Test
    public void testContentTypesFactorySkipsDataWhenAllFormItemsBroken()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        ContentType contentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( APP_KEY + ":resilient_type" ).addFormItem(
                textLine( BROKEN ) ).build();

        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( contentType ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ContentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType contentObjectType = context.getOutputType( "com_enonic_app_testapp_ResilientType" );
        assertNotNull( contentObjectType, "Content type itself must be registered" );
        assertNull( contentObjectType.getFieldDefinition( "data" ), "data field must be skipped when all form items are broken" );
        assertNull( context.getOutputType( "com_enonic_app_testapp_ResilientType_Data" ),
                    "data type must not be registered when all form items are broken" );
    }

    @Test
    public void testFormItemSetIsSkippedWhenAllInnerItemsBroken()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        FormItemSet brokenSet = FormItemSet.create().name( "brokenSet" ).addFormItem( textLine( BROKEN ) ).build();
        ContentType contentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( APP_KEY + ":resilient_type" ).addFormItem(
                textLine( GOOD_ONE ) ).addFormItem( brokenSet ).build();

        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( contentType ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ContentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType dataType = context.getOutputType( "com_enonic_app_testapp_ResilientType_Data" );
        assertNotNull( dataType );
        assertNotNull( dataType.getFieldDefinition( GOOD_ONE ) );
        assertNull( dataType.getFieldDefinition( "brokenSet" ), "FormItemSet that ends up empty must be skipped from parent data type" );
        assertNull( context.getOutputType( "com_enonic_app_testapp_ResilientType_BrokenSet" ),
                    "Empty FormItemSet must not be registered as a GraphQL type" );
    }

    @Test
    public void testFormOptionSetIsSkippedWhenAllOptionsBroken()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        FormOptionSet allBrokenOptionSet =
            FormOptionSet.create().name( "blocks" ).occurrences( Occurrences.create( 1, 1 ) ).addOptionSetOption(
                FormOptionSetOption.create().name( BROKEN ).addFormItem( textLine( "inner" ) ).build() ).build();

        ContentType contentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( APP_KEY + ":resilient_type" ).addFormItem(
                textLine( GOOD_ONE ) ).addFormItem( allBrokenOptionSet ).build();

        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( contentType ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ContentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType dataType = context.getOutputType( "com_enonic_app_testapp_ResilientType_Data" );
        assertNotNull( dataType );
        assertNotNull( dataType.getFieldDefinition( GOOD_ONE ) );
        assertNull( dataType.getFieldDefinition( "blocks" ),
                    "FormOptionSet with empty enum (all options broken) must be skipped from parent data type" );
        assertNull( context.getOutputType( "com_enonic_app_testapp_ResilientType_Blocks" ),
                    "FormOptionSet with empty enum must not be registered as a GraphQL type" );
        assertNull( context.getOutputType( "com_enonic_app_testapp_ResilientType_Blocks_OptionEnum" ),
                    "Empty option set enum must not be registered as a GraphQL type" );
    }

    @Test
    public void testFormItemSetSkipsBrokenInnerFormItem()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        FormItemSet mySet = FormItemSet.create().name( "mySet" ).required( true ).addFormItem( textLine( GOOD_ONE ) ).addFormItem(
            textLine( BROKEN ) ).addFormItem( textLine( GOOD_TWO ) ).build();

        ContentType contentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( APP_KEY + ":resilient_type" ).addFormItem( mySet ).build();

        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( contentType ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ContentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType mySetType = context.getOutputType( "com_enonic_app_testapp_ResilientType_MySet" );
        assertNotNull( mySetType, "FormItemSet type must be registered even when one inner field fails" );

        assertNotNull( mySetType.getFieldDefinition( GOOD_ONE ) );
        assertNotNull( mySetType.getFieldDefinition( GOOD_TWO ) );
        assertNull( mySetType.getFieldDefinition( BROKEN ) );

        assertNoFetcherFor( context, mySetType.getName(), BROKEN );
    }

    @Test
    public void testFormOptionSetSkipsBrokenOption()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        FormOptionSet blocks = FormOptionSet.create().name( "blocks" ).occurrences( Occurrences.create( 1, 1 ) ).addOptionSetOption(
            FormOptionSetOption.create().name( "text" ).addFormItem(
                Input.create().name( "text" ).label( "Text" ).inputType( InputTypeName.HTML_AREA ).build() ).build() ).addOptionSetOption(
            FormOptionSetOption.create().name( BROKEN ).addFormItem( textLine( "innerText" ) ).build() ).addOptionSetOption(
            FormOptionSetOption.create().name( "icon" ).addFormItem(
                Input.create().name( "icon" ).label( "Icon" ).inputType( InputTypeName.ATTACHMENT_UPLOADER ).build() ).build() ).build();

        ContentType contentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( APP_KEY + ":resilient_type" ).addFormItem(
                blocks ).build();

        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( contentType ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ContentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType blocksType = context.getOutputType( "com_enonic_app_testapp_ResilientType_Blocks" );
        assertNotNull( blocksType, "FormOptionSet type must be registered even when one option fails" );

        assertNotNull( blocksType.getFieldDefinition( "_selected" ) );
        assertNotNull( blocksType.getFieldDefinition( "text" ) );
        assertNotNull( blocksType.getFieldDefinition( "icon" ) );
        assertNull( blocksType.getFieldDefinition( BROKEN ) );

        assertNoFetcherFor( context, blocksType.getName(), BROKEN );

        GraphQLObjectType brokenOptionType = context.getOutputType( "com_enonic_app_testapp_ResilientType_Broken" );
        assertNull( brokenOptionType, "Broken option must not produce its own object type" );
    }

    @Test
    public void testFormOptionSetOptionSkipsBrokenInnerFormItem()
    {
        ServiceFacade serviceFacade = newServiceFacade();
        ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        FormOptionSet blocks = FormOptionSet.create().name( "blocks" ).occurrences( Occurrences.create( 1, 1 ) ).addOptionSetOption(
            FormOptionSetOption.create().name( "text" ).addFormItem( textLine( GOOD_ONE ) ).addFormItem( textLine( BROKEN ) ).addFormItem(
                textLine( GOOD_TWO ) ).build() ).build();

        ContentType contentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( APP_KEY + ":resilient_type" ).addFormItem(
                blocks ).build();

        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( contentType ) );

        GuillotineContext context = newContext();

        try (MockedStatic<StringNormalizer> ignored = mockBrokenNormalizer())
        {
            new ContentTypesFactory( context, serviceFacade ).create();
        }

        GraphQLObjectType textOptionType = context.getOutputType( "com_enonic_app_testapp_ResilientType_Text" );
        assertNotNull( textOptionType, "FormOptionSetOption type must be registered even when one inner field fails" );

        assertNotNull( textOptionType.getFieldDefinition( GOOD_ONE ) );
        assertNotNull( textOptionType.getFieldDefinition( GOOD_TWO ) );
        assertNull( textOptionType.getFieldDefinition( BROKEN ) );

        assertNoFetcherFor( context, textOptionType.getName(), BROKEN );
    }

    private static ServiceFacade newServiceFacade()
    {
        ServiceFacade serviceFacade = mock( ServiceFacade.class );
        MixinService mixinService = mock( MixinService.class );
        when( mixinService.inlineFormItems( any() ) ).thenReturn( null );
        when( serviceFacade.getMixinService() ).thenReturn( mixinService );
        return serviceFacade;
    }

    private static GuillotineContext newContext()
    {
        return GuillotineContext.create().addApplications( List.of( APP_KEY ) ).build();
    }

    private static Input textLine( String name )
    {
        return Input.create().name( name ).label( name ).inputType( InputTypeName.TEXT_LINE ).build();
    }

    private static MockedStatic<StringNormalizer> mockBrokenNormalizer()
    {
        return mockStatic( StringNormalizer.class, invocation -> {
            Object[] args = invocation.getArguments();
            if ( args.length == 1 && BROKEN.equals( args[0] ) )
            {
                throw new RuntimeException( "Simulated failure for '" + BROKEN + "'" );
            }
            return invocation.callRealMethod();
        } );
    }

    private static void assertNoFetcherFor( GuillotineContext context, String typeName, String fieldName )
    {
        boolean present = context.getDataFetchers().keySet().stream().anyMatch(
            ( FieldCoordinates fc ) -> typeName.equals( fc.getTypeName() ) && fieldName.equals( fc.getFieldName() ) );
        assertFalse( present, "No DataFetcher should be registered for the broken field" );
    }
}
