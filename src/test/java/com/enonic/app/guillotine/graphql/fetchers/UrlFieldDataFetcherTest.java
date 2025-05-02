package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.ImageUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class UrlFieldDataFetcherTest
{

    private DataFetchingEnvironment environment;

    @BeforeEach
    public void setUp()
    {
        final Map<String, Object> localContext = new HashMap<>();

        localContext.put( Constants.PROJECT_ARG, "myproject" );
        localContext.put( Constants.BRANCH_ARG, "draft" );
        localContext.put( Constants.CONTENTS_FIELD, Map.of( "contentId", ContentFixtures.createContentAsMap() ) );
        localContext.put( Constants.CONTENT_ID_FIELD, "contentId" );

        environment = Mockito.mock( DataFetchingEnvironment.class );
        when( environment.getLocalContext() ).thenReturn( localContext );
    }

    @Test
    public void testAttachmentUrlByName()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "attachmentUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( Constants.CONTENT_ID_FIELD, "contentId" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );

        GetAttachmentUrlByNameDataFetcher instance = new GetAttachmentUrlByNameDataFetcher( portalUrlService );
        assertEquals( "attachmentUrl", instance.get( environment ) );
    }

    @Test
    public void testImageUrl()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.imageUrl( Mockito.any( ImageUrlGeneratorParams.class ) ) ).thenReturn( "imageUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentId" );

        when( environment.getSource() ).thenReturn( source );

        when( environment.getArgument( "scale" ) ).thenReturn( "scale" );
        when( environment.getArgument( "quality" ) ).thenReturn( 1 );
        when( environment.getArgument( "background" ) ).thenReturn( "background" );
        when( environment.getArgument( "format" ) ).thenReturn( "format" );
        when( environment.getArgument( "filter" ) ).thenReturn( "filter" );
        when( environment.getArgument( "type" ) ).thenReturn( "type" );

        GetImageUrlDataFetcher instance = new GetImageUrlDataFetcher( portalUrlService );
        assertEquals( "imageUrl", instance.get( environment ) );
    }

    @Test
    public void testAttachmentUrlById()
        throws Exception
    {
        PortalUrlService portalUrlService = Mockito.mock( PortalUrlService.class );
        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "attachmentUrl" );

        Map<String, Object> source = new HashMap<>();
        source.put( "_id", "contentId" );
        source.put( "name", "name" );

        when( environment.getSource() ).thenReturn( source );
        when( environment.getArgument( "download" ) ).thenReturn( false );

        GetAttachmentUrlByIdDataFetcher instance = new GetAttachmentUrlByIdDataFetcher( portalUrlService );
        assertEquals( "attachmentUrl", instance.get( environment ) );
    }
}
