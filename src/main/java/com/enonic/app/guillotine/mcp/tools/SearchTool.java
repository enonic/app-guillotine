package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.Cursors;
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

/**
 * Finds, filters, sorts, counts and aggregates content in one project and branch. Hits carry identity, type, modification time, a
 * workflow state other than {@code READY}, and the data fields asked for; reference values and aggregation buckets keyed by content
 * ids carry the display name and path of their target, so the model does not have to resolve ids.
 */
public final class SearchTool
    implements McpTool
{
    static final int DEFAULT_COUNT = 10;

    static final String HITS = "hits";

    static final String AGGREGATIONS = "aggregations";

    static final String QUERY_TEMPLATE = "query McpSearch($query: QueryDSLInput!, $first: Int, $after: String, $sort: [SortDslInput], " +
        "$aggregations: [AggregationInput]) { guillotine { queryDslConnection(query: $query, first: $first, after: $after, " +
        "sort: $sort, aggregations: $aggregations) { totalCount aggregationsAsJson edges { node { %s } } } } }";

    static final String HIT_FIELDS = "_id _path displayName type modifiedTime";

    static final String QUERY = QUERY_TEMPLATE.formatted( HIT_FIELDS );

    static final String QUERY_WITH_DATA = QUERY_TEMPLATE.formatted( HIT_FIELDS + " dataAsJson" );

    private static final String NARROW_HINT = "Narrow with text, types, path or filters; ask for fewer fields, a smaller count, or fewer " +
        "or smaller aggregations (count 0 returns aggregations alone).";

    private final SchemaProvider schemaProvider;

    private final ContentTypeService contentTypeService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    private final ContentLookup lookup;

    public SearchTool( final SchemaProvider schemaProvider, final ContentService contentService,
                       final ContentTypeService contentTypeService, final GuillotineConfigService configService,
                       final ProjectBranchResolver resolver )
    {
        this.schemaProvider = schemaProvider;
        this.contentTypeService = contentTypeService;
        this.configService = configService;
        this.resolver = resolver;
        this.lookup = new ContentLookup( schemaProvider, contentService );
    }

    @Override
    public McpGroup group()
    {
        return McpGroup.CONTENT;
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
        final Page page = Cursors.page( args, DEFAULT_COUNT );

        final ContentTypeIndex index =
            new ContentTypeIndex( schemaProvider.getSchema(), ContentTypeIndex.registeredNames( contentTypeService ) );
        final List<?> requestedTypes = args.get( "types" ) instanceof List<?> list ? list : List.of();
        final Map<String, Object> typeClause = SearchQuery.types( requestedTypes, index::spelling );
        final DataFieldScope scope = DataFieldScope.of( index, registered(), typeClause == null ? null : scopeTypes( requestedTypes, index ) );
        final SearchQuery search =
            SearchQuery.from( args, new SearchQuery.Inputs( caller(), typeClause, scope, key -> lookup.idOf( target, key ) ) );

        final Map<String, Object> variables = new LinkedHashMap<>();
        variables.put( "query", search.query );
        variables.put( "first", page.count() );
        if ( page.offset() > 0 )
        {
            variables.put( "after", ConnectionHelper.encodeCursor( String.valueOf( page.offset() - 1 ) ) );
        }
        variables.put( "sort", search.sort );
        if ( search.aggregations != null )
        {
            variables.put( "aggregations", search.aggregations );
        }
        final ContentLookup.Response response =
            lookup.execute( target, search.fields.isEmpty() ? QUERY : QUERY_WITH_DATA, variables );
        final Map<?, ?> connection = response.object( "queryDslConnection" );
        final long total = connection != null && connection.get( "totalCount" ) instanceof Number number ? number.longValue() : 0;
        page.requireWithin( total );

        final List<Map<?, ?>> nodes = nodes( connection );
        final Map<String, String> states =
            lookup.workflowStates( target, nodes.stream().map( node -> String.valueOf( node.get( ContentLookup.ID ) ) ).toList() );
        final DataValues values = new DataValues( scope );
        final List<Map<String, Object>> hits = new ArrayList<>();
        for ( final Map<?, ?> node : nodes )
        {
            final Map<String, Object> hit = new LinkedHashMap<>();
            for ( final String field : List.of( ContentLookup.ID, ContentLookup.PATH, ContentLookup.DISPLAY_NAME, "type", "modifiedTime" ) )
            {
                hit.put( field, node.get( field ) );
            }
            final String state = states.get( String.valueOf( node.get( ContentLookup.ID ) ) );
            if ( state != null && !"READY".equals( state ) )
            {
                hit.put( "workflow", state );
            }
            if ( !search.fields.isEmpty() )
            {
                hit.put( "data", values.project( node.get( "dataAsJson" ), search.fields, String.valueOf( node.get( "type" ) ) ) );
            }
            hits.add( hit );
        }

        final List<Map<String, Object>> aggregations =
            aggregations( search.aggregates, connection != null ? connection.get( "aggregationsAsJson" ) : null, total );
        final Set<String> ids = new LinkedHashSet<>( values.referencedIds() );
        aggregations.forEach( aggregation -> buckets( aggregation ).forEach( bucket -> {
            if ( isContentId( aggregation, bucket, scope ) )
            {
                ids.add( (String) bucket.get( "key" ) );
            }
        } ) );
        final Map<String, Map<String, Object>> labels = lookup.labels( target, ids );
        final Set<String> looked = new LinkedHashSet<>( ids.stream().limit( ContentLookup.MAX_LABELS ).toList() );
        values.label( hits, labels, looked );
        aggregations.forEach( aggregation -> label( aggregation, labels, looked, scope ) );

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( ResultBudget.TOTAL, total );
        structured.put( HITS, hits );
        if ( !aggregations.isEmpty() )
        {
            structured.put( AGGREGATIONS, aggregations );
        }
        if ( !response.errors().isEmpty() )
        {
            structured.put( McpResult.ERRORS, response.errors() );
        }
        final List<String> notes = new ArrayList<>();
        if ( ids.size() > ContentLookup.MAX_LABELS )
        {
            notes.add( "Display names were looked up for the first " + ContentLookup.MAX_LABELS + " content ids only." );
        }
        if ( total == 0 )
        {
            notes.addAll( emptyNotes( target, args, requestedTypes, index ) );
        }
        if ( !notes.isEmpty() )
        {
            structured.put( ResultBudget.NOTE, String.join( " ", notes ) );
        }
        return ResultBudget.fromConfig( configService ).withNarrowHint( NARROW_HINT ).fitPage( structured, HITS, page, total );
    }

    private List<ContentType> registered()
    {
        final ContentTypes all = contentTypeService.getAll();
        return all == null ? List.of() : all.stream().toList();
    }

    /**
     * The registered content types the {@code types} argument names, for checking data field names.
     */
    private static List<String> scopeTypes( final List<?> requested, final ContentTypeIndex index )
    {
        final List<String> result = new ArrayList<>();
        for ( final Object item : requested )
        {
            final String type = String.valueOf( item ).strip();
            if ( type.contains( ":" ) )
            {
                result.add( index.spelling( type ) );
            }
            else
            {
                index.contentTypes()
                    .stream()
                    .filter( name -> name.substring( name.indexOf( ':' ) + 1 ).equalsIgnoreCase( type ) )
                    .forEach( result::add );
            }
        }
        return result;
    }

    private static PrincipalKey caller()
    {
        final User user = ProjectBranchResolver.isAuthenticated() ? ContextAccessor.current().getAuthInfo().getUser() : null;
        return user != null ? user.getKey() : null;
    }

    private static List<Map<String, Object>> aggregations( final List<SearchQuery.Aggregate> aggregates, final Object json,
                                                           final long total )
    {
        final Map<?, ?> byName = json instanceof Map<?, ?> map ? map : Map.of();
        final List<Map<String, Object>> result = new ArrayList<>();
        for ( final SearchQuery.Aggregate aggregate : aggregates )
        {
            final List<Map<String, Object>> buckets = new ArrayList<>();
            if ( byName.get( aggregate.name() ) instanceof Map<?, ?> aggregation && aggregation.get( "buckets" ) instanceof List<?> list )
            {
                for ( final Object item : list )
                {
                    if ( item instanceof Map<?, ?> bucket && bucket.get( "key" ) != null )
                    {
                        final Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put( "key", bucket.get( "key" ) );
                        entry.put( "count", bucket.get( "docCount" ) instanceof Number count ? count.longValue() : 0L );
                        buckets.add( entry );
                    }
                }
            }
            buckets.sort( Comparator.comparingLong( ( Map<String, Object> bucket ) -> (Long) bucket.get( "count" ) )
                              .reversed()
                              .thenComparing( bucket -> String.valueOf( bucket.get( "key" ) ) ) );
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put( "by", aggregate.by() );
            entry.put( "buckets", buckets );
            if ( buckets.isEmpty() && total > 0 )
            {
                entry.put( ResultBudget.NOTE,
                           "No buckets: the field is not set on the matching content, or cannot be aggregated (full-text fields cannot)." );
            }
            result.add( entry );
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> buckets( final Map<String, Object> aggregation )
    {
        return (List<Map<String, Object>>) aggregation.get( "buckets" );
    }

    /**
     * Whether the bucket key is a content id: any key of a reference field or {@code _references}, else a key shaped like a
     * generated id.
     */
    private static boolean isContentId( final Map<String, Object> aggregation, final Map<String, Object> bucket,
                                        final DataFieldScope scope )
    {
        final String by = (String) aggregation.get( "by" );
        return bucket.get( "key" ) instanceof String key &&
            ( SearchQuery.REFERENCES_FIELD.equals( by ) || scope.isReference( by ) || ContentKeys.isUuid( key ) );
    }

    private static void label( final Map<String, Object> aggregation, final Map<String, Map<String, Object>> labels,
                               final Set<String> looked, final DataFieldScope scope )
    {
        for ( final Map<String, Object> bucket : buckets( aggregation ) )
        {
            if ( isContentId( aggregation, bucket, scope ) && looked.contains( (String) bucket.get( "key" ) ) )
            {
                final String key = (String) bucket.get( "key" );
                final Map<String, Object> label = labels.get( key );
                if ( label != null )
                {
                    bucket.putAll( label );
                }
                else
                {
                    bucket.put( ContentLookup.MISSING, true );
                }
            }
        }
    }

    private static List<Map<?, ?>> nodes( final Map<?, ?> connection )
    {
        final List<Map<?, ?>> result = new ArrayList<>();
        if ( connection != null && connection.get( "edges" ) instanceof List<?> edges )
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
     * Explains an empty result whose cause the arguments show: a type no installed application defines, a path with no content, or
     * work in progress looked for on master.
     */
    private List<String> emptyNotes( final ProjectBranch target, final Map<String, Object> args, final List<?> requestedTypes,
                                     final ContentTypeIndex index )
    {
        final List<String> notes = new ArrayList<>();
        final List<String> unknown = requestedTypes.stream()
            .map( type -> String.valueOf( type ).strip() )
            .filter( type -> type.contains( ":" ) ? !index.contentTypes().contains( index.spelling( type ) )
                : scopeTypes( List.of( type ), index ).isEmpty() )
            .toList();
        if ( !unknown.isEmpty() )
        {
            notes.add( "No installed content type is named " + String.join( ", ", unknown ) + "; call list-types for the type names." );
        }
        final String path = args.get( "path" ) instanceof String value ? ContentKeys.scopePath( value ) : null;
        if ( path != null && lookup.idOf( target, path ) == null )
        {
            notes.add( "No content at path " + path + " on " + target.branch() + " that the caller can read." );
        }
        if ( "IN_PROGRESS".equals( args.get( "workflow" ) ) && ContentConstants.BRANCH_MASTER.toString().equals( target.branch() ) )
        {
            notes.add( "master holds published content, which is not in progress; search draft as a signed-in user." );
        }
        return notes;
    }
}
