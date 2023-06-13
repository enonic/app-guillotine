package com.enonic.app.guillotine.graphql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.BuiltinContentTypes;
import com.enonic.app.guillotine.BuiltinMacros;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.factory.TestFixtures;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.transformer.ExtensionsExtractorService;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.xp.app.Application;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.app.Applications;
import com.enonic.xp.config.ConfigBuilder;
import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeConfig;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.inputtype.InputTypeProperty;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.xdata.XDatas;
import com.enonic.xp.testing.ScriptTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphQLApiIT
    extends ScriptTestSupport
{
    private GraphQLApi bean;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        final BundleContext bundleContext = Mockito.mock( BundleContext.class );

        final Bundle bundle = Mockito.mock( Bundle.class );
        Mockito.when( bundle.getBundleContext() ).thenReturn( bundleContext );

        final Application application = Mockito.mock( Application.class );
        Mockito.when( application.getKey() ).thenReturn( ApplicationKey.from( "myapplication" ) );
        Mockito.when( application.getVersion() ).thenReturn( Version.emptyVersion );
        Mockito.when( application.getBundle() ).thenReturn( bundle );
        Mockito.when( application.getClassLoader() ).thenReturn( getClass().getClassLoader() );
        Mockito.when( application.isStarted() ).thenReturn( true );
        Mockito.when( application.getConfig() ).thenReturn( ConfigBuilder.create().build() );

        final ApplicationService applicationService = Mockito.mock( ApplicationService.class );

        final Applications applications = Applications.from( application );
        Mockito.when( applicationService.getInstalledApplications() ).thenReturn( applications );

        Field resourceServiceField = getClass().getSuperclass().getDeclaredField( "resourceService" );
        resourceServiceField.setAccessible( true );
        ResourceService resourceService = (ResourceService) resourceServiceField.get( this );

        final PortalScriptService scriptService = Mockito.mock( PortalScriptService.class );

        Mockito.when( scriptService.execute( Mockito.any() ) ).thenAnswer( invocation -> {
            ResourceKey resourceKey = (ResourceKey) invocation.getArguments()[0];
            return runScript( resourceKey );
        } );

        ExtensionsExtractorService extensionsExtractorService =
            new ExtensionsExtractorService( applicationService, resourceService, scriptService );

        addService( ExtensionsExtractorService.class, extensionsExtractorService );
        addService( ApplicationService.class, applicationService );

        ServiceFacade serviceFacade = Mockito.mock( ServiceFacade.class );

        ComponentDescriptorService componentDescriptorService = Mockito.mock( ComponentDescriptorService.class );
        ContentTypeService contentTypeService = Mockito.mock( ContentTypeService.class );

        Mockito.when( componentDescriptorService.getMacroDescriptors( Mockito.anyList() ) ).thenReturn(
            BuiltinMacros.getSystemMacroDescriptors() );

        Mockito.when( componentDescriptorService.getExtraData( Mockito.anyString() ) ).thenReturn(
            XDatas.from( TestFixtures.createGpsInfo() ) );

        Mockito.when( contentTypeService.getAll() ).thenReturn( createContentTypes() );

        Mockito.when( serviceFacade.getComponentDescriptorService() ).thenReturn( componentDescriptorService );
        Mockito.when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        addService( ServiceFacade.class, serviceFacade );

        this.bean = new GraphQLApi();
        this.bean.initialize( newBeanContext( ResourceKey.from( "myapplication:/test" ) ) );
    }

    @Test
    public void test()
    {
        GraphQLSchema graphQLSchema = this.bean.createSchema();

        assertNotNull( graphQLSchema );

        GraphQLObjectType queryType = graphQLSchema.getQueryType();

        assertNotNull( queryType.getFieldDefinition( "customField" ) );

        ExecutionResultMapper executionResultMapper =
            (ExecutionResultMapper) bean.execute( graphQLSchema, "query { customField }", null, null );

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        executionResultMapper.serialize( generator );

        Map<String, Object> result = CastHelper.cast( generator.getRoot() );

        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> data = CastHelper.cast( result.get( "data" ) );

        assertTrue( data.containsKey( "customField" ) );

        assertEquals( ArrayHelper.forceArray( "Static value undefined" ), data.get( "customField" ) );
    }

    private ContentTypes createContentTypes()
    {
        FieldSet fieldSet = FieldSet.create().label( "My layout" ).name( "myLayout" ).addFormItem(
            FormItemSet.create().name( "mySet" ).required( true ).addFormItem(
                Input.create().name( "myInput" ).label( "Input" ).inputType( InputTypeName.TEXT_LINE ).build() ).build() ).build();

        ContentType myContentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( "com.enonic.app.testapp:my_type" ).addFormItem(
                fieldSet ).addFormItem( Input.create().name( "dateOfBirth" ).label( "Birth of Date" ).occurrences( 0, 1 ).inputType(
                InputTypeName.DATE ).build() ).addFormItem(
                FormOptionSet.create().name( "blocks" ).occurrences( Occurrences.create( 1, 1 ) ).addOptionSetOption(
                    FormOptionSetOption.create().name( "text" ).addFormItem(
                        Input.create().name( "text" ).label( "Text" ).occurrences( 1, 1 ).inputType(
                            InputTypeName.HTML_AREA ).build() ).build() ).addOptionSetOption(
                    FormOptionSetOption.create().name( "icon" ).addFormItem(
                        Input.create().name( "icon" ).label( "Icon" ).occurrences( 1, 1 ).inputType(
                            InputTypeName.ATTACHMENT_UPLOADER ).build() ).build() ).build() ).addFormItem(
                FormItemSet.create().name( "cast" ).label( "Cast" ).occurrences( 0, 0 ).addFormItem(
                    Input.create().name( "actor" ).label( "Actor" ).occurrences( 1, 1 ).inputType(
                        InputTypeName.CONTENT_SELECTOR ).inputTypeConfig( InputTypeConfig.create().property(
                        InputTypeProperty.create( "allowContentType", "person" ).build() ).build() ).build() ).addFormItem(
                    Input.create().name( "abstract" ).label( "Abstract" ).occurrences( 1, 1 ).inputType(
                        InputTypeName.TEXT_AREA ).build() ).addFormItem(
                    Input.create().name( "photos" ).label( "Photos" ).occurrences( 0, 0 ).inputType(
                        InputTypeName.IMAGE_SELECTOR ).build() ).build() ).build();

        List<ContentType> types = new ArrayList<>( BuiltinContentTypes.getAll() );
        types.add( myContentType );

        return ContentTypes.from( types );
    }
}
