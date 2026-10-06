package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.AttachmentUrlParts;
import com.enonic.xp.portal.url.AttachmentUrlPartsParams;
import com.enonic.xp.portal.url.ImageUrlParams;
import com.enonic.xp.portal.url.ImageUrlParts;
import com.enonic.xp.portal.url.ImageUrlPartsParams;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PageUrlParts;
import com.enonic.xp.portal.url.PageUrlPartsParams;
import com.enonic.xp.portal.url.PortalScope;
import com.enonic.xp.portal.url.PortalScopeParams;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.site.SiteConfigs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        when( environment.getGraphQlContext() ).thenReturn( GraphQLContext.newContext().build() );

        selectionSet = Mockito.mock( DataFetchingFieldSelectionSet.class );
        when( selectionSet.containsAnyOf( Mockito.anyString(), Mockito.any( String[].class ) ) ).thenReturn( true );
        when( environment.getSelectionSet() ).thenReturn( selectionSet );
    }

    @Test
    public void testAttachmentUrlByName()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlPartsParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name", "", "myproject", "contentid", "hash", "name" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> result = new GetAttachmentUrlByNameDataFetcher( portalUrlService ).get( environment );

        assertFalse( result.containsKey( "apiUrl" ) );
        assertEquals( "/media:attachment/myproject/contentid:hash/name", result.get( "path" ) );
        assertFalse( result.containsKey( "url" ) );
        verify( portalUrlService, never() ).attachmentUrl( Mockito.any( AttachmentUrlParams.class ) );
    }

    @Test
    public void testImageUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlPartsParams.class ) ) ).thenReturn(
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

        final Map<String, Object> result = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", result.get( "path" ) );
        assertFalse( result.containsKey( "url" ) );
        verify( portalUrlService, never() ).imageUrl( Mockito.any( ImageUrlParams.class ) );
    }

    @Test
    public void testAttachmentUrlById()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlPartsParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name", "", "myproject", "contentid", "hash", "name" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "download" ) ).thenReturn( false );

        final Map<String, Object> result = new GetAttachmentUrlByIdDataFetcher( portalUrlService ).get( environment );

        assertFalse( result.containsKey( "apiUrl" ) );
        assertEquals( "/media:attachment/myproject/contentid:hash/name", result.get( "path" ) );
        assertFalse( result.containsKey( "url" ) );
        verify( portalUrlService, never() ).attachmentUrl( Mockito.any( AttachmentUrlParams.class ) );
    }

    @Test
    public void testImageUrlCarriesNoBase()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlPartsParams.class ) ) ).thenReturn(
            new ImageUrlParts( "/media:image/myproject:draft/contentid:hash/scale/name.jpg", "", "myproject:draft", "contentid", "hash",
                               "scale", "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );

        final Map<String, Object> result = new GetImageUrlDataFetcher( portalUrlService ).get( environment );

        assertFalse( result.containsKey( "apiUrl" ) );

        verify( portalUrlService ).imageUrlParts( Mockito.any( ImageUrlPartsParams.class ) );
        verify( portalUrlService, never() ).imageUrl( Mockito.any( ImageUrlParams.class ) );
    }

    @Test
    public void testImageUrlParts()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.imageUrlParts( Mockito.any( ImageUrlPartsParams.class ) ) ).thenReturn(
            new ImageUrlParts( "/media:image/myproject:draft/contentid:hash/max-300/name.jpg", "?quality=85", "myproject:draft",
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

        assertFalse( parts.containsKey( "url" ) );
        verify( portalUrlService, never() ).imageUrl( Mockito.any( ImageUrlParams.class ) );
    }

    @Test
    public void testAttachmentUrlPartsById()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.attachmentUrlParts( Mockito.any( AttachmentUrlPartsParams.class ) ) ).thenReturn(
            new AttachmentUrlParts( "/media:attachment/myproject/contentid:hash/name.jpg", "", "myproject", "contentid", "hash",
                                    "name.jpg" ) );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentid" );

        when( environment.getSource() ).thenReturn( source );

        final Map<String, Object> parts = new GetAttachmentUrlByIdDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/media:attachment/myproject/contentid:hash/name.jpg", parts.get( "path" ) );
        assertEquals( "", parts.get( "queryString" ) );
        assertNull( parts.get( "scale" ) );
        assertEquals( "inline", parts.get( "intent" ) );

        verify( portalUrlService, never() ).attachmentUrl( Mockito.any( AttachmentUrlParams.class ) );
    }

    @Test
    public void testPageUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        final PortalScope scope = portalScope( portalUrlService, "/mysite" );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlPartsParams.class ) ) ).thenReturn(
            new PageUrlParts( "https://site.example.com", "/b/mycontent", "?a=1" ) );

        localContext.put( Constants.SITE_ARG, "/mysite" );

        final Map<String, Object> parts = new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "https://site.example.com", parts.get( "baseUrl" ) );
        assertEquals( "/b/mycontent", parts.get( "path" ) );
        assertEquals( "?a=1", parts.get( "queryString" ) );
        assertFalse( parts.containsKey( "url" ) );

        ArgumentCaptor<PageUrlPartsParams> captor = ArgumentCaptor.forClass( PageUrlPartsParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        assertSame( scope, captor.getValue().getScope() );
        assertEquals( "/mysite", portalScopeKey( portalUrlService ) );

        verify( portalUrlService, never() ).pageUrl( Mockito.any( PageUrlParams.class ) );
    }

    @Test
    public void testPageUrlParamsWithNullValues()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlPartsParams.class ) ) ).thenReturn(
            new PageUrlParts( null, "/b/mycontent", "" ) );

        final Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put( "flag", null );
        queryParams.put( "tags", Arrays.asList( "a", null, "b" ) );
        queryParams.put( "page", 2 );
        when( environment.getArgument( "params" ) ).thenReturn( queryParams );

        new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        ArgumentCaptor<PageUrlPartsParams> captor = ArgumentCaptor.forClass( PageUrlPartsParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        assertEquals( Map.of( "flag", List.of(), "tags", List.of( "a", "b" ), "page", List.of( "2" ) ), captor.getValue().getQueryParams() );
    }

    @Test
    public void testPageUrlWithoutConfiguredBaseUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlPartsParams.class ) ) ).thenReturn(
            new PageUrlParts( null, "/b/mycontent", "" ) );

        localContext.put( Constants.SITE_ARG, "/mysite" );

        final Map<String, Object> parts = new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        assertTrue( parts.containsKey( "baseUrl" ) );
        assertNull( parts.get( "baseUrl" ) );
        assertEquals( "/b/mycontent", parts.get( "path" ) );
    }

    @Test
    public void testPageUrlWithoutSiteKeyBelongsToProject()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        final PortalScope scope = portalScope( portalUrlService, "/" );
        when( portalUrlService.pageUrlParts( Mockito.any( PageUrlPartsParams.class ) ) ).thenReturn(
            new PageUrlParts( null, "/mysite/b/mycontent", "" ) );

        final Map<String, Object> parts = new GetPageUrlDataFetcher( portalUrlService ).get( environment );

        assertEquals( "/mysite/b/mycontent", parts.get( "path" ) );

        ArgumentCaptor<PageUrlPartsParams> captor = ArgumentCaptor.forClass( PageUrlPartsParams.class );
        verify( portalUrlService ).pageUrlParts( captor.capture() );
        assertSame( scope, captor.getValue().getScope() );
        assertNull( portalScopeKey( portalUrlService ) );
    }

    @Test
    public void testImageUrlSkipsPartsWhenNoneSelected()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );

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
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );

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
    public void testLinkPageUrl()
    {
        localContext.put( Constants.SITE_ARG, "/mysite" );

        final Map<String, Object> pageUrl = Map.of( "path", "/b/mycontent", "queryString", "?a=1" );
        when( environment.getSource() ).thenReturn( Map.of( "contentId", "linkedcontent", "pageUrl", pageUrl ) );

        assertEquals( pageUrl, new GetLinkPageUrlDataFetcher().get( environment ) );
    }

    @Test
    public void testLinkPageUrlWithoutSiteKey()
    {
        final Map<String, Object> pageUrl = Map.of( "path", "/mysite/b", "queryString", "" );
        when( environment.getSource() ).thenReturn( Map.of( "contentId", "linkedcontent", "pageUrl", pageUrl ) );

        assertEquals( pageUrl, new GetLinkPageUrlDataFetcher().get( environment ) );
    }

    @Test
    public void testLinkPageUrlIsNullForMediaLinks()
    {
        when( environment.getSource() ).thenReturn( Map.of( "media", Map.of() ) );

        assertNull( new GetLinkPageUrlDataFetcher().get( environment ) );
    }

    private static PortalScope portalScope( final PortalUrlService portalUrlService, final String key )
    {
        final PortalScope scope = new PortalScope( ProjectName.from( "myproject" ), Branch.from( "master" ), ContentPath.from( key ), SiteConfigs.empty() );
        when( portalUrlService.portalScope( Mockito.any( PortalScopeParams.class ) ) ).thenReturn( scope );
        return scope;
    }

    private static String portalScopeKey( final PortalUrlService portalUrlService )
    {
        final ArgumentCaptor<PortalScopeParams> captor = ArgumentCaptor.forClass( PortalScopeParams.class );
        verify( portalUrlService ).portalScope( captor.capture() );
        final PortalScopeParams params = captor.getValue();
        return params.getContentPath() != null ? params.getContentPath().toString() : Objects.toString( params.getContentId(), null );
    }
}
