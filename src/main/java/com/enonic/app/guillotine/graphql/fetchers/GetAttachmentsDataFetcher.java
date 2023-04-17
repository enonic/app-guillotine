package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.CastHelper;

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
            return attachmentsAsMap.values();
        }
        return null;
    }
}
