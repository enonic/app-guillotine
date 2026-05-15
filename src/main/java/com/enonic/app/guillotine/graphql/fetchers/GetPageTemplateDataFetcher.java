package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;

public class GetPageTemplateDataFetcher
    extends BasePageDataFetcher
{
    public GetPageTemplateDataFetcher( final ServiceFacade serviceFacade )
    {
        super( serviceFacade );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return doGet( environment );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        Map<String, Object> sourceAsMap = environment.getSource();
        return resolvePageTemplate( sourceAsMap, environment );
    }
}
