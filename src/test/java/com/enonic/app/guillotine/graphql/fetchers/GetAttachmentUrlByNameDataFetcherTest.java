package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.repository.RepositoryId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetAttachmentUrlByNameDataFetcherTest
{
    @Test
    public void testGet()
        throws Exception
    {
        PortalRequest portalRequest = mock( PortalRequest.class );
        when( portalRequest.getRepositoryId() ).thenReturn( RepositoryId.from( "com.enonic.cms.default" ) );
        when( portalRequest.getBranch() ).thenReturn( Branch.from( "master" ) );

        PortalRequestAccessor.set( portalRequest );

        DataFetchingEnvironment environment = mock( DataFetchingEnvironment.class );

        when( environment.getSource() ).thenReturn( Map.of( "name", "Name", Constants.CONTENT_ID_FIELD, "contentId" ) );
        when( environment.getLocalContext() ).thenReturn( new HashMap<>() );
        when( environment.getArgument( "download" ) ).thenReturn( "true" );
        when( environment.getArgument( "type" ) ).thenReturn( null );

        Map<String, Object> params = new LinkedHashMap<>();
        params.put( "a", "1" );
        params.put( "b", List.of( 2, 3 ) );
        params.put( "c", null );

        when( environment.getArgument( "params" ) ).thenReturn( params );

        PortalUrlService portalUrlService = mock( PortalUrlService.class );

        when( portalUrlService.attachmentUrl( Mockito.any( AttachmentUrlParams.class ) ) ).thenReturn( "url?a=1&b=2&b=3&c" );

        GetAttachmentUrlByNameDataFetcher instance = new GetAttachmentUrlByNameDataFetcher( portalUrlService );

        String attachmentUrl = instance.get( environment );

        assertEquals( "url?a=1&b=2&b=3&c", attachmentUrl );
    }
}
