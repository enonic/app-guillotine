package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.ImageUrlGeneratorParams;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.AttachmentUrlParts;
import com.enonic.xp.portal.url.ImageUrlParts;
import com.enonic.xp.portal.url.PageUrlParts;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;
import com.enonic.xp.portal.url.PortalUrlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class UrlFieldDataFetcherTest
{

    private DataFetchingEnvironment environment;

    private DataFetchingFieldSelectionSet selectionSet;

    private Map<String, Object> localContext;

    @BeforeEach
    public void setUp()
    {
        localContext = new HashMap<>();

        localContext.put( Constants.PROJECT_ARG, "myproject" );
        localContext.put( Constants.BRANCH_ARG, "draft" );
        localContext.put( Constants.CURRENT_CONTENT_FIELD, GuillotineLocalContextHelper.mapToJson( ContentFixtures.createContentAsMap() ) );

        environment = Mockito.mock( DataFetchingEnvironment.class );
        when( environment.getLocalContext() ).thenReturn( localContext );

        selectionSet = Mockito.mock( DataFetchingFieldSelectionSet.class );
        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( true );
        when( environment.getSelectionSet() ).thenReturn( selectionSet );
    }

    @Test
    public void testAttachmentUrlByName()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "https://cdn.example.com/api", "/media:attachment/myproject/contentid:hash/name", "", "myproject", "contentid", "hash", "name" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetAttachmentUrlByNameDataFetcher( portalUrlService ).get( environment );

        assertEquals( "https://cdn.example.com/api", result.get( "apiUrl" ) );
        assertEquals( "/media:attachment/myproject/contentid:hash/name", result.get( "path" ) );
        assertFalse( result.containsKey( "url" ) );
        verify( portalUrlService, never() ).attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) );
    }

    @Test
    public void testImageUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( null, "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );
        when( environment.getArgument( "quality" ) ).thenReturn( 1 );
        when( environment.getArgument( "background" ) ).thenReturn( "background" );
        when( environment.getArgument( "format" ) ).thenReturn( "format" );
        when( environment.getArgument( "filter" ) ).thenReturn( "filter" );

        final Map<String, Object> result = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", result.get( "path" ) );
        assertFalse( result.containsKey( "url" ) );
        verify( portalUrlService, never() ).imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) );
    }

    @Test
    public void testAttachmentUrlById()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "https://cdn.example.com/api", "/media:attachment/myproject/contentid:hash/name", "", "myproject", "contentid", "hash", "name" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "download" ) ).thenReturn( false );

        final Map<String, Object> result = new GetAttachmentUrlByIdDataFetcher( portalUrlService ).get( environment );

        assertEquals( "https://cdn.example.com/api", result.get( "apiUrl" ) );
        assertEquals( "/media:attachment/myproject/contentid:hash/name", result.get( "path" ) );
        assertFalse( result.containsKey( "url" ) );
        verify( portalUrlService, never() ).attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) );
    }

    @Test
    public void testImageApiUrlComesFromTheParts()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( "https://cdn.example.com/api", "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        final Map<String, Object> result = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "https://cdn.example.com/api", result.get( "apiUrl" ) );

        // configured in XP and carried by the parts: guillotine passes no base of its own
        ArgumentCaptor<ImageUrlGeneratorParams> partsCaptor = ArgumentCaptor.forClass( ImageUrlGeneratorParams.class );
        verify( portalUrlService ).imageUrlParts( partsCaptor.capture() );
        assertNull( partsCaptor.getValue().getMediaBaseUrl() );
        assertNull( partsCaptor.getValue().getBaseUrl() );
    }

    @Test
    public void testImageUrlWithoutConfiguredApiUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( null, "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        final Map<String, Object> result = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        // nothing configured: the client supplies the media base
        assertTrue( result.containsKey( "apiUrl" ) );
        assertNull( result.get( "apiUrl" ) );
        assertEquals( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", result.get( "path" ) );
    }

    @Test
    public void testImageUrlParts()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( null, "/media:image/myproject:draft/contentid:hash/max-300/name.jpg", "?quality=85", "myproject:draft",
                               "contentid", "hash", "max-300", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "max(300)" );

        final Map<String, Object> parts = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/media:image/myproject:draft/contentid:hash/max-300/name.jpg", parts.get( "path" ) );
        assertEquals( "?quality=85", parts.get( "queryString" ) );
        assertEquals( "myproject:draft", parts.get( "context" ) );
        assertEquals( "contentid", parts.get( "id" ) );
        assertEquals( "hash", parts.get( "fingerprint" ) );
        assertEquals( "max-300", parts.get( "scale" ) );
        assertEquals( "name.jpg", parts.get( "name" ) );

        // the url generator is never called and there is no assembled url
        assertFalse( parts.containsKey( "url" ) );
        verify( portalUrlService, never() ).imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) );
    }

    @Test
    public void testAttachmentUrlPartsById()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( null, "/media:attachment/myproject/contentid:hash/name.jpg", "", "myproject", "contentid", "hash",
                                    "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts = new GetAttachmentUrlByIdDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/media:attachment/myproject/contentid:hash/name.jpg", parts.get( "path" ) );
        assertEquals( "", parts.get( "queryString" ) );
        assertNull( parts.get( "scale" ) );
        assertEquals( "inline", parts.get( "intent" ) );

        verify( portalUrlService, never() ).attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) );
    }

    @Test
    public void testPageUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn(
            new PageUrlParts( "https://site.example.com", "/b/mycontent", "?a=1" ) );

        localContext.put( Constants.SITE_ARG, "/mysite" );

        final Map<String, Object> parts = new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "https://site.example.com", parts.get( "baseUrl" ) );
        assertEquals( "/b/mycontent", parts.get( "path" ) );
        assertEquals( "?a=1", parts.get( "queryString" ) );
        assertFalse( parts.containsKey( "url" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        assertEquals( "/mysite", captor.getValue().getBase().getPath() );

        // the assembled URL is left to the client
        verify( portalUrlService, never() ).pageUrl( Mockito.any( PageUrlParams.class ) );
    }

    @Test
    public void testPageUrlWithoutConfiguredBaseUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn(
            new PageUrlParts( null, "/b/mycontent", "" ) );

        localContext.put( Constants.SITE_ARG, "/mysite" );

        final Map<String, Object> parts = new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        assertTrue( parts.containsKey( "baseUrl" ) );
        assertNull( parts.get( "baseUrl" ) );
        assertEquals( "/b/mycontent", parts.get( "path" ) );
    }

    @Test
    public void testPageUrlRequiresSiteKey()
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );

        final IllegalArgumentException e =
            assertThrows( IllegalArgumentException.class, () -> new GetPageUrlDataFetcher( portalUrlService ).get( environment ) );
        assertTrue( e.getMessage().contains( Constants.SITE_ARG ) );

        verifyNoInteractions( portalUrlService );
    }

    @Test
    public void testLinkPageUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn(
            new PageUrlParts( "https://site.example.com", "/b/mycontent", "" ) );

        localContext.put( Constants.SITE_ARG, "/mysite" );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "linkedcontent" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts = new GetLinkPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "https://site.example.com", parts.get( "baseUrl" ) );
        assertEquals( "/b/mycontent", parts.get( "path" ) );
        assertFalse( parts.containsKey( "url" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        assertEquals( "linkedcontent", captor.getValue().getId() );
        assertEquals( "/mysite", captor.getValue().getBase().getPath() );

        verify( portalUrlService, never() ).pageUrl( Mockito.any( PageUrlParams.class ) );
    }

    @Test
    public void testLinkPageUrlRequiresSiteKey()
    {
        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "linkedcontent" );

        when( environment.getSource() ).thenReturn( source );

        assertThrows( IllegalArgumentException.class,
                      () -> new GetLinkPageUrlDataFetcher( Mockito.mock( PortalUrlService.class ) ).get( environment ) );
    }

    @Test
    public void testLinkPageUrlIsNullForMediaLinks()
        throws Exception
    {
        // media link projections carry the contentId inside the media object, not on the link
        Map<String, Object> source = new HashMap<>();
        when( environment.getSource() ).thenReturn( source );

        assertNull( new GetLinkPageUrlDataFetcher( Mockito.mock( PortalUrlService.class ) ).get( environment ) );
    }

    @Test
    public void testLinkMediaUrlHonorsDownloadIntent()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlGeneratorService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlGeneratorService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( null, "/media:attachment/myproject/contentid:hash/name.jpg", "?download", "myproject", "contentid",
                                    "hash", "name.jpg" ) );

        ContentService contentService = Mockito.mock( ContentService.class );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( Mockito.mock( Content.class ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "contentid" );
        source.put( "intent", "download" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts =
            new GetLinkMediaUrlDataFetcher( portalUrlGeneratorService, contentService ).get( environment );

        assertEquals( "?download", parts.get( "queryString" ) );
        assertEquals( "download", parts.get( "intent" ) );

        ArgumentCaptor<AttachmentUrlGeneratorParams> captor = ArgumentCaptor.forClass( AttachmentUrlGeneratorParams.class );
        verify( portalUrlGeneratorService ).attachmentUrlParts( captor.capture() );
        assertTrue( captor.getValue().isDownload() );
    }

    @Test
    public void testLinkMediaApiUrlComesFromTheParts()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlGeneratorService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlGeneratorService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "https://cdn.example.com/api", "/media:attachment/myproject/contentid:hash/name.jpg", "", "myproject", "contentid", "hash",
                                    "name.jpg" ) );

        ContentService contentService = Mockito.mock( ContentService.class );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( Mockito.mock( Content.class ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result =
            new GetLinkMediaUrlDataFetcher( portalUrlGeneratorService, contentService ).get( environment );

        assertEquals( "https://cdn.example.com/api", result.get( "apiUrl" ) );
        assertEquals( "/media:attachment/myproject/contentid:hash/name.jpg", result.get( "path" ) );
        assertFalse( result.containsKey( "url" ) );

        ArgumentCaptor<AttachmentUrlGeneratorParams> partsCaptor = ArgumentCaptor.forClass( AttachmentUrlGeneratorParams.class );
        verify( portalUrlGeneratorService ).attachmentUrlParts( partsCaptor.capture() );
        assertNull( partsCaptor.getValue().getMediaBaseUrl() );
        verify( portalUrlGeneratorService, never() ).attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) );
    }

    @Test
    public void testImageUrlSkipsPartsWhenNoneSelected()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );

        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        final Map<String, Object> result = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        assertTrue( result.isEmpty() );
        verifyNoInteractions( portalUrlService );
    }

    @Test
    public void testAttachmentUrlSkipsPartsWhenOnlyIntentSelected()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );

        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetAttachmentUrlByIdDataFetcher( portalUrlService ).get( environment );

        assertFalse( result.containsKey( "apiUrl" ) );
        // intent is computed locally and stays available without the parts call
        assertEquals( "inline", result.get( "intent" ) );
        assertFalse( result.containsKey( "path" ) );
        verifyNoInteractions( portalUrlService );
    }

    @Test
    public void testLinkMediaUrlSkipsContentLoadWhenOnlyIntentSelected()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlGeneratorService = Mockito.mock( PortalUrlGeneratorService.class );

        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        ContentService contentService = Mockito.mock( ContentService.class );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "contentid" );
        source.put( "intent", "download" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetLinkMediaUrlDataFetcher( portalUrlGeneratorService, contentService ).get( environment );

        assertEquals( "download", result.get( "intent" ) );
        assertFalse( result.containsKey( "path" ) );

        // no parts selected: the target content must not be fetched from storage
        verify( contentService, never() ).getById( Mockito.any( ContentId.class ) );
        verifyNoInteractions( portalUrlGeneratorService );
    }
}
