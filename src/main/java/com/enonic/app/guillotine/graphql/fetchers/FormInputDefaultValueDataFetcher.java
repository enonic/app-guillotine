package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class FormInputDefaultValueDataFetcher
    implements DataFetcher<Object>
{
    public static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();
        return MAPPER.writeValueAsString( sourceAsMap.get( "value" ) );
    }
}
