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
        final Map<String, Object> targetContext = getTargetContext( environment );

        final ContextBuilder contextBuilder = ContextBuilder.from( ContextAccessor.current() );

        if ( targetContext != null )
        {
            if ( targetContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ) != null )
            {
                contextBuilder.branch( targetContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ).toString() );
            }
            if ( targetContext.get( Constants.GUILLOTINE_TARGET_REPO_CTX ) != null )
            {
                contextBuilder.repositoryId(
                    ProjectConstants.PROJECT_REPO_ID_PREFIX + targetContext.get( Constants.GUILLOTINE_TARGET_REPO_CTX ).toString() );
            }
        }

        return contextBuilder.build().callWith( callable );
    }

    public static String getSiteKey( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> targetContext = getTargetContext( environment );

        if ( targetContext != null && !Objects.toString( targetContext.get( Constants.GUILLOTINE_TARGET_SITE_CTX ), "" ).isEmpty() )
        {
            return targetContext.get( Constants.GUILLOTINE_TARGET_SITE_CTX ).toString();
        }

        return environment.getGraphQlContext().getOrDefault( "__siteKey", "" );
    }

    public static String getRepositoryId( final DataFetchingEnvironment environment, final RepositoryId defaultRepoId )
    {
        final Map<String, Object> targetContext = getTargetContext( environment );
        if ( targetContext != null && targetContext.get( Constants.GUILLOTINE_TARGET_REPO_CTX ) != null )
        {
            return ProjectConstants.PROJECT_REPO_ID_PREFIX + targetContext.get( Constants.GUILLOTINE_TARGET_REPO_CTX ).toString();
        }
        return defaultRepoId.toString();
    }

    public static String getBranch( final DataFetchingEnvironment environment, final Branch defaultBranch )
    {
        final Map<String, Object> targetContext = getTargetContext( environment );
        if ( targetContext != null && targetContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ) != null )
        {
            return targetContext.get( Constants.GUILLOTINE_TARGET_BRANCH_CTX ).toString();
        }
        return defaultBranch.toString();
    }

    private static Map<String, Object> getTargetContext( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        return CastHelper.cast( localContext.get( Constants.GUILLOTINE_LOCAL_CTX ) );
    }
}
