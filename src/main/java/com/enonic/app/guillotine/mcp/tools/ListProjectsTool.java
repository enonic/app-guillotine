package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.McpArgs;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.project.Project;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.security.User;

public final class ListProjectsTool
    implements McpTool
{
    private final ProjectService projectService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    public ListProjectsTool( final ProjectService projectService, final GuillotineConfigService configService,
                             final ProjectBranchResolver resolver )
    {
        this.projectService = projectService;
        this.configService = configService;
        this.resolver = resolver;
    }

    @Override
    public String name()
    {
        return "list-projects";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final List<Project> projects = new ArrayList<>( projectService.list().stream().toList() );
        addReadableDefault( projects );
        projects.sort( Comparator.comparing( project -> project.getName().toString() ) );

        final String defaultProject = resolveDefaultProject( projects );

        final List<Map<String, Object>> items = projects.stream().map( project -> toItem( project, defaultProject ) ).toList();

        final Map<String, Object> structured = new LinkedHashMap<>();
        structured.put( "projects", items );
        structured.put( "defaultProject", defaultProject );
        structured.put( "defaultBranch", ProjectBranchResolver.resolveBranch( null ) );
        structured.put( "caller", caller() );

        final ResultBudget budget = ResultBudget.listOnly( configService.getMcpMaxResultChars(), "projects",
                                                           "Pass a project name directly to the tools that need it.", "projects",
                                                           "defaultBranch", "caller" );
        return McpResult.success( result -> summary( projects.size(), result.get( "projects" ), defaultProject ), structured, budget, null,
                                  null, 0 );
    }

    private static Map<String, Object> caller()
    {
        final Map<String, Object> caller = new LinkedHashMap<>();
        final User user = ContextAccessor.current().getAuthInfo().getUser();
        if ( !McpArgs.isAuthenticated() || user == null )
        {
            caller.put( "anonymous", true );
            return caller;
        }
        caller.put( "anonymous", false );
        caller.put( "key", user.getKey().toString() );
        caller.put( "displayName", user.getDisplayName() );
        return caller;
    }

    private void addReadableDefault( final List<Project> projects )
    {
        final String configured = configService.getMcpDefaultProject();
        if ( configured == null || configured.isEmpty() ||
            projects.stream().anyMatch( project -> project.getName().toString().equals( configured ) ) )
        {
            return;
        }
        final Project readable = resolver.readableProject( configured, ProjectBranchResolver.resolveBranch( null ) );
        if ( readable != null )
        {
            projects.add( readable );
        }
    }

    private String resolveDefaultProject( final List<Project> projects )
    {
        final String configured = configService.getMcpDefaultProject();
        if ( configured != null && !configured.isEmpty() )
        {
            return projects.stream().anyMatch( project -> project.getName().toString().equals( configured ) ) ? configured : null;
        }
        return projects.size() == 1 ? projects.get( 0 ).getName().toString() : null;
    }

    private static Map<String, Object> toItem( final Project project, final String defaultProject )
    {
        final String name = project.getName().toString();

        final Map<String, Object> item = new LinkedHashMap<>();
        item.put( "name", name );
        item.put( "displayName", project.getDisplayName() != null ? project.getDisplayName() : name );
        item.put( "description", project.getDescription() );
        item.put( "language", project.getLanguage() != null ? project.getLanguage().toLanguageTag() : null );
        item.put( "isDefault", name.equals( defaultProject ) );
        return item;
    }

    private static String summary( final int count, final Object returned, final String defaultProject )
    {
        if ( count == 0 )
        {
            return "No projects are visible to the current user.";
        }
        final int listed = returned instanceof List<?> list ? list.size() : 0;
        final String projects = ( count == 1 ? "1 project" : count + " projects" ) + " visible" +
            ( listed < count ? " (" + listed + " listed here)" : "" );
        return defaultProject != null
            ? projects + "; default project: " + defaultProject + "."
            : projects + "; no default project.";
    }
}
