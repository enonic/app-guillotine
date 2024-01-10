package com.enonic.app.guillotine.graphql.helper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.project.ProjectConstants;
import com.enonic.xp.repository.RepositoryId;

public class GuillotineLocalContextHelper
{
    public static <T> T executeInContext( final DataFetchingEnvironment environment, Callable<T> callable )
    {
        final Map<String, Object> localContext = environment.getLocalContext();

        final ContextBuilder contextBuilder = ContextBuilder.from( ContextAccessor.current() );

        if ( localContext.get( Constants.BRANCH_ARG ) != null )
        {
            contextBuilder.branch( localContext.get( Constants.BRANCH_ARG ).toString() );
        }
        if ( localContext.get( Constants.PROJECT_ARG ) != null )
        {
            contextBuilder.repositoryId( ProjectConstants.PROJECT_REPO_ID_PREFIX + localContext.get( Constants.PROJECT_ARG ).toString() );
        }

        return contextBuilder.build().callWith( callable );
    }

    public static String getSiteKey( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        return Objects.toString( localContext.get( Constants.SITE_ARG ), null );
    }

    public static RepositoryId getRepositoryId( final DataFetchingEnvironment environment, final RepositoryId defaultRepoId )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        if ( localContext.get( Constants.PROJECT_ARG ) != null )
        {
            return RepositoryId.from( ProjectConstants.PROJECT_REPO_ID_PREFIX + localContext.get( Constants.PROJECT_ARG ).toString() );
        }
        return defaultRepoId;
    }

    public static Branch getBranch( final DataFetchingEnvironment environment, final Branch defaultBranch )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        if ( localContext.get( Constants.BRANCH_ARG ) != null )
        {
            return Branch.from( localContext.get( Constants.BRANCH_ARG ).toString() );
        }
        return defaultBranch;
    }
}
