package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.project.Project;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.security.User;

/**
 * Who the caller is, the projects they can read with their parent layer, language and item count on the default branch, and the
 * project and branch other tools use when the arguments name none. The projects are those {@link ProjectService#list()} returns, which
 * needs a project role, plus the configured default project; listing every project readable through content permissions alone would
 * need an elevated context.
 */
public final class ListProjectsTool
    implements McpTool
{
    private static final String PROJECTS = "projects";

    private final ProjectService projectService;

    private final ContentService contentService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    public ListProjectsTool( final ProjectService projectService, final ContentService contentService,
                             final GuillotineConfigService configService, final ProjectBranchResolver resolver )
    {
        this.projectService = projectService;
        this.contentService = contentService;
        this.configService = configService;
        this.resolver = resolver;
    }

    @Override
    public McpGroup group()
    {
        return McpGroup.CONTENT;
    }

    @Override
    public String name()
    {
        return "list-projects";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final String branch = ProjectBranchResolver.defaultBranch();
        final List<Project> candidates = new ArrayList<>( projectService.list().stream().toList() );
        addReadableDefault( candidates, branch );
        candidates.sort( Comparator.comparing( project -> project.getName().toString() ) );

        final List<Project> projects = new ArrayList<>();
        final List<Map<String, Object>> items = new ArrayList<>();
        for ( final Project project : candidates )
        {
            final Long count = itemCount( project.getName().toString(), branch );
            if ( count != null )
            {
                projects.add( project );
                items.add( toItem( project, count ) );
            }
        }

        final Map<String, Object> structured = new LinkedHashMap<>();
        structured.put( "caller", caller() );
        structured.put( "defaultProject", defaultProject( projects ) );
        structured.put( "defaultBranch", branch );
        structured.put( PROJECTS, items );
        final String note = note( projects, structured.get( "defaultProject" ) != null );
        if ( note != null )
        {
            structured.put( ResultBudget.NOTE, note );
        }

        return ResultBudget.fromConfig( configService )
            .withNarrowHint( "Pass a project name directly to the tools that need it." )
            .fitList( structured, PROJECTS );
    }

    /**
     * What to do when the listed projects may not be the one the user means: a project readable only through public content is not
     * listed, so with none or one listed the name of another can only come from the user.
     */
    static String note( final List<Project> projects, final boolean hasDefault )
    {
        if ( projects.isEmpty() )
        {
            return "No projects are listed: only projects the caller has a role in, and the default project, are listed, and there " +
                "is no default project. Ask the user for the project name; a project with published public content can be read by " +
                "passing its name as project.";
        }
        if ( projects.size() == 1 )
        {
            return "Only projects the caller has a role in, and the default project, are listed. When the user means another " +
                "project, pass its name as project, or ask the user for it; a project with published public content is readable that way.";
        }
        return hasDefault ? null : "There is no default project. Unless the user named the project, ask which of these projects to use.";
    }

    private static Map<String, Object> caller()
    {
        final Map<String, Object> caller = new LinkedHashMap<>();
        final User user = ContextAccessor.current().getAuthInfo().getUser();
        if ( !ProjectBranchResolver.isAuthenticated() || user == null )
        {
            caller.put( "anonymous", true );
            return caller;
        }
        caller.put( "anonymous", false );
        caller.put( "key", user.getKey().toString() );
        caller.put( "displayName", displayName( user.getDisplayName() ) );
        return caller;
    }

    /**
     * {@code name} cut so that its JSON, escapes included, takes at most {@link McpJson#PREVIEW_CHARS} characters, which keeps the
     * caller within any result budget.
     */
    static String displayName( final String name )
    {
        String shortened = McpJson.shorten( name, McpJson.PREVIEW_CHARS );
        while ( shortened != null && McpJson.length( shortened ) > McpJson.PREVIEW_CHARS )
        {
            shortened = McpJson.shorten( shortened, shortened.length() - 1 );
        }
        return shortened;
    }

    /**
     * {@link ProjectService#list()} omits a project the caller can read only because its content is public; the configured default
     * project is listed anyway when the caller can read it.
     */
    private void addReadableDefault( final List<Project> projects, final String branch )
    {
        final String configured = configService.getMcpDefaultProject();
        if ( configured == null || configured.isEmpty() || isListed( projects, configured ) )
        {
            return;
        }
        final Project readable = resolver.readableProject( configured, branch );
        if ( readable != null )
        {
            projects.add( readable );
        }
    }

    private String defaultProject( final List<Project> projects )
    {
        final String configured = configService.getMcpDefaultProject();
        if ( configured != null && !configured.isEmpty() )
        {
            return isListed( projects, configured ) ? configured : null;
        }
        return projects.size() == 1 ? projects.get( 0 ).getName().toString() : null;
    }

    private static boolean isListed( final List<Project> projects, final String name )
    {
        return projects.stream().anyMatch( project -> project.getName().toString().equals( name ) );
    }

    private static Map<String, Object> toItem( final Project project, final long itemCount )
    {
        final String name = project.getName().toString();

        final Map<String, Object> item = new LinkedHashMap<>();
        item.put( "name", name );
        item.put( "displayName", project.getDisplayName() != null ? project.getDisplayName() : name );
        item.put( "description", project.getDescription() );
        item.put( "language", project.getLanguage() != null ? project.getLanguage().toLanguageTag() : null );
        item.put( "parent", project.getParent() != null ? project.getParent().toString() : null );
        item.put( "itemCount", itemCount );
        return item;
    }

    /**
     * Content items the caller can read in the project on {@code branch}, or {@code null} when the caller may not query its content
     * there: draft needs a project role even where the content itself is public.
     */
    private Long itemCount( final String project, final String branch )
    {
        try
        {
            return new ProjectBranch( project, branch ).callWith(
                () -> contentService.find( ContentQuery.create().size( 0 ).build() ).getTotalHits() );
        }
        catch ( ToolError e )
        {
            return null;
        }
    }
}
