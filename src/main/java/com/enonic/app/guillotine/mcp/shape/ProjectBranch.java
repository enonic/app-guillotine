package com.enonic.app.guillotine.mcp.shape;

import java.util.Map;

import com.enonic.xp.branch.Branch;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.repository.RepositoryId;

public record ProjectBranch(String project, String branch)
{
    public RepositoryId repositoryId()
    {
        return ProjectName.from( project ).getRepoId();
    }

    public Branch xpBranch()
    {
        return Branch.from( branch );
    }

    public Map<String, Object> putInto( final Map<String, Object> structured )
    {
        structured.put( "project", project );
        structured.put( "branch", branch );
        return structured;
    }
}
