package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;

public class GetAttachmentsDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Map<String, Object> contentAsMap = environment.getSource();
        if ( contentAsMap == null )
        {
            return null;
        }
        final Map<String, Object> attachmentsAsMap = CastHelper.cast( contentAsMap.get( "attachments" ) );
        return attachmentsAsMap != null ? attachmentsAsMap.values() : null;
    }
}
