package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.site.Site;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class GetSiteDataFetcherTest
{
    private DataFetchingEnvironment environment;

    @BeforeEach
    public void setUp()
    {
        final PortalRequest portalRequest = new PortalRequest();
        portalRequest.setRepositoryId( RepositoryId.from( "myproject" ) );
        portalRequest.setBranch( Branch.from( "draft" ) );
        PortalRequestAccessor.set( portalRequest );

        Map<String, Object> localContext = new HashMap<>();
        localContext.put( Constants.GUILLOTINE_TARGET_PROJECT_CTX, "myproject" );
        localContext.put( Constants.GUILLOTINE_TARGET_BRANCH_CTX, "draft" );
        localContext.put( Constants.GUILLOTINE_TARGET_SITE_CTX, "/siteKey" );

        environment = Mockito.mock( DataFetchingEnvironment.class );
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
        ContentService contentService = Mockito.mock( ContentService.class );
        when( contentService.findNearestSiteByPath( Mockito.any() ) ).thenReturn(
            Site.create().name( "site" ).type( ContentTypeName.site() ).parentPath( ContentPath.ROOT ).data(
                new PropertyTree() ).displayName( "Site" ).id( ContentId.from( "siteId" ) ).build() );

        GetSiteDataFetcher instance = new GetSiteDataFetcher( contentService );
        assertNotNull( instance.get( environment ) );
    }
}
