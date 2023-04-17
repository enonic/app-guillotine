package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class GetContentDataDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();

        if ( sourceAsMap.get( "attachments" ) != null && !( (Map<?, ?>) sourceAsMap.get( "attachments" ) ).isEmpty() )
        {
            Map<String, Object> result = new HashMap<>();
            result.put( "__contentWithAttachments", sourceAsMap );
            result.put( "__data", sourceAsMap.get( "data" ) );
            return result;
        }
        else
        {
            return sourceAsMap.get( "data" );
        }
    }
}
