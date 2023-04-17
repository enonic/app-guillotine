package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.SecurityHelper;

public class GetContentOwnerDataFetcher
    implements DataFetcher<String>
{
    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        if ( !SecurityHelper.canAccessCmsData() )
        {
            throw new IllegalAccessException( "Unauthorized" );
        }
        Map<String, Object> sourceAsMap = environment.getSource();
        return sourceAsMap.get( "owner" ).toString();
    }
}
