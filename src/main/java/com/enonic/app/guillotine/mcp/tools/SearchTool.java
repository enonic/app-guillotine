package com.enonic.app.guillotine.mcp.tools;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.ContentKeys;
import com.enonic.app.guillotine.mcp.shape.Cursors;
import com.enonic.app.guillotine.mcp.shape.GraphQlErrors;
import com.enonic.app.guillotine.mcp.shape.McpArgs;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.Page;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.User;

public final class SearchTool
    implements McpTool
{
    static final String QUERY_TEMPLATE = "query McpSearch($project: String, $branch: String, $query: QueryDSLInput!, $first: Int, " +
        "$after: String, $sort: [SortDslInput], $aggregations: [AggregationInput]) {" +
        " guillotine(project: $project, branch: $branch) {" +
        " queryDslConnection(query: $query, first: $first, after: $after, sort: $sort, aggregations: $aggregations) {" +
        " totalCount aggregationsAsJson edges { node { %s } } } } }";

    static final String QUERY = QUERY_TEMPLATE.formatted( ContentSummary.fields( false ) );

    static final String QUERY_WITH_PRINCIPALS = QUERY_TEMPLATE.formatted( ContentSummary.fields( true ) );

    private static final String AGGREGATION_HINT =
        "Narrow the request with types, path, filters or a smaller count; pass count 0 to get aggregation buckets alone.";

    private static final String HITS_HINT = "Narrow with text, types, path or filters, or use a smaller count.";

    private static final String BUCKETS_HINT = "Narrow with types, path or filters, or use fewer aggregate entries or a smaller size.";

    private final ContentGraphQl graphQl;

    private final ProjectBranchResolver resolver;

    private final GuillotineConfigService configService;

    private final ContentLinks links;

    private final ContentTypeService contentTypeService;

    public SearchTool( final ContentGraphQl graphQl, final ProjectBranchResolver resolver, final GuillotineConfigService configService,
                       final ContentService contentService, final ContentTypeService contentTypeService )
    {
        this.graphQl = graphQl;
        this.resolver = resolver;
        this.configService = configService;
        this.links = new ContentLinks( contentService );
        this.contentTypeService = contentTypeService;
    }

    @Override
    public String name()
    {
        return "search";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final ProjectBranch target = resolver.resolve( args );

        final Page page = Cursors.page( args, target, "only total and aggregations" );

        final boolean withPrincipals = ContentGraphQl.canReadPrincipals();
        final SearchQuery search = SearchQuery.from( args, currentUserKey(), withPrincipals );

        final Map<String, Object> variables = new LinkedHashMap<>();
        variables.put( "query", search.query );
        variables.put( "first", page.count() );
        variables.put( "after", page.offset() > 0 ? connectionCursor( page.offset() - 1 ) : null );
        variables.put( "sort", search.sort );
        variables.put( "aggregations", search.aggregations );

        final Map<String, Object> response = graphQl.execute( target, withPrincipals ? QUERY_WITH_PRINCIPALS : QUERY, variables );

        final Map<String, Object> connection = ContentGraphQl.field( response, "queryDslConnection" );
        if ( connection == null )
        {
            return ContentGraphQl.failure( response, target );
        }

        final long total = connection.get( "totalCount" ) instanceof Number number ? number.longValue() : 0;
        page.requireWithin( total );

        final List<Map<?, ?>> nodes = nodes( connection );
        final Map<String, String> workflow =
            graphQl.workflowStates( target, nodes.stream().map( node -> String.valueOf( node.get( "_id" ) ) ).toList() );
        final List<Map<String, Object>> hits =
            nodes.stream().map( node -> ContentSummary.of( node, workflow.get( String.valueOf( node.get( "_id" ) ) ) ) ).toList();

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( "total", total );
        if ( page.offset() > 0 )
        {
            structured.put( "offset", page.offset() );
        }
        if ( page.count() > 0 )
        {
            structured.put( "hits", hits );
            structured.put( ResultBudget.NEXT_CURSOR, page.nextCursor( total, hits.size() ) );
        }
        if ( !search.aggregates.isEmpty() )
        {
            structured.put( "aggregations", aggregations( target, search.aggregates, connection.get( "aggregationsAsJson" ), total ) );
        }
        final List<Map<String, Object>> errors = GraphQlErrors.sanitize( ContentGraphQl.errors( response ) );
        if ( !errors.isEmpty() )
        {
            structured.put( McpResult.ERRORS, errors );
        }
        if ( total == 0 )
        {
            structured.put( ResultBudget.NOTE, emptyNote( target, args ) );
        }

        // buckets are capped only next to hits; with count 0 an aggregation may use the whole result budget
        final ResultBudget budget = search.aggregates.isEmpty()
            ? ResultBudget.fromConfig( configService ).withNarrowHint( HITS_HINT )
            : page.count() == 0
                ? ResultBudget.fromConfig( configService ).withNarrowHint( BUCKETS_HINT )
                : ResultBudget.fromConfig( configService, "buckets" ).withNarrowHint( AGGREGATION_HINT );
        return McpResult.success( result -> summary( target, total, page, result ), structured, budget, "hits", page, total );
    }

    /**
     * Explains an empty result whose cause is visible from the arguments: a type name no installed content type has, a missing path,
     * a path with nothing below it when nothing else restricts the search, or in-progress content on master.
     */
    private String emptyNote( final ProjectBranch target, final Map<String, Object> args )
    {
        final List<String> notes = new ArrayList<>();
        if ( args.get( "types" ) instanceof List<?> types && !types.isEmpty() )
        {
            final List<String> unknown = unknownTypes( types );
            if ( !unknown.isEmpty() )
            {
                notes.add( "No installed content type named " + String.join( ", ", unknown ) + "; call list-types for the type names." );
            }
        }
        final String path = ContentKeys.scopePath( (String) args.get( "path" ) );
        if ( path != null )
        {
            if ( links.get( target, path ) == null )
            {
                notes.add( "No content at path " + path + " on " + target.branch() + "; call search without path to find paths." );
            }
            else if ( onlyPathRestricts( args ) )
            {
                notes.add( "path matches only content below " + path + ", not the item itself; read it with get." );
            }
        }
        if ( "IN_PROGRESS".equals( args.get( "workflow" ) ) && ContentConstants.BRANCH_MASTER.toString().equals( target.branch() ) )
        {
            notes.add( "master holds published content, which is normally not in progress; search draft as a signed-in user." );
        }
        return notes.isEmpty() ? null : String.join( " ", notes );
    }

    private static boolean onlyPathRestricts( final Map<String, Object> args )
    {
        return !( args.get( "text" ) instanceof String text && !text.isBlank() ) &&
            !( args.get( "types" ) instanceof List<?> types && !types.isEmpty() ) && !Boolean.TRUE.equals( args.get( "mine" ) ) &&
            !( args.get( "workflow" ) instanceof String workflow && !workflow.isEmpty() ) &&
            !( args.get( "filters" ) instanceof List<?> filters && !filters.isEmpty() );
    }

    private List<String> unknownTypes( final List<?> requested )
    {
        final ContentTypes all = contentTypeService.getAll();
        if ( all == null )
        {
            return List.of();
        }
        final List<String> names = all.stream().map( ContentType::getName ).map( Object::toString ).toList();
        return requested.stream()
            .map( String::valueOf )
            .filter( type -> names.stream()
                .noneMatch( name -> ( type.contains( ":" ) ? name : name.substring( name.indexOf( ':' ) + 1 ) ).equalsIgnoreCase( type ) ) )
            .toList();
    }

    private List<Map<String, Object>> aggregations( final ProjectBranch target, final List<SearchQuery.Aggregate> aggregates,
                                                    final Object json, final long total )
    {
        final Map<?, ?> byName = json instanceof Map<?, ?> map ? map : Map.of();
        final List<Map<String, Object>> result = new ArrayList<>();
        final List<String> ids = new ArrayList<>();

        for ( final SearchQuery.Aggregate aggregate : aggregates )
        {
            final List<Map<String, Object>> buckets = new ArrayList<>();
            if ( byName.get( aggregate.name() ) instanceof Map<?, ?> aggregation && aggregation.get( "buckets" ) instanceof List<?> list )
            {
                for ( final Object item : list )
                {
                    if ( item instanceof Map<?, ?> bucket )
                    {
                        final Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put( "key", bucket.get( "key" ) );
                        entry.put( "count", bucket.get( "docCount" ) );
                        buckets.add( entry );
                        if ( aggregate.interval() == null && bucket.get( "key" ) instanceof String key &&
                            McpArgs.isUuid( key ) )
                        {
                            ids.add( key );
                        }
                    }
                }
            }
            if ( aggregate.interval() == null )
            {
                buckets.sort( Comparator.comparingLong( ( Map<String, Object> bucket ) -> count( bucket ) ).reversed()
                                  .thenComparing( bucket -> String.valueOf( bucket.get( "key" ) ) ) );
            }

            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put( "by", aggregate.by() );
            entry.put( "interval", aggregate.interval() );
            entry.put( "buckets", buckets );
            if ( buckets.isEmpty() && total > 0 )
            {
                entry.put( "note", aggregate.interval() != null
                    ? "No buckets: interval needs a date field such as modifiedTime or publish.from."
                    : "No buckets: the field is not set on the matching content or cannot be aggregated (full-text and _id fields " +
                        "cannot). Call describe-type to see the fields of a type." );
            }
            result.add( entry );
        }

        final List<String> labelled = ids.stream().distinct().limit( ContentGraphQl.MAX_LABELS ).toList();
        final Map<String, Map<String, Object>> labels = graphQl.labels( target, labelled );
        final boolean capped = ids.stream().distinct().count() > labelled.size();
        result.forEach( aggregation -> {
            boolean skipped = false;
            for ( final Object item : (List<?>) aggregation.get( "buckets" ) )
            {
                final Map<String, Object> bucket = McpArgs.castMap( (Map<?, ?>) item );
                final String key = String.valueOf( bucket.get( "key" ) );
                if ( labels.containsKey( key ) )
                {
                    bucket.put( "label", labels.get( key ) );
                }
                else if ( labelled.contains( key ) )
                {
                    bucket.put( "labelMissing", true );
                }
                else if ( capped && McpArgs.isUuid( key ) )
                {
                    skipped = true;
                }
            }
            if ( skipped )
            {
                aggregation.put( "note", "Labels resolved for the first " + ContentGraphQl.MAX_LABELS + " content-id buckets only." );
            }
        } );
        return result;
    }

    private static long count( final Map<String, Object> bucket )
    {
        return bucket.get( "count" ) instanceof Number number ? number.longValue() : 0;
    }

    private static List<Map<?, ?>> nodes( final Map<String, Object> connection )
    {
        final List<Map<?, ?>> result = new ArrayList<>();
        if ( connection.get( "edges" ) instanceof List<?> edges )
        {
            for ( final Object edge : edges )
            {
                if ( edge instanceof Map<?, ?> map && map.get( "node" ) instanceof Map<?, ?> node )
                {
                    result.add( node );
                }
            }
        }
        return result;
    }

    /**
     * Guillotine's connection cursor: the base64 offset of the last item already seen.
     */
    private static String connectionCursor( final int lastSeen )
    {
        return Base64.getEncoder().encodeToString( String.valueOf( lastSeen ).getBytes( StandardCharsets.UTF_8 ) );
    }

    private static PrincipalKey currentUserKey()
    {
        final User user = ContextAccessor.current().getAuthInfo().getUser();
        return user != null ? user.getKey() : null;
    }

    /**
     * Describes the result as returned, after the budget may have dropped hits or other parts.
     */
    static String summary( final ProjectBranch target, final long total, final Page page, final Map<String, Object> result )
    {
        final String where = " in " + target.project() + " (" + target.branch() + ")";
        final boolean truncated = Boolean.TRUE.equals( result.get( ResultBudget.TRUNCATED ) );
        final String truncation = truncated ? " Result truncated; see hint." : "";
        if ( total == 0 )
        {
            return "No content found" + where + "." + truncation;
        }
        final String found = total + ( total == 1 ? " item" : " items" ) + " found" + where;
        final int returned = result.get( "hits" ) instanceof List<?> hits ? hits.size() : 0;
        if ( returned == 0 )
        {
            return found + "." + truncation;
        }
        final String shown = "; showing " + ( page.offset() + 1 ) + "-" + ( page.offset() + returned ) + " of " + total;
        final String more = result.get( ResultBudget.NEXT_CURSOR ) != null ? ", continue with nextCursor" : "";
        return found + shown + more + "." + truncation;
    }
}
