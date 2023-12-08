package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.CastHelper;

public class GetAttachmentsDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> contentAsMap = environment.getSource();
        Map<String, Object> attachmentsAsMap = CastHelper.cast( contentAsMap.get( "attachments" ) );

        if ( attachmentsAsMap != null )
        {
            Map<String, Object> parentLocalContext = environment.getLocalContext();
            Map<String, Object> localContext = new HashMap<>( parentLocalContext );
            localContext.put( Constants.CONTENT_ID_FIELD, contentAsMap.get( "_id" ).toString() );

            final DataFetcherResult.Builder<Object> builder = DataFetcherResult.newResult();

            builder.data( attachmentsAsMap.values() );
            builder.localContext( Collections.unmodifiableMap( localContext ) );

            return builder.build();
        }
        return null;
    }
}
