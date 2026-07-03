package com.enonic.app.guillotine.graphql;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.portal.html.HtmlDocument;
import com.enonic.xp.portal.html.HtmlElement;
import com.enonic.xp.portal.url.HtmlProcessorParams;
import com.enonic.xp.portal.url.ProcessHtmlParams;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;

import static com.enonic.app.guillotine.graphql.ResourceHelper.readGraphQLQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RichTextGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void testRichTextField()
    {
        when( serviceFacade.getPortalUrlService().processHtml( any( ProcessHtmlParams.class ) ) ).thenReturn( "processedHtml" );

        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

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
    public void testRichTextFieldWithMediaBaseUrl()
    {
        // allowlist entry without trailing slash must match the argument with one
        setAllowedBaseUrls( "https://media.example.com" );

        when( serviceFacade.getPortalUrlService().processHtml( any( ProcessHtmlParams.class ) ) ).thenReturn( "processedHtml" );

        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        String query = "query { guillotine(mediaBaseUrl: \"https://media.example.com/\") { get(key: \"contentid\") { _id " +
            "...on myapplication_News { data { text { processedHtml } } } } } }";

        Map<String, Object> response = executeQuery( graphQLSchema, query );

        assertFalse( response.containsKey( "errors" ) );

        // with mediaBaseUrl set, XP is asked for root-relative "/_/..." URLs ("/" baseUrl);
        // mediaBaseUrl itself is prepended by the custom HTML processor afterwards
        ArgumentCaptor<ProcessHtmlParams> captor = ArgumentCaptor.forClass( ProcessHtmlParams.class );
        verify( serviceFacade.getPortalUrlService() ).processHtml( captor.capture() );
        assertEquals( "/", captor.getValue().getBaseUrl() );
    }

    @Test
    public void testMediaBaseUrlArgumentRejectedByDefault()
    {
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        String query = "query { guillotine(mediaBaseUrl: \"https://media.example.com/\") { get(key: \"contentid\") { _id " +
            "...on myapplication_News { data { text { processedHtml } } } } } }";

        Map<String, Object> response = executeQuery( graphQLSchema, query );

        assertTrue( response.containsKey( "errors" ) );
    }

    @Test
    public void testPageBaseUrlArgumentRejectedWhenNotInAllowlist()
    {
        setAllowedBaseUrls( "https://www.example.com" );

        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        String query = "query { guillotine(pageBaseUrl: \"https://evil.example.com/\") { get(key: \"contentid\") { _id " +
            "...on myapplication_News { data { text { processedHtml } } } } } }";

        Map<String, Object> response = executeQuery( graphQLSchema, query );

        assertTrue( response.containsKey( "errors" ) );
    }

    @Test
    public void testRichTextFieldWithPageBaseUrl()
    {
        setAllowedBaseUrls( "*" );

        when( serviceFacade.getPortalUrlService().processHtml( any( ProcessHtmlParams.class ) ) ).thenReturn( "processedHtml" );

        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        String query = "query { guillotine(pageBaseUrl: \"https://pages.example.com/\") { get(key: \"contentid\") { _id " +
            "...on myapplication_News { data { text { processedHtml } } } } } }";

        Map<String, Object> response = executeQuery( graphQLSchema, query );

        assertFalse( response.containsKey( "errors" ) );

        // pageBaseUrl is delegated to XP: content:// links are generated against it by processHtml itself
        ArgumentCaptor<ProcessHtmlParams> captor = ArgumentCaptor.forClass( ProcessHtmlParams.class );
        verify( serviceFacade.getPortalUrlService() ).processHtml( captor.capture() );
        assertEquals( "https://pages.example.com/", captor.getValue().getPageBaseUrl() );
    }

    @Test
    public void testRichTextFieldPrependsMediaBaseUrlWhenSet()
    {
        setAllowedBaseUrls( "*" );

        when( serviceFacade.getPortalUrlService().processHtml( any( ProcessHtmlParams.class ) ) ).thenReturn( "processedHtml" );

        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        String query = "query { guillotine(mediaBaseUrl: \"https://media.example.com/whatever\") { get(key: \"contentid\") { _id " +
            "...on myapplication_News { data { text { processedHtml } } } } } }";

        Map<String, Object> response = executeQuery( graphQLSchema, query );

        assertFalse( response.containsKey( "errors" ) );

        ArgumentCaptor<ProcessHtmlParams> captor = ArgumentCaptor.forClass( ProcessHtmlParams.class );
        verify( serviceFacade.getPortalUrlService() ).processHtml( captor.capture() );

        // mediaBaseUrl provided -> XP generates root-relative "/_/..." media URLs and a customHtmlProcessor
        // must be set to prepend mediaBaseUrl to them (dropping the "_" endpoint segment)
        assertNotNull( captor.getValue().getCustomHtmlProcessor() );

        HtmlElement image = mock( HtmlElement.class );
        when( image.getAttribute( "src" ) ).thenReturn( "/_/media:image/myproject:draft/contentid:hash/scale/name.jpg" );

        HtmlElement responsiveImage = mock( HtmlElement.class );
        when( responsiveImage.getAttribute( "srcset" ) ).thenReturn(
            "/_/media:image/myproject:draft/contentid:hash/600/name.jpg 600w, " +
                "/_/media:image/myproject:draft/contentid:hash/900/name.jpg 900w" );

        HtmlElement mediaLink = mock( HtmlElement.class );
        when( mediaLink.getAttribute( "href" ) ).thenReturn( "/_/media:attachment/myproject:draft/contentid:hash/doc.pdf" );

        // hand-written external link containing "/_/" must not be touched
        HtmlElement externalLink = mock( HtmlElement.class );
        when( externalLink.getAttribute( "href" ) ).thenReturn( "https://othersite.com/_/service/whatever" );

        // non-media XP endpoint URL under /_/ must not be moved to the media host
        HtmlElement serviceLink = mock( HtmlElement.class );
        when( serviceLink.getAttribute( "href" ) ).thenReturn( "/_/service/myapp/myservice" );

        HtmlDocument document = mock( HtmlDocument.class );
        when( document.select( "[src]" ) ).thenReturn( List.of( image ) );
        when( document.select( "[srcset]" ) ).thenReturn( List.of( responsiveImage ) );
        when( document.select( "[href]" ) ).thenReturn( List.of( mediaLink, externalLink, serviceLink ) );
        when( document.select( "figcaption:empty" ) ).thenReturn( List.of() );
        when( document.getInnerHtml() ).thenReturn( "result" );

        HtmlProcessorParams processorParams = HtmlProcessorParams.create()
            .htmlDocument( document )
            .defaultProcessor( postProcessor -> {
            } )
            .defaultElementProcessor( ( element, postProcessor ) -> {
            } )
            .build();

        captor.getValue().getCustomHtmlProcessor().apply( processorParams );

        verify( image ).setAttribute( "src",
                                      "https://media.example.com/whatever/media:image/myproject:draft/contentid:hash/scale/name.jpg" );
        verify( responsiveImage ).setAttribute( "srcset",
                                                "https://media.example.com/whatever/media:image/myproject:draft/contentid:hash/600/name.jpg 600w, " +
                                                    "https://media.example.com/whatever/media:image/myproject:draft/contentid:hash/900/name.jpg 900w" );
        verify( mediaLink ).setAttribute( "href",
                                          "https://media.example.com/whatever/media:attachment/myproject:draft/contentid:hash/doc.pdf" );
        verify( externalLink, org.mockito.Mockito.never() ).setAttribute( org.mockito.ArgumentMatchers.eq( "href" ),
                                                                          org.mockito.ArgumentMatchers.anyString() );
        verify( serviceLink, org.mockito.Mockito.never() ).setAttribute( org.mockito.ArgumentMatchers.eq( "href" ),
                                                                         org.mockito.ArgumentMatchers.anyString() );
    }

    @Test
    public void testEmptyRichTextField()
    {
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( false ) );

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

        builder.id( ContentId.from( "contentid" ) );
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
