package com.enonic.app.guillotine.mcp.shape;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeNotFoundException;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.Project;
import com.enonic.xp.project.ProjectConstants;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.repository.BranchNotFoundException;
import com.enonic.xp.repository.RepositoryNotFoundException;

/**
 * Resolves the project and branch a tool reads from: the {@code project} argument, else {@code mcp.defaultProject}, else the caller's
 * only listed project; the {@code branch} argument, else {@code draft} for a signed-in caller and {@code master} for an anonymous one.
 * A project counts as readable when the caller can read its content root, because {@link ProjectService#list()} omits projects that
 * are only publicly readable.
 */
public class ProjectBranchResolver
{
    private static final String DRAFT = ContentConstants.BRANCH_DRAFT.toString();

    private static final String MASTER = ContentConstants.BRANCH_MASTER.toString();

    private final ProjectService projectService;

    private final NodeService nodeService;

    private final GuillotineConfigService configService;

    public ProjectBranchResolver( final ProjectService projectService, final NodeService nodeService,
                                  final GuillotineConfigService configService )
    {
        this.projectService = projectService;
        this.nodeService = nodeService;
        this.configService = configService;
    }

    public ProjectBranch resolve( final Map<String, ?> args )
    {
        final String branch = args.get( ProjectBranch.BRANCH ) instanceof String value && !value.isEmpty() ? value : defaultBranch();
        return new ProjectBranch( resolveProject( args.get( ProjectBranch.PROJECT ), branch ), branch );
    }

    public static String defaultBranch()
    {
        return isAuthenticated() ? DRAFT : MASTER;
    }

    public static boolean isAuthenticated()
    {
        return ContextAccessor.current().getAuthInfo().isAuthenticated();
    }

    /**
     * The project as read from its content root in the caller's context, or {@code null} when it does not exist or the caller cannot
     * read it.
     */
    public Project readableProject( final String name, final String branch )
    {
        final ProjectName projectName;
        try
        {
            projectName = ProjectName.from( name );
        }
        catch ( IllegalArgumentException | NullPointerException e )
        {
            return null;
        }

        final Node contentRoot;
        try
        {
            contentRoot = new ProjectBranch( projectName.toString(), branch ).callWith(
                () -> nodeService.getByPath( ContentConstants.CONTENT_ROOT_PATH ) );
        }
        catch ( RepositoryNotFoundException | BranchNotFoundException | NodeNotFoundException e )
        {
            return null;
        }
        return contentRoot == null ? null : toProject( projectName, contentRoot );
    }

    private String resolveProject( final Object project, final String branch )
    {
        if ( project instanceof String name && !name.isEmpty() )
        {
            return requireReadable( name, branch );
        }

        final String configured = configService.getMcpDefaultProject();
        if ( configured != null && !configured.isEmpty() )
        {
            if ( readableProject( configured, branch ) == null )
            {
                throw new ToolError( "The default project '" + configured + "' (mcp.defaultProject) is not readable on " + branch + ".",
                                     "Pass the project argument; call list-projects to see available projects." );
            }
            return configured;
        }

        final List<Project> listed = projectService.list().stream().toList();
        if ( listed.size() == 1 )
        {
            return requireReadable( listed.get( 0 ).getName().toString(), branch );
        }
        throw new ToolError( "Project is required: pass the project argument.",
                             "Call list-projects to see project names, or ask the administrator to set mcp.defaultProject " +
                                 "in com.enonic.app.guillotine.cfg." );
    }

    private String requireReadable( final String name, final String branch )
    {
        if ( readableProject( name, branch ) == null )
        {
            throw new ToolError( "Project '" + McpJson.shorten( name, McpJson.PREVIEW_CHARS ) + "' not found or not readable on " +
                                     branch + ".", "Call list-projects to see available projects." );
        }
        return name;
    }

    private static Project toProject( final ProjectName name, final Node contentRoot )
    {
        final PropertyTree data = contentRoot.data();
        final PropertySet projectData = data.getSet( ContentPropertyNames.DATA );
        final String language = data.getString( ContentPropertyNames.LANGUAGE );
        final Project.Builder project = Project.create()
            .name( name )
            .displayName( data.getString( ContentPropertyNames.DISPLAY_NAME ) )
            .description( projectData != null ? projectData.getString( ProjectConstants.PROJECT_DESCRIPTION_PROPERTY ) : null )
            .language( language != null ? Locale.forLanguageTag( language ) : null );
        if ( projectData != null )
        {
            projectData.getStrings( ProjectConstants.PROJECT_PARENTS_PROPERTY ).forEach( parent -> project.addParent( ProjectName.from( parent ) ) );
        }
        return project.build();
    }
}
