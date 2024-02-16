package com.enonic.app.guillotine.graphql;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.portal.url.ProcessHtmlParams;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;

import static com.enonic.app.guillotine.graphql.ResourceHelper.readGraphQLQuery;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class RichTextGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void testRichTextField()
    {
        when( serviceFacade.getPortalUrlService().processHtml( any( ProcessHtmlParams.class ) ) ).thenReturn( "processedHtml" );

        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        assertNotNull( graphQLSchema.getObjectType( "myapplication_News" ) );

        Map<String, Object> response = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/richText.graphql" ) );

        assertFalse( response.containsKey( "errors" ) );
        assertTrue( response.containsKey( "data" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( response, "get" ) );

        Map<String, Object> dataField = CastHelper.cast( getField.get( "data" ) );

        Map<String, Object> textField = CastHelper.cast( dataField.get( "text" ) );

        assertNotNull( textField );
    }

    @Test
    public void testEmptyRichTextField()
    {
        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( createContent( false ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        assertNotNull( graphQLSchema.getObjectType( "myapplication_News" ) );

        Map<String, Object> response = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/richText.graphql" ) );

        assertFalse( response.containsKey( "errors" ) );
        assertTrue( response.containsKey( "data" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( response, "get" ) );

        Map<String, Object> dataField = CastHelper.cast( getField.get( "data" ) );

        assertNull( CastHelper.cast( dataField.get( "text" ) ) );
    }

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        ContentType newsContentType =
            ContentType.create().superType( ContentTypeName.structured() ).name( "myapplication:news" ).addFormItem(
                Input.create().name( "text" ).label( "Text" ).occurrences( 1, 1 ).inputType( InputTypeName.HTML_AREA ).build() ).build();

        return List.of( newsContentType );
    }

    private Content createContent( boolean includeHtml )
    {
        final Content.Builder<?> builder = Content.create();

        builder.id( ContentId.from( "contentId" ) );
        builder.name( "news" );
        builder.displayName( "Hot News" );
        builder.valid( true );
        builder.type( ContentTypeName.from( "myapplication:news" ) );
        builder.parentPath( ContentPath.ROOT );
        builder.modifier( PrincipalKey.from( "user:system:admin" ) );
        builder.modifiedTime( Instant.ofEpochSecond( 0 ) );
        builder.creator( PrincipalKey.from( "user:system:admin" ) );
        builder.owner( PrincipalKey.from( "user:system:admin" ) );
        builder.createdTime( Instant.ofEpochSecond( 0 ) );
        builder.language( Locale.ENGLISH );
        builder.permissions( AccessControlList.create().add(
            AccessControlEntry.create().allowAll().principal( RoleKeys.CONTENT_MANAGER_ADMIN ).build() ).build() );

        PropertyTree data = new PropertyTree();

        if ( includeHtml )
        {
            data.setString( "text", "<p><a href=\"content://a8b374a2-c532-45eb-9aa1-73d1c37cd681\">Link to Content</a></p>\n" +
                "<p><a href=\"media://inline/289e6ba0-e5f7-4667-a2a0-fe6afa4a6267\" target=\"_blank\">Link to Media</a></p>\n" +
                "<p>[embed]&lt;iframe title=\"YouTube video player\" src=\"https://www.youtube.com/embed/6FTpJtS8NVE\" height=\"315\" width=\"560\"&gt;&lt;/iframe&gt;[/embed]</p>\n" +
                "<figure class=\"captioned conteditor-style-grayscale editor-align-justify\">\n" +
                "  <img alt=\"bruce-willis.jpg\" src=\"image://cbad75b1-7048-46a4-85b1-99b923da139c?style=conteditor-style-grayscale\" style=\"width:100%\" />\n" +
                "  <figcaption>Bruce Willis</figcaption>\n" + "</figure>\n" + "<p>Text</p>\n" );
        }

        builder.data( data );

        return builder.build();
    }
}
