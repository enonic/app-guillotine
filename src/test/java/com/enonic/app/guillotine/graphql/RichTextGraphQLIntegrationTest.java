package com.enonic.app.guillotine.graphql;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.macro.MacroKey;
import com.enonic.xp.portal.url.AttachmentUrlParts;
import com.enonic.xp.portal.url.ImageUrlParts;
import com.enonic.xp.portal.url.PageUrlParts;
import com.enonic.xp.portal.url.PortalScope;
import com.enonic.xp.portal.url.PortalScopeParams;
import com.enonic.xp.portal.url.ProcessHtmlPartsParams;
import com.enonic.xp.portal.url.ProcessedHtml;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;
import com.enonic.xp.site.SiteConfigs;

import static com.enonic.app.guillotine.graphql.ResourceHelper.readGraphQLQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RichTextGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void testRichTextField()
    {
        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn(
            new ProcessedHtml( "processedHtml", null, List.of(), List.of(), List.of() ) );

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
    public void testContentLinksBelongToSiteKey()
    {
        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn(
            new ProcessedHtml( "processedHtml", null, List.of(), List.of(), List.of() ) );

        when( contentService.contentExists( ContentPath.from( "/mysite" ) ) ).thenReturn( true );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> response = executeQuery( graphQLSchema,
                                                     "query { guillotine(siteKey: \"/mysite\") { get(key: \"contentid\") { " +
                                                         "...on myapplication_News { data { text { processedHtml } } } } } }" );

        assertFalse( response.containsKey( "errors" ) );

        // the site key selects the scope: XP resolves it once, and makes the path of each link relative to it
        assertEquals( "/mysite", portalScopeKey() );
    }

    @Test
    public void testBaseIsResolvedOncePerQuery()
    {
        final PortalScope scope =
            new PortalScope( ProjectName.from( "myproject" ), Branch.from( "master" ), ContentPath.from( "/mysite" ), SiteConfigs.empty() );
        when( serviceFacade.getPortalUrlService().portalScope( any( PortalScopeParams.class ) ) ).thenReturn( scope );
        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn(
            new ProcessedHtml( "processedHtml", null, List.of(), List.of(), List.of() ) );

        when( contentService.contentExists( ContentPath.from( "/mysite" ) ) ).thenReturn( true );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> response = executeQuery( graphQLSchema,
                                                     "query { guillotine(siteKey: \"/mysite\") { get(key: \"contentid\") { " +
                                                         "...on myapplication_News { data { first: text { processedHtml } " +
                                                         "second: text { processedHtml } } } } } }" );

        assertFalse( response.containsKey( "errors" ) );

        verify( serviceFacade.getPortalUrlService(), times( 1 ) ).portalScope( any( PortalScopeParams.class ) );

        ArgumentCaptor<ProcessHtmlPartsParams> captor = ArgumentCaptor.forClass( ProcessHtmlPartsParams.class );
        verify( serviceFacade.getPortalUrlService(), times( 2 ) ).processHtmlParts( captor.capture() );
        captor.getAllValues().forEach( params -> assertSame( scope, params.getScope() ) );
    }

    @Test
    public void testLinksBelongToProjectWithoutSiteKey()
    {
        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn(
            new ProcessedHtml( "processedHtml", null, List.of(), List.of(), List.of() ) );

        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> response = executeQuery( graphQLSchema, "query { guillotine { get(key: \"contentid\") { " +
            "...on myapplication_News { data { text { processedHtml } } } } } }" );

        assertFalse( response.containsKey( "errors" ) );

        assertNull( portalScopeKey() );

        ArgumentCaptor<ProcessHtmlPartsParams> captor = ArgumentCaptor.forClass( ProcessHtmlPartsParams.class );
        verify( serviceFacade.getPortalUrlService() ).processHtmlParts( captor.capture() );
        assertNull( captor.getValue().getCustomStyleDescriptorsCallback() );

        assertNull( captor.getValue().getCustomHtmlProcessor() );
        assertTrue( captor.getValue().isProcessMacros() );
    }

    @Test
    public void testMacrosComeFromProcessedHtml()
    {
        final String html = "<editor-macro data-macro-name=\"embed\" data-macro-ref=\"macro-1\">&lt;iframe&gt;&lt;/iframe&gt;</editor-macro>";
        final ProcessedHtml processed = new ProcessedHtml( html, null, List.of(), List.of(), List.of(
            new ProcessedHtml.Macro( "macro-1", MacroKey.from( "system:embed" ), Map.of(), "&lt;iframe&gt;&lt;/iframe&gt;" ),
            new ProcessedHtml.Macro( "macro-2", MacroKey.from( "myapp:removed" ), Map.of(), "" ) ) );

        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn( processed );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> response = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/richText.graphql" ) );

        assertFalse( response.containsKey( "errors" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( response, "get" ) );
        Map<String, Object> textField = CastHelper.cast( CastHelper.<Map<String, Object>>cast( getField.get( "data" ) ).get( "text" ) );

        assertEquals( html, textField.get( "processedHtml" ) );

        // a macro whose descriptor is gone by the time it is serialized has no entry
        List<Map<String, Object>> macros = CastHelper.cast( textField.get( "macros" ) );
        assertEquals( 1, macros.size() );
        assertEquals( "macro-1", macros.get( 0 ).get( "ref" ) );
        assertEquals( "embed", macros.get( 0 ).get( "name" ) );
        assertEquals( "system:embed", macros.get( 0 ).get( "descriptor" ) );
        Map<String, Object> config = CastHelper.cast( macros.get( 0 ).get( "config" ) );
        assertEquals( Map.of( "body", "&lt;iframe&gt;&lt;/iframe&gt;" ), config.get( "embed" ) );
    }

    @Test
    public void testMacroConfigOfAnotherApplicationsMacroOfTheSameName()
    {
        // the site's embed macro comes from myapp, while the schema's embed field is built for the built-in one
        final MacroKey key = MacroKey.from( "myapp:embed" );
        when( serviceFacade.getMacroDescriptorService().getByKey( key ) ).thenReturn( MacroDescriptor.create().key( key ).build() );

        final ProcessedHtml processed = new ProcessedHtml(
            "<editor-macro data-macro-name=\"embed\" data-macro-ref=\"macro-1\">body</editor-macro>", null, List.of(), List.of(),
            List.of( new ProcessedHtml.Macro( "macro-1", key, Map.of(), "body" ) ) );

        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn( processed );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> response = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/richText.graphql" ) );

        assertFalse( response.containsKey( "errors" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( response, "get" ) );
        Map<String, Object> textField = CastHelper.cast( CastHelper.<Map<String, Object>>cast( getField.get( "data" ) ).get( "text" ) );

        List<Map<String, Object>> macros = CastHelper.cast( textField.get( "macros" ) );
        assertEquals( "myapp:embed", macros.get( 0 ).get( "descriptor" ) );
        Map<String, Object> config = CastHelper.cast( macros.get( 0 ).get( "config" ) );
        assertNull( config.get( "embed" ) );

        // its parameters stay available as JSON
        List<Map<String, Object>> macrosAsJson = CastHelper.cast( textField.get( "macrosAsJson" ) );
        assertEquals( Map.of( "embed", Map.of( "body", "body" ) ), macrosAsJson.get( 0 ).get( "config" ) );
    }

    @Test
    public void testLinksAndImagesComeFromProcessedHtml()
    {
        final ProcessedHtml processed = new ProcessedHtml(
            "<a href=\"/posts/first?a=1#top\" data-link-ref=\"link-1\">Post</a><a href=\"/media:attachment/p:b/f:h/doc.pdf?download\" " +
                "data-link-ref=\"link-2\">Doc</a><img src=\"/media:image/p:b/i:h/width-768/a.jpg\" data-image-ref=\"image-1\">",
            "https://site.example.com", List.of(
            new ProcessedHtml.ContentLink( "link-1", "content://first", "first", new PageUrlParts( "https://site.example.com", "/posts/first", "?a=1" ),
                                           "#top" ),
            new ProcessedHtml.AttachmentLink( "link-2", "media://download/doc", "doc",
                                              new AttachmentUrlParts( "/media:attachment/p:b/f:h/doc.pdf", "?download", "p:b", "f", "h",
                                                                      "doc.pdf" ), true ) ), List.of(
            new ProcessedHtml.Image( "image-1", "image", new ProcessedHtml.Style( ApplicationKey.from( "myapp" ), "wide", "16:9", "grayscale()" ),
                                     new ImageUrlParts( "/media:image/p:b/i:h/width-768/a.jpg", "", "p:b", "i", "h", "width-768", "a.jpg" ),
                                     List.of( new ProcessedHtml.Source( 400, new ImageUrlParts( "/media:image/p:b/i:h/width-400/a.jpg", "",
                                                                                               "p:b", "i", "h", "width-400",
                                                                                               "a.jpg" ) ) ) ) ), List.of() );

        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn( processed );

        when( contentService.contentExists( ContentPath.from( "/mysite" ) ) ).thenReturn( true );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> response = executeQuery( graphQLSchema,
                                                     "query { guillotine(siteKey: \"/mysite\") { get(key: \"contentid\") { " +
                                                         "...on myapplication_News { data { text { processedHtml " +
                                                         "links { ref uri pageUrl { baseUrl path queryString } fragment media { intent mediaUrl { path queryString } } } " +
                                                         "images { ref style { application name aspectRatio filter } src { path queryString } srcset { width imageUrl { path } } } } } } } } }" );

        assertFalse( response.containsKey( "errors" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( response, "get" ) );
        Map<String, Object> textField = CastHelper.cast( CastHelper.<Map<String, Object>>cast( getField.get( "data" ) ).get( "text" ) );

        List<Map<String, Object>> links = CastHelper.cast( textField.get( "links" ) );
        assertEquals( "link-1", links.get( 0 ).get( "ref" ) );
        assertEquals( "content://first", links.get( 0 ).get( "uri" ) );
        assertEquals( Map.of( "baseUrl", "https://site.example.com", "path", "/posts/first", "queryString", "?a=1" ),
                      links.get( 0 ).get( "pageUrl" ) );
        assertEquals( "#top", links.get( 0 ).get( "fragment" ) );
        assertNull( links.get( 0 ).get( "media" ) );

        assertEquals( "link-2", links.get( 1 ).get( "ref" ) );
        assertNull( links.get( 1 ).get( "pageUrl" ) );
        assertEquals( "", links.get( 1 ).get( "fragment" ) );
        Map<String, Object> media = CastHelper.cast( links.get( 1 ).get( "media" ) );
        assertEquals( "download", media.get( "intent" ) );
        assertEquals( Map.of( "path", "/media:attachment/p:b/f:h/doc.pdf", "queryString", "?download" ), media.get( "mediaUrl" ) );

        List<Map<String, Object>> images = CastHelper.cast( textField.get( "images" ) );
        assertEquals( "image-1", images.get( 0 ).get( "ref" ) );
        assertEquals( Map.of( "application", "myapp", "name", "wide", "aspectRatio", "16:9", "filter", "grayscale()" ),
                      images.get( 0 ).get( "style" ) );
        assertEquals( Map.of( "path", "/media:image/p:b/i:h/width-768/a.jpg", "queryString", "" ), images.get( 0 ).get( "src" ) );
        assertEquals( List.of( Map.of( "width", 400, "imageUrl", Map.of( "path", "/media:image/p:b/i:h/width-400/a.jpg" ) ) ),
                      images.get( 0 ).get( "srcset" ) );

        ArgumentCaptor<ProcessHtmlPartsParams> captor = ArgumentCaptor.forClass( ProcessHtmlPartsParams.class );
        verify( serviceFacade.getPortalUrlService() ).processHtmlParts( captor.capture() );
        assertNull( captor.getValue().getCustomStyleDescriptorsCallback() );
    }

    @Test
    public void testLinksAndImagesThatDoNotResolve()
    {
        final ProcessedHtml processed = new ProcessedHtml( "<a href=\"content://gone\" data-link-ref=\"link-1\">Gone</a>", null, List.of(
            new ProcessedHtml.ContentLink( "link-1", "content://gone?fragment=top", "gone", null, "#top" ),
            new ProcessedHtml.AttachmentLink( "link-2", "media://download/gone", "gone", null, true ) ),
                                                           List.of( new ProcessedHtml.Image( "image-1", "gone", null, null, List.of() ) ),
                                                           List.of() );

        when( serviceFacade.getPortalUrlService().processHtmlParts( any( ProcessHtmlPartsParams.class ) ) ).thenReturn( processed );
        when( contentService.contentExists( ContentPath.from( "/mysite" ) ) ).thenReturn( true );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( createContent( true ) );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> response = executeQuery( graphQLSchema,
                                                     "query { guillotine(siteKey: \"/mysite\") { get(key: \"contentid\") { " +
                                                         "...on myapplication_News { data { text { " +
                                                         "links { ref pageUrl { path } fragment media { intent mediaUrl { path } } } " +
                                                         "images { ref src { path } srcset { width } } } } } } } }" );

        assertFalse( response.containsKey( "errors" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( response, "get" ) );
        Map<String, Object> textField = CastHelper.cast( CastHelper.<Map<String, Object>>cast( getField.get( "data" ) ).get( "text" ) );

        List<Map<String, Object>> links = CastHelper.cast( textField.get( "links" ) );
        assertEquals( "link-1", links.get( 0 ).get( "ref" ) );
        assertNull( links.get( 0 ).get( "pageUrl" ) );
        assertEquals( "#top", links.get( 0 ).get( "fragment" ) );
        Map<String, Object> media = CastHelper.cast( links.get( 1 ).get( "media" ) );
        assertEquals( "download", media.get( "intent" ) );
        assertNull( media.get( "mediaUrl" ) );

        List<Map<String, Object>> images = CastHelper.cast( textField.get( "images" ) );
        assertEquals( "image-1", images.get( 0 ).get( "ref" ) );
        assertNull( images.get( 0 ).get( "src" ) );
        assertEquals( List.of(), images.get( 0 ).get( "srcset" ) );
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

    private String portalScopeKey()
    {
        final ArgumentCaptor<PortalScopeParams> captor = ArgumentCaptor.forClass( PortalScopeParams.class );
        verify( serviceFacade.getPortalUrlService() ).portalScope( captor.capture() );
        final PortalScopeParams params = captor.getValue();
        return params.getContentPath() != null ? params.getContentPath().toString() : Objects.toString( params.getContentId(), null );
    }
}
