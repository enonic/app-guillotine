package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetContentProjectDataFetcherTest
{
    @Test
    public void test()
        throws Exception
    {
        Map<String, Object> localContext = new HashMap<>();
        localContext.put( Constants.PROJECT_ARG, "project" );
        localContext.put( Constants.BRANCH_ARG, "master" );

        DataFetchingEnvironment environment = mock( DataFetchingEnvironment.class );
        when( environment.getLocalContext() ).thenReturn( localContext );

        GetContentProjectDataFetcher fetcher = new GetContentProjectDataFetcher();
        assertEquals( "project", fetcher.get( environment ) );
    }
}
