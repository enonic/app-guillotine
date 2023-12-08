package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.project.ProjectName;

public class GetContentProjectDataFetcher
    implements DataFetcher<Object>
{
    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> Objects.toString(
            ProjectName.from( ContextAccessor.current().getRepositoryId() ), null ) );
    }
}
