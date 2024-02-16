package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.DataFetcherHelper;

public class ContentDataFieldDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = CastHelper.cast( environment.getSource() );
        Object data = CastHelper.cast( sourceAsMap.get( "data" ) );
        if ( Objects.equals( sourceAsMap.get( "type" ), "portal:site" ) || Objects.equals( sourceAsMap.get( "_path" ), "/content" ) )
        {
            data = DataFetcherHelper.removeField( data, "siteConfig" );
        }
        return DataFetcherHelper.removeContentIdField( data );
    }
}
