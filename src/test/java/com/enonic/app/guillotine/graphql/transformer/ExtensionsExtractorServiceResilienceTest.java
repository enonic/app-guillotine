package com.enonic.app.guillotine.graphql.transformer;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import graphql.Scalars;

import com.enonic.xp.app.Application;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.app.Applications;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.ScriptExports;
import com.enonic.xp.script.ScriptValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtensionsExtractorServiceResilienceTest
{
    private static final ApplicationKey BAD_APP = ApplicationKey.from( "badapp" );

    private static final ApplicationKey GOOD_APP = ApplicationKey.from( "goodapp" );

    private static final String GOOD_TYPE = "GoodType";

    @Test
    public void testInvalidExtensionsOfOneApplicationDoNotBreakOthers()
    {
        final Applications applications = Applications.from( application( BAD_APP ), application( GOOD_APP ) );
        final ApplicationService applicationService = mock( ApplicationService.class );
        when( applicationService.getInstalledApplications() ).thenReturn( applications );

        final Resource resource = mock( Resource.class );
        when( resource.exists() ).thenReturn( true );
        final ResourceService resourceService = mock( ResourceService.class );
        when( resourceService.getResource( any() ) ).thenReturn( resource );

        final Map<ApplicationKey, ScriptExports> exportsByApp =
            Map.of( BAD_APP, scriptExports( brokenExtensions() ), GOOD_APP, scriptExports( goodExtensions() ) );

        final PortalScriptService scriptService = mock( PortalScriptService.class );
        when( scriptService.execute( any() ) ).thenAnswer( invocation -> {
            final ResourceKey resourceKey = invocation.getArgument( 0 );
            return exportsByApp.get( resourceKey.getApplicationKey() );
        } );

        final ExtensionsExtractorService service = new ExtensionsExtractorService( applicationService, resourceService, scriptService );

        final SchemaExtensions schemaExtensions = service.extractSchemaExtensions();

        assertEquals( Set.of( GOOD_TYPE ), schemaExtensions.getTypes().keySet() );
        assertTrue( schemaExtensions.getInputTypes().isEmpty() );
        assertTrue( schemaExtensions.getEnums().isEmpty() );
        assertTrue( schemaExtensions.getUnions().isEmpty() );
        assertTrue( schemaExtensions.getInterfaces().isEmpty() );
    }

    private static ScriptExports scriptExports( final ScriptValue extensions )
    {
        final ScriptExports exports = mock( ScriptExports.class );
        when( exports.hasMethod( "extensions" ) ).thenReturn( true );
        when( exports.executeMethod( eq( "extensions" ), any() ) ).thenReturn( extensions );
        return exports;
    }

    private static Application application( final ApplicationKey key )
    {
        final Application application = mock( Application.class );
        when( application.getKey() ).thenReturn( key );
        return application;
    }

    /**
     * {@code types} is not an object, which makes {@link GraphQLExtensionValidator#validateTypeDefs} throw.
     */
    private static ScriptValue brokenExtensions()
    {
        final ScriptValue types = mock( ScriptValue.class );
        when( types.isObject() ).thenReturn( false );

        final ScriptValue extensions = mock( ScriptValue.class );
        when( extensions.isObject() ).thenReturn( true );
        when( extensions.getMember( "types" ) ).thenReturn( types );
        return extensions;
    }

    private static ScriptValue goodExtensions()
    {
        final ScriptValue typeDef = goodOutputTypeDef();

        final ScriptValue types = mock( ScriptValue.class );
        when( types.isObject() ).thenReturn( true );
        when( types.getKeys() ).thenReturn( Set.of( GOOD_TYPE ) );
        when( types.getMember( GOOD_TYPE ) ).thenReturn( typeDef );

        final ScriptValue extensions = mock( ScriptValue.class );
        when( extensions.isObject() ).thenReturn( true );
        when( extensions.getMember( "types" ) ).thenReturn( types );
        return extensions;
    }

    private static ScriptValue goodOutputTypeDef()
    {
        final ScriptValue description = mock( ScriptValue.class );
        when( description.isValue() ).thenReturn( true );
        when( description.getValue() ).thenReturn( "Good output" );

        final ScriptValue typeMember = mock( ScriptValue.class );
        when( typeMember.getValue() ).thenReturn( Scalars.GraphQLString );

        final ScriptValue nameField = mock( ScriptValue.class );
        when( nameField.isObject() ).thenReturn( true );
        when( nameField.getMember( "type" ) ).thenReturn( typeMember );
        when( nameField.getMember( "args" ) ).thenReturn( null );

        final ScriptValue fields = mock( ScriptValue.class );
        when( fields.isObject() ).thenReturn( true );
        when( fields.getKeys() ).thenReturn( Set.of( "name" ) );
        when( fields.getMember( "name" ) ).thenReturn( nameField );

        final ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        when( typeDef.getMember( "description" ) ).thenReturn( description );
        when( typeDef.getMember( "fields" ) ).thenReturn( fields );
        return typeDef;
    }
}
