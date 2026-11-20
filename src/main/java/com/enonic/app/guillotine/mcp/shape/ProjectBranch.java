package com.enonic.app.guillotine.mcp.shape;

import java.util.Map;
import java.util.concurrent.Callable;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.exception.ForbiddenAccessException;
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
     * Runs {@code callable} in the project's repository and branch, as the current caller and with the caller's permissions only. XP
     * refuses some reads with {@link ForbiddenAccessException}, for example any read of {@code draft} by a caller without a role in
     * the project; that becomes {@link #accessDenied()}.
     */
    public <T> T callWith( final Callable<T> callable )
    {
        try
        {
            return ContextBuilder.copyOf( ContextAccessor.current() )
                .repositoryId( repositoryId() )
                .branch( xpBranch() )
                .build()
                .callWith( callable );
        }
        catch ( ForbiddenAccessException e )
        {
            throw accessDenied();
        }
    }

    /**
     * The error for a read XP refused to the current caller in this project and branch, with the branch to use instead.
     */
    public ToolError accessDenied()
    {
        final String message = "Access denied: the caller cannot read branch " + branch + " of project " + project + ".";
        if ( !ProjectBranchResolver.isAuthenticated() )
        {
            return new ToolError( message, "Anonymous callers read only published content: omit branch or pass branch master." );
        }
        if ( ContentConstants.BRANCH_DRAFT.toString().equals( branch ) )
        {
            return new ToolError( message, "Pass branch master to read published content, or ask an administrator for a role in " +
                "project " + project + "." );
        }
        return new ToolError( message, "Ask an administrator for read access to project " + project + "." );
    }

    /**
     * The project and branch of the current context, or {@code null} when its repository is not a project's.
     */
    public static ProjectBranch current()
    {
        final Context context = ContextAccessor.current();
        final ProjectName project = context.getRepositoryId() != null ? ProjectName.from( context.getRepositoryId() ) : null;
        return project != null && context.getBranch() != null ? new ProjectBranch( project.toString(), context.getBranch().toString() )
            : null;
    }
}
