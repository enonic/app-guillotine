package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetAttachmentUrlByNameDataFetcherTest
{
    @Test
    public void testGet()
        throws Exception
    {
        DataFetchingEnvironment environment = mock( DataFetchingEnvironment.class );

        final Map<String, Object> localContext = new HashMap<>();

        localContext.put( Constants.PROJECT_ARG, "myproject" );
        localContext.put( Constants.BRANCH_ARG, "draft" );
        localContext.put( Constants.CURRENT_CONTENT_FIELD, ContentFixtures.createContentAsMap() );

        when( environment.getSource() ).thenReturn( Map.of( "name", "Name" ) );
        when( environment.getLocalContext() ).thenReturn( localContext );
        when( environment.getArgument( "download" ) ).thenReturn( true );
        when( environment.getArgument( "type" ) ).thenReturn( null );

        Map<String, Object> params = new LinkedHashMap<>();
        params.put( "a", "1" );
        params.put( "b", List.of( 2, 3 ) );
        params.put( "c", null );

        when( environment.getArgument( "params" ) ).thenReturn( params );

        PortalUrlGeneratorService portalUrlService = mock( PortalUrlGeneratorService.class );

        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn( "url?a=1&b=2&b=3&c" );

        GetAttachmentUrlByNameDataFetcher instance = new GetAttachmentUrlByNameDataFetcher( portalUrlService );

        String attachmentUrl = instance.get( environment );

        assertEquals( "url?a=1&b=2&b=3&c", attachmentUrl );
    }
}
