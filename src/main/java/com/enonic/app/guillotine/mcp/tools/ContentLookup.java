package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.shape.GraphQlErrors;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.WorkflowInfo;
import com.enonic.xp.content.WorkflowState;

/**
 * Reads for the content tools, each in the target project and branch with the caller's permissions: constant GraphQL documents with
 * user input as variables, display labels for content ids, and workflow states, which the GraphQL schema does not expose.
 */
final class ContentLookup
{
    static final int MAX_LABELS = 100;

    static final String DISPLAY_NAME = "displayName";

    static final String PATH = "_path";

    static final String ID = "_id";

    /**
     * A target that is missing or that the caller cannot read; the two are one state.
     */
    static final String MISSING = "missing";

    static final String LABELS_QUERY = "query McpLabels($query: QueryDSLInput, $first: Int) { guillotine { " +
        "queryDsl(query: $query, first: $first) { _id _path displayName } } }";

    static final String ID_QUERY = "query McpId($key: ID) { guillotine { get(key: $key) { _id } } }";

    private final SchemaProvider schemaProvider;

    private final ContentService contentService;

    ContentLookup( final SchemaProvider schemaProvider, final ContentService contentService )
    {
        this.schemaProvider = schemaProvider;
        this.contentService = contentService;
    }

    /**
     * The {@code guillotine} object of {@code document}'s data and the reduced errors next to it. A response with errors and no data
     * is a {@link com.enonic.app.guillotine.mcp.shape.ToolError}.
     */
    Response execute( final ProjectBranch target, final String document, final Map<String, Object> variables )
    {
        final Map<String, Object> response = target.callWith(
            () -> schemaProvider.executeToSpecification( document, variables, null, GraphQlErrors.hideInternalErrors() ) );
        final Object data = response.get( QueryTool.DATA );
        final List<Map<String, Object>> errors =
            GraphQlErrors.partialErrors( data, response.get( McpResult.ERRORS ) instanceof List<?> list ? list : List.of() );
        final Map<?, ?> guillotine =
            data instanceof Map<?, ?> root && root.get( "guillotine" ) instanceof Map<?, ?> map ? map : Map.of();
        return new Response( guillotine, errors );
    }

    record Response(Map<?, ?> guillotine, List<Map<String, Object>> errors)
    {
        Map<?, ?> object( final String field )
        {
            return guillotine.get( field ) instanceof Map<?, ?> map ? map : null;
        }
    }

    /**
     * {@code {displayName, _path}} of each readable content among the first {@link #MAX_LABELS} distinct {@code ids}; ids that are
     * missing or not readable are absent.
     */
    Map<String, Map<String, Object>> labels( final ProjectBranch target, final Collection<String> ids )
    {
        final List<String> unique = new ArrayList<>( new LinkedHashSet<>( ids ) ).stream().limit( MAX_LABELS ).toList();
        final Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if ( unique.isEmpty() )
        {
            return result;
        }
        final Map<String, Object> variables = new LinkedHashMap<>();
        variables.put( "query", Map.of( "in", Map.of( "field", ID, "stringValues", unique ) ) );
        variables.put( "first", unique.size() );
        if ( execute( target, LABELS_QUERY, variables ).guillotine().get( "queryDsl" ) instanceof List<?> hits )
        {
            for ( final Object hit : hits )
            {
                if ( hit instanceof Map<?, ?> node && node.get( ID ) != null )
                {
                    final Map<String, Object> label = new LinkedHashMap<>();
                    label.put( DISPLAY_NAME, node.get( DISPLAY_NAME ) );
                    label.put( PATH, node.get( PATH ) );
                    result.put( String.valueOf( node.get( ID ) ), label );
                }
            }
        }
        return result;
    }

    /**
     * The id of the content at {@code key}, an id or a path, or {@code null} when it is missing or not readable.
     */
    String idOf( final ProjectBranch target, final String key )
    {
        final Map<String, Object> variables = new LinkedHashMap<>();
        variables.put( "key", key );
        final Map<?, ?> content = execute( target, ID_QUERY, variables ).object( "get" );
        return content != null && content.get( ID ) != null ? String.valueOf( content.get( ID ) ) : null;
    }

    /**
     * The workflow state of each readable content among {@code ids}; content without workflow info is {@code READY}.
     */
    Map<String, String> workflowStates( final ProjectBranch target, final Collection<String> ids )
    {
        final Map<String, String> result = new LinkedHashMap<>();
        if ( ids.isEmpty() )
        {
            return result;
        }
        final Iterable<Content> contents = target.callWith( () -> contentService.getByIds(
            GetContentByIdsParams.create().contentIds( ContentIds.from( ids.stream().distinct().map( ContentId::from ).toList() ) ).build() ) );
        if ( contents != null )
        {
            for ( final Content content : contents )
            {
                final WorkflowInfo workflow = content.getWorkflowInfo();
                final WorkflowState state = workflow != null && workflow.getState() != null ? workflow.getState() : WorkflowState.READY;
                result.put( content.getId().toString(), state.toString() );
            }
        }
        return result;
    }
}
