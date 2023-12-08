package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.context.ContextAccessor;

public class GetContentBranchDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> ContextAccessor.current().getBranch().toString() );
    }
}
