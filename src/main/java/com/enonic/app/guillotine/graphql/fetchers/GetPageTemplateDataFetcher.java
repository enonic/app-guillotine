package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;

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
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        Map<String, Object> sourceAsMap = environment.getSource();
        return resolvePageTemplate( sourceAsMap, environment );
    }
}
