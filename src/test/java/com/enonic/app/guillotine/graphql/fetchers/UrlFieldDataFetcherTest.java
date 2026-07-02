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
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;
import com.enonic.xp.portal.url.PortalUrlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    public void testImageUrlWithMediaBaseUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        localContext.put( Constants.MEDIA_BASE_URL, "https://config.example.com/" );

        new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        // with mediaBaseUrl set, the generator is asked for a root-relative URL ("/" baseUrl)
        ArgumentCaptor<ImageUrlGeneratorParams> captor = ArgumentCaptor.forClass( ImageUrlGeneratorParams.class );
        verify( portalUrlService ).imageUrl( captor.capture() );
        assertEquals( "/", captor.getValue().getBaseUrl() );
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
        assertEquals( "https://site.example.com/", captor.getValue().getBaseUrl() );
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
    public void testImageUrlPrependsMediaBaseUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn(
            "/_/media:image/myproject:draft/contentid:hash/scale/name.jpg" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        localContext.put( Constants.MEDIA_BASE_URL, "https://media.example.com/whatever" );

        assertEquals( "https://media.example.com/whatever/media:image/myproject:draft/contentid:hash/scale/name.jpg",
                      new GetImageUrlDataFetcher( portalUrlService ).get( environment ) );
    }

    @Test
    public void testImageUrlKeepsEndpointSegmentWithoutMediaBaseUrl()
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
    public void testAttachmentUrlByIdWithMediaBaseUrl()
        throws Exception
    {
        PortalUrlGeneratorService portalUrlService = Mockito.mock( PortalUrlGeneratorService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            "/_/media:attachment/myproject:draft/contentid:hash/name.jpg" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );
        localContext.put( Constants.MEDIA_BASE_URL, "https://media.example.com/whatever" );

        final String result = new GetAttachmentUrlByIdDataFetcher( portalUrlService ).get( environment );

        ArgumentCaptor<AttachmentUrlGeneratorParams> captor = ArgumentCaptor.forClass( AttachmentUrlGeneratorParams.class );
        verify( portalUrlService ).attachmentUrl( captor.capture() );
        assertEquals( "/", captor.getValue().getBaseUrl() );
        assertEquals( "https://media.example.com/whatever/media:attachment/myproject:draft/contentid:hash/name.jpg", result );
    }

    @Test
    public void testPageUrlWithoutPageBaseUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "/site/repo/branch/path" );

        assertEquals( "/site/repo/branch/path", new GetPageUrlDataFetcher( portalUrlService ).get( environment ) );
    }

    @Test
    public void testPageUrlWithPageBaseUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "/site/repo/branch/path" );

        localContext.put( Constants.PAGE_BASE_URL, "https://pages.example.com/" );

        assertEquals( "https://pages.example.com/site/repo/branch/path",
                      new GetPageUrlDataFetcher( portalUrlService ).get( environment ) );
    }
}
