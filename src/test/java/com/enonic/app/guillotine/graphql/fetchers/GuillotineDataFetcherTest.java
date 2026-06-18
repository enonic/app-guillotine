package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.repository.RepositoryId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class GuillotineDataFetcherTest
{
    @Test
    public void resolves_project_and_branch_without_portal_request()
        throws Exception
    {
        // No siteKey arg and no PortalRequest (the library case) - must not NPE; project/branch fall back to the context.
        final DataFetchingEnvironment environment = mock( DataFetchingEnvironment.class );

        final GuillotineDataFetcher fetcher = new GuillotineDataFetcher( () -> null, () -> mock( ServiceFacade.class ) );

        final Context context =
            ContextBuilder.create().repositoryId( RepositoryId.from( "com.enonic.cms.myproject" ) ).branch( Branch.from( "draft" ) ).build();

        final DataFetcherResult<?> result = (DataFetcherResult<?>) context.callWith( () -> fetcher.get( environment ) );

        final Map<?, ?> localContext = (Map<?, ?>) result.getLocalContext();
        assertEquals( "myproject", localContext.get( Constants.PROJECT_ARG ) );
        assertEquals( "draft", localContext.get( Constants.BRANCH_ARG ) );
        assertNull( localContext.get( Constants.SITE_ARG ) );
    }
}
