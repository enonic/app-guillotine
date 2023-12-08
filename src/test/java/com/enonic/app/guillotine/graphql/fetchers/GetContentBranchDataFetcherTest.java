package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetContentBranchDataFetcherTest
{
    @Test
    public void test()
        throws Exception
    {
        Map<String, Object> localContext = new HashMap<>();
        localContext.put( Constants.GUILLOTINE_TARGET_PROJECT_CTX, "project" );
        localContext.put( Constants.GUILLOTINE_TARGET_BRANCH_CTX, "master" );

        DataFetchingEnvironment environment = mock( DataFetchingEnvironment.class );
        when( environment.getLocalContext() ).thenReturn( localContext );

        GetContentBranchDataFetcher fetcher = new GetContentBranchDataFetcher();
        assertEquals( "master", fetcher.get( environment ) );
    }
}
