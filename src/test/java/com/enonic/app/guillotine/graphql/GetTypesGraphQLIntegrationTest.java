package com.enonic.app.guillotine.graphql;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.BuiltinContentTypes;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.content.GetContentTypeParams;

import static com.enonic.app.guillotine.graphql.ResourceHelper.readGraphQLQuery;
import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class GetTypesGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    private ContentTypeService contentTypeService;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        contentTypeService = Mockito.mock( ContentTypeService.class );
        addService( ContentTypeService.class, contentTypeService );
    }

    @Test
    public void testGetTypesField()
    {
        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( BuiltinContentTypes.getAll() ) );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/getContentTypes.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        List<Map<String, Object>> getTypesField = CastHelper.cast( getFieldFromGuillotine( result, "getTypes" ) );

        getTypesField.forEach( getTypeField -> {
            if ( Objects.equals( "portal:site", getTypeField.get( "name" ) ) )
            {
                assertEquals( "Site", getTypeField.get( "displayName" ) );
                assertEquals( "Root content for sites", getTypeField.get( "description" ) );
                assertEquals( "base:structured", getTypeField.get( "superType" ) );
                assertEquals( false, getTypeField.get( "abstract" ) );
                assertEquals( true, getTypeField.get( "final" ) );
                assertEquals( true, getTypeField.get( "allowChildContent" ) );
                assertNull( getTypeField.get( "contentDisplayNameScript" ) );
            }
        } );
    }

    @Test
    public void testGetTypeField()
    {
        when( contentTypeService.getAll() ).thenReturn( ContentTypes.from( BuiltinContentTypes.getAll() ) );
        when( contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.site() ) ) ).thenReturn(
            BuiltinContentTypes.getContentType( ContentTypeName.site() ) );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/getType.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> getTypeField = CastHelper.cast( getFieldFromGuillotine( result, "getType" ) );

        assertEquals( "portal:site", getTypeField.get( "name" ) );
        assertEquals( "Site", getTypeField.get( "displayName" ) );
        assertEquals( "Root content for sites", getTypeField.get( "description" ) );
        assertEquals( "base:structured", getTypeField.get( "superType" ) );
        assertEquals( false, getTypeField.get( "abstract" ) );
        assertEquals( true, getTypeField.get( "final" ) );
        assertEquals( true, getTypeField.get( "allowChildContent" ) );
        assertNull( getTypeField.get( "contentDisplayNameScript" ) );
        assertNotNull( getTypeField.get( "formAsJson" ) );

        List<Map<String, Object>> formField = CastHelper.cast( getTypeField.get( "form" ) );
        assertEquals( 2, formField.size() );

        Map<String, Object> formItem_1 = CastHelper.cast( formField.get( 0 ) );

        assertEquals( "Input", formItem_1.get( "formItemType" ) );
        assertEquals( "description", formItem_1.get( "name" ) );
        assertEquals( "Description", formItem_1.get( "label" ) );

        Map<String, Object> formItem_2 = CastHelper.cast( formField.get( 1 ) );

        assertEquals( "Input", formItem_2.get( "formItemType" ) );
        assertEquals( "siteConfig", formItem_2.get( "name" ) );
        assertEquals( "Applications", formItem_2.get( "label" ) );
    }

    @Test
    public void testGetPermissionsField()
    {
        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( ContentFixtures.createMediaContent() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/getPermissionsField.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> getPermissionsField = CastHelper.cast( getFieldFromGuillotine( result, "getPermissions" ) );

        assertEquals( 2, getPermissionsField.size() );

        assertTrue( (boolean) getPermissionsField.get( "inheritsPermissions" ) );

        List<Map<String, Object>> permissionsEntries = CastHelper.cast( getPermissionsField.get( "permissions" ) );

        assertEquals( 2, permissionsEntries.size() );
        permissionsEntries.forEach( entry -> {
            Map<String, String> principal = CastHelper.cast( entry.get( "principal" ) );
            if ( Objects.equals( "role:cms.admin", principal.get( "value" ) ) )
            {
                List<String> allow = CastHelper.cast( entry.get( "allow" ) );
                assertEquals( 7, allow.size() );
                List<String> deny = CastHelper.cast( entry.get( "deny" ) );
                assertEquals( 0, deny.size() );
            }
            else
            {
                List<String> allow = CastHelper.cast( entry.get( "allow" ) );
                assertEquals( 3, allow.size() );
                List<String> deny = CastHelper.cast( entry.get( "deny" ) );
                assertEquals( 2, deny.size() );
            }
        } );
    }
}
