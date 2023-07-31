package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.SecurityHelper;

public class GetContentFieldDataFetcher
    implements DataFetcher<Object>
{

    private final String fieldName;

    public GetContentFieldDataFetcher( final String fieldName )
    {
        this.fieldName = Objects.requireNonNull( fieldName );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        if ( !SecurityHelper.canAccessCmsData() )
        {
            throw new IllegalAccessException( "Unauthorized" );
        }
        Map<String, Object> sourceAsMap = environment.getSource();
        return sourceAsMap.get( fieldName );
    }
}
