package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.RenderMode;
import com.enonic.xp.repository.RepositoryId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GuillotineDataFetcherTest
{
    @AfterEach
    public void cleanUp()
    {
        PortalRequestAccessor.remove();
    }

    @Test
    public void argumentsIgnoredWhenPortalRequestHasMode()
        throws Exception
    {
        final PortalRequest portalRequest = new PortalRequest();
        portalRequest.setMode( RenderMode.EDIT );
        PortalRequestAccessor.set( portalRequest );

        final Map<Object, Object> localContext = fetchLocalContext( environmentWithArgs( "otherproject", "master" ) );

        assertEquals( "myproject", localContext.get( Constants.PROJECT_ARG ) );
        assertEquals( "draft", localContext.get( Constants.BRANCH_ARG ) );
    }

    @Test
    public void argumentsWinWhenPortalRequestHasNoMode()
        throws Exception
    {
        PortalRequestAccessor.set( new PortalRequest() );

        final Map<Object, Object> localContext = fetchLocalContext( environmentWithArgs( "otherproject", "master" ) );

        assertEquals( "otherproject", localContext.get( Constants.PROJECT_ARG ) );
        assertEquals( "master", localContext.get( Constants.BRANCH_ARG ) );
    }

    @Test
    public void argumentsWinWithoutPortalRequest()
        throws Exception
    {
        final Map<Object, Object> localContext = fetchLocalContext( environmentWithArgs( "otherproject", "master" ) );

        assertEquals( "otherproject", localContext.get( Constants.PROJECT_ARG ) );
        assertEquals( "master", localContext.get( Constants.BRANCH_ARG ) );
    }

    @Test
    public void contextUsedWithoutArguments()
        throws Exception
    {
        final Map<Object, Object> localContext = fetchLocalContext( environmentWithArgs( null, null ) );

        assertEquals( "myproject", localContext.get( Constants.PROJECT_ARG ) );
        assertEquals( "draft", localContext.get( Constants.BRANCH_ARG ) );
    }

    private DataFetchingEnvironment environmentWithArgs( final String project, final String branch )
    {
        final DataFetchingEnvironment environment = Mockito.mock( DataFetchingEnvironment.class );
        when( environment.getArgument( Constants.PROJECT_ARG ) ).thenReturn( project );
        when( environment.getArgument( Constants.BRANCH_ARG ) ).thenReturn( branch );
        return environment;
    }

    private Map<Object, Object> fetchLocalContext( final DataFetchingEnvironment environment )
        throws Exception
    {
        // no siteKey argument, so the ServiceFacade is never touched
        final GuillotineDataFetcher fetcher = new GuillotineDataFetcher( () -> null );

        final DataFetcherResult<?> result = ContextBuilder.create()
            .repositoryId( RepositoryId.from( "com.enonic.cms.myproject" ) )
            .branch( Branch.from( "draft" ) )
            .build()
            .callWith( () -> (DataFetcherResult<?>) fetcher.get( environment ) );

        return CastHelper.cast( result.getLocalContext() );
    }
}
