package com.enonic.app.guillotine.mcp.shape;

import java.util.Map;
import java.util.concurrent.Callable;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.repository.RepositoryId;

public record ProjectBranch(String project, String branch)
{
    public static final String PROJECT = "project";

    public static final String BRANCH = "branch";

    public RepositoryId repositoryId()
    {
        return ProjectName.from( project ).getRepoId();
    }

    public Branch xpBranch()
    {
        return Branch.from( branch );
    }

    /**
     * Puts {@code project} and {@code branch} into a structured result, which every tool result that reads a project carries.
     */
    public Map<String, Object> putInto( final Map<String, Object> structured )
    {
        structured.put( PROJECT, project );
        structured.put( BRANCH, branch );
        return structured;
    }

    /**
     * Runs {@code callable} in the project's repository and branch, as the current caller and with the caller's permissions only.
     */
    public <T> T callWith( final Callable<T> callable )
    {
        return ContextBuilder.copyOf( ContextAccessor.current() )
            .repositoryId( repositoryId() )
            .branch( xpBranch() )
            .build()
            .callWith( callable );
    }
}
