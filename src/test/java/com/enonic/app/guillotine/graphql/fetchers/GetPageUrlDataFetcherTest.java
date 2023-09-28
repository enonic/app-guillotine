package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.repository.RepositoryId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetPageUrlDataFetcherTest
{
    private DataFetchingEnvironment environment;

    @BeforeEach
    public void setUp()
    {
        PortalRequest portalRequest = mock( PortalRequest.class );
        when( portalRequest.getRepositoryId() ).thenReturn( RepositoryId.from( "com.enonic.cms.default" ) );
        when( portalRequest.getBranch() ).thenReturn( Branch.from( "master" ) );
        PortalRequestAccessor.set( portalRequest );

        environment = mock( DataFetchingEnvironment.class );

        when( environment.getSource() ).thenReturn( Map.of( "_id", "contentId" ) );
        when( environment.getLocalContext() ).thenReturn( Map.of() );
        when( environment.getGraphQlContext() ).thenReturn( GraphQLContext.getDefault() );
        when( environment.getArgument( "type" ) ).thenReturn( "server" );
    }

    @Test
    public void testGet()
        throws Exception
    {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put( "a", "1" );
        params.put( "b", List.of( 2, 3 ) );
        params.put( "c", null );

        when( environment.getArgument( "params" ) ).thenReturn( params );

        PortalUrlService portalUrlService = mock( PortalUrlService.class );
        when( portalUrlService.pageUrl( Mockito.any( PageUrlParams.class ) ) ).thenReturn( "url?a=1&b=2&b=3&c" );

        GetPageUrlDataFetcher instance = new GetPageUrlDataFetcher( portalUrlService );

        String pageUrl = instance.get( environment );

        assertEquals( "url?a=1&b=2&b=3&c", pageUrl );
    }
}
