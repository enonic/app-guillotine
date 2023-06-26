package com.enonic.app.guillotine.graphql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.BuiltinContentTypes;
import com.enonic.app.guillotine.BuiltinMacros;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.factory.TestFixtures;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.transformer.ExtensionsExtractorService;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.xp.app.Application;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.app.Applications;
import com.enonic.xp.config.ConfigBuilder;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.xdata.XDatas;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.testing.ScriptTestSupport;

public class BaseGraphQLIntegrationTest
    extends ScriptTestSupport
{
    private GraphQLApi bean;

    protected ServiceFacade serviceFacade;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        final BundleContext bundleContext = Mockito.mock( BundleContext.class );

        final Bundle bundle = Mockito.mock( Bundle.class );
        Mockito.when( bundle.getBundleContext() ).thenReturn( bundleContext );

        final Application application = createApplication( bundle );

        final ApplicationService applicationService = Mockito.mock( ApplicationService.class );

        final Applications applications = Applications.from( application );
        Mockito.when( applicationService.getInstalledApplications() ).thenReturn( applications );

        final PortalScriptService scriptService = Mockito.mock( PortalScriptService.class );

        Mockito.when( scriptService.execute( Mockito.any() ) ).thenAnswer( invocation -> {
            ResourceKey resourceKey = (ResourceKey) invocation.getArguments()[0];
            return runScript( resourceKey );
        } );

        final ExtensionsExtractorService extensionsExtractorService =
            new ExtensionsExtractorService( applicationService, getResourceService(), scriptService );

        this.serviceFacade = Mockito.mock( ServiceFacade.class );

        final ComponentDescriptorService componentDescriptorService = Mockito.mock( ComponentDescriptorService.class );

        Mockito.when( componentDescriptorService.getMacroDescriptors( Mockito.anyList() ) ).thenReturn(
            BuiltinMacros.getSystemMacroDescriptors() );

        Mockito.when( componentDescriptorService.getExtraData( Mockito.anyString() ) ).thenReturn(
            XDatas.from( TestFixtures.CAMERA_METADATA, TestFixtures.IMAGE_METADATA, TestFixtures.GPS_METADATA ) );

        final ContentTypeService contentTypeService = Mockito.mock( ContentTypeService.class );

        Mockito.when( contentTypeService.getAll() ).thenReturn( createContentTypes() );

        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );

        Mockito.when( serviceFacade.getComponentDescriptorService() ).thenReturn( componentDescriptorService );
        Mockito.when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );
        Mockito.when( serviceFacade.getContentService() ).thenReturn( contentService );
        Mockito.when( serviceFacade.getPortalUrlService() ).thenReturn( portalUrlService );

        addService( ServiceFacade.class, serviceFacade );
        addService( ExtensionsExtractorService.class, extensionsExtractorService );
        addService( ApplicationService.class, applicationService );
        addService( PortalUrlService.class, portalUrlService );

        createGraphQLApiBean();
    }

    public GraphQLApi getBean()
    {
        return bean;
    }

    protected List<ContentType> getCustomContentTypes()
    {
        return new ArrayList<>();
    }

    protected Map<String, Object> executeQuery( final GraphQLSchema graphQLSchema, final String query )
    {
        ExecutionResultMapper executionResultMapper =
            createAdminContext().callWith( () -> (ExecutionResultMapper) bean.execute( graphQLSchema, query, null, null ) );

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        executionResultMapper.serialize( generator );

        return CastHelper.cast( generator.getRoot() );
    }

    protected Object getFieldFromGuillotine( final Map<String, Object> response, final String fieldName )
    {
        Map<String, Object> data = CastHelper.cast( response.get( "data" ) );
        Map<String, Object> guillotineField = CastHelper.cast( data.get( "guillotine" ) );
        return guillotineField.get( fieldName );
    }

    private void createGraphQLApiBean()
    {
        this.bean = new GraphQLApi();
        this.bean.initialize( newBeanContext( ResourceKey.from( "myapplication:/test" ) ) );
    }

    private ResourceService getResourceService()
        throws Exception
    {
        Field resourceServiceField = BaseGraphQLIntegrationTest.class.getSuperclass().getDeclaredField( "resourceService" );
        resourceServiceField.setAccessible( true );
        return (ResourceService) resourceServiceField.get( this );
    }

    private Application createApplication( final Bundle bundle )
    {
        final Application application = Mockito.mock( Application.class );

        Mockito.when( application.getKey() ).thenReturn( ApplicationKey.from( "myapplication" ) );
        Mockito.when( application.getVersion() ).thenReturn( Version.emptyVersion );
        Mockito.when( application.getBundle() ).thenReturn( bundle );
        Mockito.when( application.getClassLoader() ).thenReturn( getClass().getClassLoader() );
        Mockito.when( application.isStarted() ).thenReturn( true );
        Mockito.when( application.getConfig() ).thenReturn( ConfigBuilder.create().build() );

        return application;
    }

    private ContentTypes createContentTypes()
    {
        List<ContentType> types = new ArrayList<>( BuiltinContentTypes.getAll() );
        types.addAll( getCustomContentTypes() );
        return ContentTypes.from( types );
    }

    private Context createAdminContext()
    {
        return ContextBuilder.copyOf( ContextAccessor.current() ).authInfo(
            AuthenticationInfo.create().principals( RoleKeys.AUTHENTICATED, RoleKeys.ADMIN ).user( User.ANONYMOUS ).build() ).build();
    }

}
