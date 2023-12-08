package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;

public class CreateDataFetcherResultWithAttachmentsInfo
    implements DataFetcher<Object>
{
    private final String fieldName;

    public CreateDataFetcherResultWithAttachmentsInfo( final String fieldName )
    {
        this.fieldName = Objects.requireNonNull( fieldName );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Map<String, Object> sourceAsMap = environment.getSource();
        final Map<String, Object> attachments = CastHelper.cast( sourceAsMap.get( "attachments" ) );
        final Map<String, Object> localContext =
            GuillotineLocalContextHelper.applyAttachmentsInfo( environment, sourceAsMap.get( "_id" ).toString(), attachments );

        return DataFetcherResult.newResult().data( sourceAsMap.get( fieldName ) ).localContext(
            Collections.unmodifiableMap( localContext ) ).build();
    }
}
