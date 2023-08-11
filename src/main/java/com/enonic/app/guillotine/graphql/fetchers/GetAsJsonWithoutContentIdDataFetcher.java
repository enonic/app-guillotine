package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.DataFetcherHelper;

public class GetAsJsonWithoutContentIdDataFetcher
    implements DataFetcher<Object>
{

    private final String fieldName;

    public GetAsJsonWithoutContentIdDataFetcher( final String fieldName )
    {
        this.fieldName = fieldName;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = new HashMap<>( environment.getSource() );
        Object data = CastHelper.cast( sourceAsMap.get( fieldName ) );
        return DataFetcherHelper.removeContentIdField( data );
    }

}
