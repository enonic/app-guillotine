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
    public static <T> T executeInContext( final DataFetchingEnvironment environment, final Callable<T> callable )
    {
        final ContextBuilder contextBuilder = ContextBuilder.from( ContextAccessor.current() );

        if ( environment.getLocalContext() instanceof Map )
        {
            final Map<String, Object> localContext = environment.getLocalContext();

            if ( localContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ) != null )
            {
                contextBuilder.branch( localContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ).toString() );
            }
            if ( localContext.get( Constants.GUILLOTINE_TARGET_PROJECT_CTX ) != null )
            {
                contextBuilder.repositoryId(
                    ProjectConstants.PROJECT_REPO_ID_PREFIX + localContext.get( Constants.GUILLOTINE_TARGET_PROJECT_CTX ).toString() );
            }
        }

        return contextBuilder.build().callWith( callable );
    }

    public static String getSiteKey( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        return Objects.toString( localContext.get( Constants.GUILLOTINE_TARGET_SITE_CTX ), null );
    }

    public static RepositoryId getRepositoryId( final DataFetchingEnvironment environment, final RepositoryId defaultRepoId )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        if ( localContext.get( Constants.GUILLOTINE_TARGET_PROJECT_CTX ) != null )
        {
            return RepositoryId.from(
                ProjectConstants.PROJECT_REPO_ID_PREFIX + localContext.get( Constants.GUILLOTINE_TARGET_PROJECT_CTX ).toString() );
        }
        return defaultRepoId;
    }

    public static Branch getBranch( final DataFetchingEnvironment environment, final Branch defaultBranch )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        if ( localContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ) != null )
        {
            return Branch.from( localContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ).toString() );
        }
        return defaultBranch;
    }
}
