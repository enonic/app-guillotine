package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.ImageUrlGeneratorParams;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.MediaUrlParts;
import com.enonic.xp.portal.url.PageUrlParts;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;
import com.enonic.xp.portal.url.PortalUrlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UrlFieldDataFetcherTest
{

    private DataFetchingEnvironment environment;

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
    }

    @Test
    public void testAttachmentUrlByName()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "attachmentUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );

        GetAttachmentUrlByNameDataFetcher instance = new GetAttachmentUrlByNameDataFetcher( portalUrlService );
        assertEquals( "attachmentUrl", instance.get( environment ) );
    }

    @Test
    public void testImageUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );
        when( environment.getArgument( "quality" ) ).thenReturn( 1 );
        when( environment.getArgument( "background" ) ).thenReturn( "background" );
        when( environment.getArgument( "format" ) ).thenReturn( "format" );
        when( environment.getArgument( "filter" ) ).thenReturn( "filter" );

        GetImageUrlDataFetcher instance = new GetImageUrlDataFetcher( portalUrlService );
        assertEquals( "imageUrl", instance.get( environment ) );
    }

    @Test
    public void testAttachmentUrlById()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "attachmentUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "download" ) ).thenReturn( false );

        GetAttachmentUrlByIdDataFetcher instance = new GetAttachmentUrlByIdDataFetcher( portalUrlService );
        assertEquals( "attachmentUrl", instance.get( environment ) );
    }


    @Test
    public void testImageUrlFallsBackToSiteBaseUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        localContext.put( Constants.SITE_BASE_URL, "https://site.example.com/" );

        new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        ArgumentCaptor<ImageUrlGeneratorParams> captor = ArgumentCaptor.forClass( ImageUrlGeneratorParams.class );
        verify( portalUrlService ).imageUrl( captor.capture() );
        // the site base URL is a mount base: media APIs live under its "_" endpoint segment
        assertEquals( "https://site.example.com/_", captor.getValue().getMediaBaseUrl() );
        assertNull( captor.getValue().getBaseUrl() );
    }

    @Test
    public void testImageUrlWithoutAnyBaseUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );

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

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        localContext.put( Constants.SITE_BASE_URL, "https://site.example.com/" );

        assertEquals( "/site/repo/draft/app/_/media:image/myproject:draft/contentid:hash/scale/name.jpg",
                      new GetImageUrlDataFetcher( portalUrlService ).get( environment ) );
    }


    @Test
    public void testImageUrlParts()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            MediaUrlParts.create()
                .setPath( "/media:image/myproject:draft/contentid:hash/max-300/name.jpg" )
                .setQueryString( "?quality=85" )
                .setContext( "myproject:draft" )
                .setId( "contentid" )
                .setHash( "hash" )
                .setScale( "max-300" )
                .setName( "name.jpg" )
                .build() );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "max(300)" );

        final Map<String, Object> parts = new GetImageUrlPartsDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/media:image/myproject:draft/contentid:hash/max-300/name.jpg", parts.get( "path" ) );
        assertEquals( "?quality=85", parts.get( "queryString" ) );
        assertEquals( "myproject:draft", parts.get( "context" ) );
        assertEquals( "contentid", parts.get( "id" ) );
        assertEquals( "hash", parts.get( "hash" ) );
        assertEquals( "max-300", parts.get( "scale" ) );
        assertEquals( "name.jpg", parts.get( "name" ) );
        assertNull( parts.get( "intent" ) );
    }

    @Test
    public void testAttachmentUrlPartsById()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            MediaUrlParts.create()
                .setPath( "/media:attachment/myproject/contentid:hash/name.jpg" )
                .setQueryString( "" )
                .setContext( "myproject" )
                .setId( "contentid" )
                .setHash( "hash" )
                .setName( "name.jpg" )
                .build() );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts = new GetAttachmentUrlPartsByIdDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/media:attachment/myproject/contentid:hash/name.jpg", parts.get( "path" ) );
        assertEquals( "", parts.get( "queryString" ) );
        assertNull( parts.get( "scale" ) );
        assertEquals( "inline", parts.get( "intent" ) );
    }

    @Test
    public void testPageUrlParts()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn(
            PageUrlParts.create().setPath( "/b/mycontent" ).setQueryString( "?a=1" ).build() );

        final Map<String, Object> parts = new GetPageUrlPartsDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/b/mycontent", parts.get( "path" ) );
        assertEquals( "?a=1", parts.get( "queryString" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        // parts never carry a base URL: components are independent of siteKey and request
        assertNull( captor.getValue().getBaseUrl() );
    }

    @Test
    public void testLinkPageUrlParts()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlParams.class ) ) ).thenReturn(
            PageUrlParts.create().setPath( "/b/mycontent" ).setQueryString( "" ).build() );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "linkedcontent" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts = new GetLinkPageUrlPartsDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/b/mycontent", parts.get( "path" ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        assertEquals( "linkedcontent", captor.getValue().getId() );
    }

    @Test
    public void testLinkPageUrlPartsIsNullForMediaLinks()
        throws Exception
    {
        // media link projections carry the contentId inside the media object, not on the link
        Map<String, Object> source = new HashMap<>();
        when( environment.getSource() ).thenReturn( source );

        assertNull( new GetLinkPageUrlPartsDataFetcher( Mockito.mock( PortalUrlService.class ) ).get( environment ) );
    }

    @Test
    public void testLinkMediaUrlPartsHonorsDownloadIntent()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlGeneratorService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlGeneratorService.attachmentUrlParts( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            MediaUrlParts.create()
                .setPath( "/media:attachment/myproject/contentid:hash/name.jpg" )
                .setQueryString( "?download" )
                .build() );

        ContentService contentService = Mockito.mock( ContentService.class );
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( Mockito.mock( Content.class ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "contentId", "contentid" );
        source.put( "intent", "download" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts =
            new GetLinkMediaUrlPartsDataFetcher( portalUrlGeneratorService, contentService ).get( environment );

        assertEquals( "?download", parts.get( "queryString" ) );
        assertEquals( "download", parts.get( "intent" ) );

        ArgumentCaptor<AttachmentUrlGeneratorParams> captor = ArgumentCaptor.forClass( AttachmentUrlGeneratorParams.class );
        verify( portalUrlGeneratorService ).attachmentUrlParts( captor.capture() );
        assertTrue( captor.getValue().isDownload() );
    }

    @Test
    public void testPageUrlWithoutSiteBaseUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "/site/myproject/draft/mysite/path" );

        assertEquals( "/site/myproject/draft/mysite/path", new GetPageUrlDataFetcher( portalUrlService ).get( environment ) );

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

        // present only when siteKey resolved to a configured Base URL
        localContext.put( Constants.SITE_BASE_URL, "https://site.example.com/" );

        assertEquals( "https://site.example.com/path", new GetPageUrlDataFetcher( portalUrlService ).get( environment ) );

        ArgumentCaptor<PageUrlParams> captor = ArgumentCaptor.forClass( PageUrlParams.class );
        verify( portalUrlService ).pageUrl( captor.capture() );
        assertEquals( "https://site.example.com/", captor.getValue().getBaseUrl() );
    }


}
