package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeConfig;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.inputtype.InputTypeProperty;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.site.Site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GuillotineApiGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void testGenericContentFields()
    {
        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( ContentFixtures.createMediaContent() );
        when( contentService.getById( ContentId.from( "referenceId_1" ) ) ).thenReturn( ContentFixtures.createMediaContent() );
        when( contentService.getOutboundDependencies( Mockito.any( ContentId.class ) ) ).thenReturn( ContentIds.from( "referenceId_1" ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getContent.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( result, "get" ) );

        assertEquals( "contentId", getField.get( "_id" ) );
        assertEquals( "mycontent", getField.get( "_name" ) );
        assertEquals( "/a/b/mycontent", getField.get( "_path" ) );
        assertEquals( "My Content", getField.get( "displayName" ) );
        assertEquals( "en", getField.get( "language" ) );
        assertEquals( "media:image", getField.get( "type" ) );
        assertFalse( (boolean) getField.get( "hasChildren" ) );
        assertTrue( (boolean) getField.get( "valid" ) );
        assertNull( getField.get( "_score" ) );

        Map<String, Object> creatorField = CastHelper.cast( getField.get( "creator" ) );

        assertEquals( "user:system:admin", creatorField.get( "value" ) );
        assertEquals( "user", creatorField.get( "type" ) );
        assertEquals( "system", creatorField.get( "idProvider" ) );
        assertEquals( "admin", creatorField.get( "principalId" ) );

        Map<String, Object> modifierField = CastHelper.cast( getField.get( "modifier" ) );

        assertEquals( "user:system:admin", modifierField.get( "value" ) );
        assertEquals( "user", modifierField.get( "type" ) );
        assertEquals( "system", modifierField.get( "idProvider" ) );
        assertEquals( "admin", modifierField.get( "principalId" ) );

        Map<String, Object> ownerField = CastHelper.cast( getField.get( "owner" ) );

        assertEquals( "user:system:admin", ownerField.get( "value" ) );
        assertEquals( "user", ownerField.get( "type" ) );
        assertEquals( "system", ownerField.get( "idProvider" ) );
        assertEquals( "admin", ownerField.get( "principalId" ) );

        Map<String, Object> publishField = CastHelper.cast( getField.get( "publish" ) );

        assertEquals( "2016-11-03T10:00:00Z", publishField.get( "from" ) );
        assertEquals( "2016-11-23T10:00:00Z", publishField.get( "to" ) );
        assertNull( publishField.get( "first" ) );

        assertEquals( "1970-01-01T00:00:00.000Z", getField.get( "createdTime" ) );
        assertEquals( "1970-01-01T00:00:00.000Z", getField.get( "modifiedTime" ) );

        Map<String, Object> contentTypeField = CastHelper.cast( getField.get( "contentType" ) );
        assertNull( contentTypeField );

        Map<String, Object> dataAsJsonField = CastHelper.cast( getField.get( "dataAsJson" ) );
        assertNotNull( dataAsJsonField.get( "media" ) );

        List<Map<String, Object>> attachmentsField = CastHelper.cast( getField.get( "attachments" ) );
        assertEquals( 1, attachmentsField.size() );

        Map<String, Object> attachment = attachmentsField.get( 0 );

        assertEquals( "image.jpeg", attachment.get( "name" ) );
        assertEquals( "source", attachment.get( "label" ) );
        assertEquals( "image/jpeg", attachment.get( "mimeType" ) );

        Map<String, Object> xAsJsonField = CastHelper.cast( getField.get( "xAsJson" ) );
        assertEquals( 1, xAsJsonField.values().size() );

        final Map<String, Object> xMedia = CastHelper.cast( xAsJsonField.get( "media" ) );
        assertNotNull( xMedia );
        assertEquals( 1, xMedia.values().size() ); // empty application suffix

        Map<String, Object> xMediaAppConfig = CastHelper.cast( xMedia.get( "" ) );
        Map<String, Object> media = CastHelper.cast( xMediaAppConfig.get( "media" ) );

        final Map<String, Object> imageInfo = CastHelper.cast( media.get( "imageInfo" ) );
        assertEquals( 16036032, imageInfo.get( "pixelSize" ) );
        assertEquals( 3468, imageInfo.get( "imageHeight" ) );
        assertEquals( 3620112, imageInfo.get( "byteSize" ) );

        Map<String, Object> permissionsField = CastHelper.cast( getField.get( "permissions" ) );

        assertEquals( 2, permissionsField.size() );

        assertTrue( (boolean) permissionsField.get( "inheritsPermissions" ) );

        List<Map<String, Object>> permissionsEntries = CastHelper.cast( permissionsField.get( "permissions" ) );

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

    @Test
    public void testGetSiteField()
    {
        when( contentService.findNearestSiteByPath( any( ContentPath.class ) ) ).thenReturn(
            Site.create().name( "site" ).type( ContentTypeName.site() ).parentPath( ContentPath.ROOT ).data(
                new PropertyTree() ).displayName( "Site" ).id( ContentId.from( "siteId" ) ).build() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).build();

        ExecutionInput executionInput =
            ExecutionInput.newExecutionInput().query( ResourceHelper.readGraphQLQuery( "graphql/getSiteField.graphql" ) ).localContext(
                new HashMap<>() ).build();

        ExecutionResultMapper executionResultMapper = new ExecutionResultMapper( graphQL.execute( executionInput ) );

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        executionResultMapper.serialize( generator );

        Map<String, Object> response = CastHelper.cast( generator.getRoot() );

        assertFalse( response.containsKey( "errors" ) );
        assertTrue( response.containsKey( "data" ) );

        Map<String, Object> data = CastHelper.cast( response.get( "data" ) );

        assertTrue( data.containsKey( "getSite" ) );
        Map<String, Object> getSite = CastHelper.cast( data.get( "getSite" ) );
        Map<String, Object> getForGetSite = CastHelper.cast( getSite.get( "getSite" ) );

        assertNull( getForGetSite );

        assertTrue( data.containsKey( "getSiteByKey" ) );

        Map<String, Object> getSiteByKey = CastHelper.cast( data.get( "getSiteByKey" ) );
        Map<String, Object> getForGetSiteByKey = CastHelper.cast( getSiteByKey.get( "getSite" ) );

        assertNotNull( getForGetSiteByKey );
        assertEquals( "siteId", getForGetSiteByKey.get( "_id" ) );
        assertEquals( "Site", getForGetSiteByKey.get( "displayName" ) );
    }

    @Override
    protected List<ContentType> getCustomContentTypes()
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

        List<ContentType> result = new ArrayList<>();

        result.add( myContentType );

        return result;
    }

    @Test
    public void testExecuteQueryInLocalContext()
    {
        Content contentInMasterBranch =
            Content.create().id( ContentId.from( "contentId" ) ).path( ContentPath.from( "/contentPath" ) ).name( "name" ).displayName(
                "Name" ).parentPath( ContentPath.ROOT ).type( ContentTypeName.unstructured() ).data( new PropertyTree() ).build();

        Content contentInDraftBranch =
            Content.create().id( ContentId.from( "contentId" ) ).path( ContentPath.from( "/contentPath" ) ).displayName(
                "New Name" ).parentPath( ContentPath.ROOT ).type( ContentTypeName.unstructured() ).data( new PropertyTree() ).build();

        Site site = Site.create().name( "site" ).type( ContentTypeName.site() ).path( ContentPath.from( "/sitePath" ) ).parentPath(
            ContentPath.ROOT ).data( new PropertyTree() ).displayName( "Site" ).id( ContentId.from( "siteId" ) ).build();

        when( contentService.findNearestSiteByPath( ContentPath.from( "/sitePath" ) ) ).thenReturn( site );
        when( contentService.getNearestSite( ContentId.from( "siteId" ) ) ).thenReturn( site );

        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( contentInMasterBranch );
        when( contentService.getByPath( ContentPath.from( "/sitePath/contentPath" ) ) ).thenReturn( contentInDraftBranch );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result =
            executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/executeQueryInLocalContext.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> data = CastHelper.cast( result.get( "data" ) );

        assertTrue( data.containsKey( "g1" ) );
        Map<String, Object> g1 = CastHelper.cast( data.get( "g1" ) );
        Map<String, Object> getFoG1 = CastHelper.cast( g1.get( "get" ) );
        assertEquals( "Name", getFoG1.get( "displayName" ) );
        assertEquals( "contentId", getFoG1.get( "_id" ) );

        assertTrue( data.containsKey( "g2" ) );
        Map<String, Object> g2 = CastHelper.cast( data.get( "g2" ) );
        Map<String, Object> getFoG2 = CastHelper.cast( g2.get( "get" ) );
        assertEquals( "New Name", getFoG2.get( "displayName" ) );
        assertEquals( "contentId", getFoG2.get( "_id" ) );
    }

    @Test
    public void testGetContentPath()
    {
        when( contentService.getByPath( ContentPath.from( "/a/b" ) ) ).thenReturn( ContentFixtures.createContent( "id_1", "b", "/a" ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getContentPath.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( result, "get" ) );
        assertEquals( "/a/b", getField.get( "_path" ) );
    }
}
