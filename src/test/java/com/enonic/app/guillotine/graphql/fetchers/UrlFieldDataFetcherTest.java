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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when( selectionSet.contains( "url" ) ).thenReturn( true );
        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( true );
        when( environment.getSelectionSet() ).thenReturn( selectionSet );
    }

    @Test
    public void testAttachmentUrlByName()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "attachmentUrl" );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name", "", "myproject", "contentid", "hash", "name" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );

        GetAttachmentUrlByNameDataFetcher instance = new GetAttachmentUrlByNameDataFetcher( portalUrlService );
        assertEquals( "attachmentUrl", instance.get( environment ).get( "url" ) );
    }

    @Test
    public void testImageUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );
        when( environment.getArgument( "quality" ) ).thenReturn( 1 );
        when( environment.getArgument( "background" ) ).thenReturn( "background" );
        when( environment.getArgument( "format" ) ).thenReturn( "format" );
        when( environment.getArgument( "filter" ) ).thenReturn( "filter" );

        GetImageUrlDataFetcher instance = new GetImageUrlDataFetcher( portalUrlService );
        assertEquals( "imageUrl", instance.get( environment ).get( "url" ) );
    }

    @Test
    public void testAttachmentUrlById()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "attachmentUrl" );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name", "", "myproject", "contentid", "hash", "name" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "download" ) ).thenReturn( false );

        GetAttachmentUrlByIdDataFetcher instance = new GetAttachmentUrlByIdDataFetcher( portalUrlService );
        assertEquals( "attachmentUrl", instance.get( environment ).get( "url" ) );
    }


    @Test
    public void testImageUrlUsesImageBaseUrlFromSiteKey()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        // resolved by XP at the guillotine field: guillotine passes it through unchanged
        localContext.put( Constants.IMAGE_BASE_URL, "https://site.example.com/_" );

        new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        ArgumentCaptor<ImageUrlGeneratorParams> captor = ArgumentCaptor.forClass( ImageUrlGeneratorParams.class );
        verify( portalUrlService ).imageUrl( captor.capture() );
        assertEquals( "https://site.example.com/_", captor.getValue().getMediaBaseUrl() );
        assertNull( captor.getValue().getBaseUrl() );

        // parts never carry a base URL: components are independent of siteKey and request
        ArgumentCaptor<ImageUrlGeneratorParams> partsCaptor = ArgumentCaptor.forClass( ImageUrlGeneratorParams.class );
        verify( portalUrlService ).imageUrlParts( partsCaptor.capture() );
        assertNull( partsCaptor.getValue().getMediaBaseUrl() );
    }

    @Test
    public void testImageUrlWithoutAnyBaseUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        ArgumentCaptor<ImageUrlGeneratorParams> captor = ArgumentCaptor.forClass( ImageUrlGeneratorParams.class );
        verify( portalUrlService ).imageUrl( captor.capture() );
        assertNull( captor.getValue().getBaseUrl() );
    }


    @Test
    public void testImageUrlKeepsEndpointSegmentFromGenerator()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            "/site/repo/draft/app/_/media:image/myproject:draft/contentid:hash/scale/name.jpg" );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        localContext.put( Constants.IMAGE_BASE_URL, "https://site.example.com/_" );

        assertEquals( "/site/repo/draft/app/_/media:image/myproject:draft/contentid:hash/scale/name.jpg",
                      new GetImageUrlDataFetcher( portalUrlService ).get( environment ).get( "url" ) );
    }


    @Test
    public void testImageUrlParts()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            new ImageUrlParts( "/media:image/myproject:draft/contentid:hash/max-300/name.jpg", "?quality=85", "myproject:draft",
                               "contentid", "hash", "max-300", "name.jpg" ) );

        when( selectionSet.contains( "url" ) ).thenReturn( false );

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

        // url not selected: the url generator must not be called and the key must be absent
        assertFalse( parts.containsKey( "url" ) );
        verify( portalUrlService, never() ).imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) );
    }

    @Test
    public void testAttachmentUrlPartsById()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name.jpg", "", "myproject", "contentid", "hash",
                                    "name.jpg" ) );

        when( selectionSet.contains( "url" ) ).thenReturn( false );

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
    public void testPageUrlParts()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn(
            new PageUrlParts( "/b/mycontent", "?a=1" ) );

        when( selectionSet.contains( "url" ) ).thenReturn( false );

        final Map<String, Object> parts = new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/b/mycontent", parts.get( "path" ) );
        assertEquals( "?a=1", parts.get( "queryString" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        // parts never carry a base URL: components are independent of siteKey and request
        assertNull( captor.getValue().getBaseUrl() );

        verify( portalUrlService, never() ).pageUrl( Mockito.any( PageUrlParams.class ) );
    }

    @Test
    public void testLinkPageUrlParts()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn( new PageUrlParts( "/b/mycontent", "" ) );

        when( selectionSet.contains( "url" ) ).thenReturn( false );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "linkedcontent" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts = new GetLinkPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/b/mycontent", parts.get( "path" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        assertEquals( "linkedcontent", captor.getValue().getId() );
    }

    @Test
    public void testLinkPageUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn( new PageUrlParts( "/b/mycontent", "" ) );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "https://site.example.com/b/mycontent" );

        localContext.put( Constants.SITE_BASE_URL, "https://site.example.com/" );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "linkedcontent" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetLinkPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "https://site.example.com/b/mycontent", result.get( "url" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrl( captor.capture() );
        assertEquals( "linkedcontent", captor.getValue().getId() );
        assertEquals( "https://site.example.com/", captor.getValue().getBaseUrl() );

        ArgumentCaptor<PageUrlParams> partsCaptor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( partsCaptor.capture() );
        assertNull( partsCaptor.getValue().getBaseUrl() );
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
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name.jpg", "?download", "myproject", "contentid",
                                    "hash", "name.jpg" ) );

        when( selectionSet.contains( "url" ) ).thenReturn( false );

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
    public void testLinkMediaUrlUsesAttachmentBaseUrlFromSiteKey()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlGeneratorService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlGeneratorService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name.jpg", "", "myproject", "contentid", "hash",
                                    "name.jpg" ) );
        when( portalUrlGeneratorService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "mediaUrl" );

        localContext.put( Constants.ATTACHMENT_BASE_URL, "https://site.example.com/_" );

        ContentService contentService = Mockito.mock( ContentService.class );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( Mockito.mock( Content.class ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result =
            new GetLinkMediaUrlDataFetcher( portalUrlGeneratorService, contentService ).get( environment );

        assertEquals( "mediaUrl", result.get( "url" ) );

        ArgumentCaptor<AttachmentUrlGeneratorParams> captor = ArgumentCaptor.forClass( AttachmentUrlGeneratorParams.class );
        verify( portalUrlGeneratorService ).attachmentUrl( captor.capture() );
        assertEquals( "https://site.example.com/_", captor.getValue().getMediaBaseUrl() );

        ArgumentCaptor<AttachmentUrlGeneratorParams> partsCaptor = ArgumentCaptor.forClass( AttachmentUrlGeneratorParams.class );
        verify( portalUrlGeneratorService ).attachmentUrlParts( partsCaptor.capture() );
        assertNull( partsCaptor.getValue().getMediaBaseUrl() );
    }

    @Test
    public void testImageUrlSkipsPartsWhenOnlyUrlSelected()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );

        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        final Map<String, Object> result = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "imageUrl", result.get( "url" ) );
        assertFalse( result.containsKey( "path" ) );
        verify( portalUrlService, never() ).imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) );
    }

    @Test
    public void testAttachmentUrlSkipsPartsWhenOnlyUrlSelected()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "attachmentUrl" );

        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetAttachmentUrlByIdDataFetcher( portalUrlService ).get( environment );

        assertEquals( "attachmentUrl", result.get( "url" ) );
        // intent is computed locally and stays available without the parts call
        assertEquals( "inline", result.get( "intent" ) );
        assertFalse( result.containsKey( "path" ) );
        verify( portalUrlService, never() ).attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) );
    }

    @Test
    public void testPageUrlSkipsPartsWhenOnlyUrlSelected()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "/site/myproject/draft/mysite/path" );

        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        final Map<String, Object> result = new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/site/myproject/draft/mysite/path", result.get( "url" ) );
        assertFalse( result.containsKey( "path" ) );
        verify( portalUrlService, never() ).pageUrlParts( Mockito.any( PageUrlParams.class ) );
    }

    @Test
    public void testLinkMediaUrlSkipsPartsWhenOnlyUrlSelected()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlGeneratorService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlGeneratorService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "mediaUrl" );

        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        ContentService contentService = Mockito.mock( ContentService.class );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( Mockito.mock( Content.class ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "contentid" );
        source.put( "intent", "download" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetLinkMediaUrlDataFetcher( portalUrlGeneratorService, contentService ).get( environment );

        assertEquals( "mediaUrl", result.get( "url" ) );
        assertEquals( "download", result.get( "intent" ) );
        assertFalse( result.containsKey( "path" ) );
        verify( portalUrlGeneratorService, never() ).attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) );
    }

    @Test
    public void testLinkMediaUrlSkipsContentLoadWhenOnlyIntentSelected()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlGeneratorService = Mockito.mock( PortalUrlGeneratorService.class );

        when( selectionSet.contains( "url" ) ).thenReturn( false );
        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( false );

        ContentService contentService = Mockito.mock( ContentService.class );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "contentid" );
        source.put( "intent", "download" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetLinkMediaUrlDataFetcher( portalUrlGeneratorService, contentService ).get( environment );

        assertEquals( "download", result.get( "intent" ) );
        assertFalse( result.containsKey( "url" ) );
        assertFalse( result.containsKey( "path" ) );

        // neither url nor parts selected: the target content must not be fetched from storage
        verify( contentService, never() ).getById( Mockito.any( ContentId.class ) );
        verify( portalUrlGeneratorService, never() ).attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) );
        verify( portalUrlGeneratorService, never() ).attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) );
    }

    @Test
    public void testPageUrlWithoutSiteBaseUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "/site/myproject/draft/mysite/path" );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn( new PageUrlParts( "/mysite/path", "" ) );

        assertEquals( "/site/myproject/draft/mysite/path",
                      new GetPageUrlDataFetcher( portalUrlService ).get( environment ).get( "url" ) );

        // without a siteKey-resolved base URL the field uses the same request-aware call as content links in processHtml:
        // no baseUrl and no project/branch on the params, so preferSiteRequest can take effect
        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrl( captor.capture() );
        assertNull( captor.getValue().getBaseUrl() );
        assertNull( captor.getValue().getProjectName() );
        assertNull( captor.getValue().getBranch() );
    }

    @Test
    public void testPageUrlUsesSiteBaseUrlFromSiteKey()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "https://site.example.com/path" );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn( new PageUrlParts( "/path", "" ) );

        // present only when siteKey resolved to a configured Base URL
        localContext.put( Constants.SITE_BASE_URL, "https://site.example.com/" );

        assertEquals( "https://site.example.com/path",
                      new GetPageUrlDataFetcher( portalUrlService ).get( environment ).get( "url" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrl( captor.capture() );
        assertEquals( "https://site.example.com/", captor.getValue().getBaseUrl() );

        // parts never carry a base URL: components are independent of siteKey and request
        ArgumentCaptor<PageUrlParams> partsCaptor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( partsCaptor.capture() );
        assertNull( partsCaptor.getValue().getBaseUrl() );
    }


}
