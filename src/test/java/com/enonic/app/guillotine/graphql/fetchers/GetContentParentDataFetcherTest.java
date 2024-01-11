package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.repository.RepositoryId;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetContentParentDataFetcherTest
{
    private ContentService contentService;

    private DataFetchingEnvironment environment;

    @BeforeEach
    public void setUp()
    {
        this.contentService = mock( ContentService.class );

        final PortalRequest portalRequest = new PortalRequest();
        portalRequest.setRepositoryId( RepositoryId.from( "myproject" ) );
        portalRequest.setBranch( Branch.from( "draft" ) );
        PortalRequestAccessor.set( portalRequest );

        Map<String, Object> localContext = new HashMap<>();
        localContext.put( Constants.PROJECT_ARG, "myproject" );
        localContext.put( Constants.BRANCH_ARG, "draft" );

        this.environment = mock( DataFetchingEnvironment.class );
        when( environment.getLocalContext() ).thenReturn( localContext );
    }

    @AfterEach
    public void cleanUp()
    {
        PortalRequestAccessor.remove();
    }

    @Test
    public void testGet()
        throws Exception
    {
        when( contentService.getByPath( Mockito.any( ContentPath.class ) ) ).thenReturn( ContentFixtures.createMediaContent() );

        Map<String, Object> source = new HashMap<>();
        source.put( "_path", "/a/b" );

        when( environment.getSource() ).thenReturn( source );

        GetContentParentDataFetcher instance = new GetContentParentDataFetcher( contentService );
        Object result = instance.get( environment );

        assertNotNull( result );
    }

    @Test
    public void testGetParentForRoot()
        throws Exception
    {
        Map<String, Object> source = new HashMap<>();
        source.put( "_path", "/a" );

        when( environment.getSource() ).thenReturn( source );

        GetContentParentDataFetcher instance = new GetContentParentDataFetcher( contentService );
        Object result = instance.get( environment );

        assertNull( result );
    }
}
