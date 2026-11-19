package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.graphql.helper.SecurityHelper;
import com.enonic.app.guillotine.mcp.shape.GraphQlErrors;
import com.enonic.app.guillotine.mcp.shape.McpArgs;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.WorkflowInfo;
import com.enonic.xp.content.WorkflowState;

/**
 * Runs constant Guillotine GraphQL documents in-process for the content tools. User input only ever reaches GraphQL as variables.
 */
public class ContentGraphQl
{
    public static final int MAX_LABELS = 100;

    static final String LABELS_QUERY = "query McpLabels($project: String, $branch: String, $query: QueryDSLInput, $first: Int) {" +
        " guillotine(project: $project, branch: $branch) { queryDsl(query: $query, first: $first) { _id _path displayName } } }";

    private final SchemaProvider schemaProvider;

    private final ContentService contentService;

    public ContentGraphQl( final SchemaProvider schemaProvider, final ContentService contentService )
    {
        this.schemaProvider = schemaProvider;
        this.contentService = contentService;
    }

    /**
     * Executes {@code document} in the caller's context with the repository and branch of {@code target}; {@code project} and
     * {@code branch} variables are always set from {@code target}.
     */
    public Map<String, Object> execute( final ProjectBranch target, final String document, final Map<String, Object> variables )
    {
        final Map<String, Object> all = new LinkedHashMap<>( variables );
        all.put( ProjectBranchResolver.PROJECT_ARG, target.project() );
        all.put( ProjectBranchResolver.BRANCH_ARG, target.branch() );
        return ProjectBranchResolver.runInProjectContext( target, () -> schemaProvider.executeToSpecification(
            document, all, null, GraphQlErrors.hideInternalErrors() ) );
    }

    /**
     * Path and display name of each readable content among {@code ids} (at most {@link #MAX_LABELS}); unknown or unreadable ids are
     * absent from the result.
     */
    public Map<String, Map<String, Object>> labels( final ProjectBranch target, final Collection<String> ids )
    {
        final List<String> unique = new ArrayList<>( new LinkedHashSet<>( ids ) ).stream().limit( MAX_LABELS ).toList();
        final Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if ( unique.isEmpty() )
        {
            return result;
        }

        final Map<String, Object> variables = new LinkedHashMap<>();
        variables.put( "query", Map.of( "in", Map.of( "field", "_id", "stringValues", unique ) ) );
        variables.put( "first", unique.size() );

        final Object hits = guillotine( execute( target, LABELS_QUERY, variables ) ).get( "queryDsl" );
        if ( hits instanceof List<?> list )
        {
            for ( final Object hit : list )
            {
                if ( hit instanceof Map<?, ?> node && node.get( "_id" ) != null )
                {
                    final Map<String, Object> label = new LinkedHashMap<>();
                    label.put( "path", node.get( "_path" ) );
                    label.put( "displayName", node.get( "displayName" ) );
                    result.put( String.valueOf( node.get( "_id" ) ), label );
                }
            }
        }
        return result;
    }

    /**
     * Workflow state of each readable content among {@code ids}. Content created without workflow info counts as {@code READY}.
     */
    public Map<String, String> workflowStates( final ProjectBranch target, final Collection<String> ids )
    {
        final Map<String, String> result = new LinkedHashMap<>();
        if ( ids.isEmpty() )
        {
            return result;
        }
        final Iterable<Content> contents = ProjectBranchResolver.runInProjectContext( target, () -> contentService.getByIds(
            new GetContentByIdsParams( ContentIds.from( ids.stream().distinct().map( ContentId::from ).toList() ) ) ) );
        if ( contents == null )
        {
            return result;
        }
        for ( final Content content : contents )
        {
            final WorkflowInfo workflow = content.getWorkflowInfo();
            final WorkflowState state = workflow != null && workflow.getState() != null ? workflow.getState() : WorkflowState.READY;
            result.put( content.getId().toString(), state.toString() );
        }
        return result;
    }

    /**
     * Guillotine only lets admins and CMS users read creator, modifier and owner; asking for them otherwise fails each hit.
     */
    public static boolean canReadPrincipals()
    {
        return SecurityHelper.canAccessCmsData();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> guillotine( final Map<String, Object> response )
    {
        if ( response.get( "data" ) instanceof Map<?, ?> data && data.get( "guillotine" ) instanceof Map<?, ?> guillotine )
        {
            return (Map<String, Object>) guillotine;
        }
        return Map.of();
    }

    /**
     * The object returned for {@code name} under {@code guillotine}, or {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> field( final Map<String, Object> response, final String name )
    {
        return guillotine( response ).get( name ) instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    /**
     * The error result for a response without data.
     */
    public static Map<String, Object> failure( final Map<String, Object> response, final ProjectBranch target )
    {
        checkAccess( response, target );
        final List<Map<String, Object>> errors = GraphQlErrors.sanitize( errors( response ) );
        return McpResult.error( errors.isEmpty()
                                    ? "The query returned no data. Retry the call, or ask the administrator to check the log."
                                    : GraphQlErrors.failure( errors ) );
    }

    public static List<?> errors( final Map<String, Object> response )
    {
        return response.get( "errors" ) instanceof List<?> errors ? errors : List.of();
    }

    /**
     * Turns an access-denied failure into an error that tells the caller which branch to use instead.
     */
    public static void checkAccess( final Map<String, Object> response, final ProjectBranch target )
    {
        final boolean denied = errors( response ).stream()
            .anyMatch( error -> error instanceof Map<?, ?> map && String.valueOf( map.get( "message" ) ).contains( "Access denied" ) );
        if ( denied )
        {
            throw accessDenied( target );
        }
    }

    public static ToolError accessDenied( final ProjectBranch target )
    {
        final String message = "Access denied: the current user cannot read branch '" + target.branch() + "' of project '" +
            target.project() + "'.";
        if ( !McpArgs.isAuthenticated() )
        {
            return new ToolError( message, "Anonymous callers can read only published content: omit branch or pass \"master\"." );
        }
        return new ToolError( message, "Pass branch \"master\" for published content, or ask for a role in the project." );
    }
}
