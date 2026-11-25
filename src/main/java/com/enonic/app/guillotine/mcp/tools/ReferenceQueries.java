package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.enonic.app.guillotine.mcp.shape.Deadline;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.xp.aggregation.Aggregation;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.query.aggregation.TermsAggregationQuery;
import com.enonic.xp.query.expr.DslExpr;
import com.enonic.xp.query.expr.DslOrderExpr;
import com.enonic.xp.query.expr.QueryExpr;
import com.enonic.xp.schema.content.ContentTypeService;

/**
 * Backend reads of the {@code references} tools, in one project and branch with the caller's permissions, counted against a fixed
 * number of calls and a {@link Deadline} per tool call. Queries are XP query DSL built from constants and checked arguments; they return content ids only.
 */
final class ReferenceQueries
{
    static final String REFERENCES = "_references";

    static final String TYPE = "type";

    static final String ID = "_id";

    static final String PATH = "_path";

    static final String DISPLAY_NAME = "displayName";

    private static final String BUCKETS = "referenced";

    private final ContentService contentService;

    private final ProjectBranch target;

    private final Deadline deadline;

    private int callsLeft;

    private boolean timedOut;

    ReferenceQueries( final ContentService contentService, final ProjectBranch target, final int maxCalls, final Deadline deadline )
    {
        this.contentService = contentService;
        this.target = target;
        this.callsLeft = maxCalls;
        this.deadline = deadline;
    }

    /**
     * Whether {@code calls} more backend calls fit in this tool call's cap and the deadline has not passed.
     */
    boolean canCall( final int calls )
    {
        if ( callsLeft < calls )
        {
            return false;
        }
        if ( deadline.passed() )
        {
            timedOut = true;
            return false;
        }
        return true;
    }

    /**
     * Whether {@link #canCall} refused a call because the deadline had passed while the cap still had room.
     */
    boolean timedOut()
    {
        return timedOut;
    }

    /**
     * The clause for the scope of a scan: the {@code types} asked for (or {@code fallbackTypes}, a {@code like} pattern, when none) and
     * {@code path} with everything below it. Type names are matched to their registered spelling, ignoring case.
     */
    static Map<String, Object> scope( final List<?> types, final String fallbackType, final String path,
                                      final ContentTypeService contentTypeService )
    {
        final List<Map<String, Object>> must = new ArrayList<>();
        final Map<String, String> spellings = new HashMap<>();
        ContentTypeIndex.registeredNames( contentTypeService )
            .forEach( name -> spellings.putIfAbsent( name.toString().toLowerCase( Locale.ROOT ), name.toString() ) );
        final Map<String, Object> typeClause = types( types != null ? types : List.of(),
                                                      name -> spellings.getOrDefault( name.toLowerCase( Locale.ROOT ), name ) );
        if ( typeClause != null )
        {
            must.add( typeClause );
        }
        else if ( fallbackType != null )
        {
            must.add( Map.of( "like", Map.of( "field", TYPE, "value", fallbackType ) ) );
        }
        if ( path != null )
        {
            must.add( pathMatch( path ) );
        }
        return must.isEmpty() ? Map.of( "matchAll", Map.of() ) : Map.of( "boolean", Map.of( "must", must ) );
    }

    /**
     * The clause for a {@code types} argument: full names as given, matched against registered spellings by {@code spelling}, and
     * local names such as {@code article} in any application. {@code null} for no types.
     */
    private static Map<String, Object> types( final List<?> requested, final Function<String, String> spelling )
    {
        final List<String> fullNames = new ArrayList<>();
        final List<Map<String, Object>> clauses = new ArrayList<>();
        for ( final Object item : requested )
        {
            final String type = String.valueOf( item ).strip();
            if ( type.contains( ":" ) )
            {
                fullNames.add( spelling.apply( type ) );
            }
            else if ( !type.isEmpty() )
            {
                clauses.add( Map.of( "like", Map.of( "field", TYPE, "value", "*:" + type ) ) );
            }
        }
        if ( !fullNames.isEmpty() )
        {
            clauses.add( 0, in( TYPE, fullNames ) );
        }
        if ( clauses.isEmpty() )
        {
            return null;
        }
        return clauses.size() == 1 ? clauses.get( 0 ) : Map.of( "boolean", Map.of( "should", clauses ) );
    }

    /**
     * The clause that matches {@code path}, a scope path from {@link ContentKeys#scopePath}, and everything below it.
     */
    private static Map<String, Object> pathMatch( final String path )
    {
        return Map.of( "pathMatch", Map.of( "field", PATH, "path", "/content" + path, "minimumMatch",
                                            path.substring( 1 ).split( "/" ).length + 1 ) );
    }

    static Map<String, Object> in( final String field, final Collection<String> values )
    {
        return Map.of( "in", Map.of( "field", field, "values", List.copyOf( values ) ) );
    }

    static Map<String, Object> and( final List<Map<String, Object>> must, final List<Map<String, Object>> mustNot )
    {
        final Map<String, Object> bool = new LinkedHashMap<>();
        bool.put( "must", must );
        if ( !mustNot.isEmpty() )
        {
            bool.put( "mustNot", mustNot );
        }
        return Map.of( "boolean", bool );
    }

    /**
     * Ids of the content that {@code query} matches, by path, {@code from} and {@code size}, and the number of matches.
     */
    Ids ids( final Map<String, Object> query, final int from, final int size )
    {
        final FindContentIdsByQueryResult result = find( ContentQuery.create()
                                                             .queryExpr( QueryExpr.from( DslExpr.from( PropertyTree.fromMap( query ) ),
                                                                                         DslOrderExpr.from( PropertyTree.fromMap(
                                                                                             Map.of( "field", "_path", "direction",
                                                                                                     "ASC" ) ) ) ) )
                                                             .from( from )
                                                             .size( size )
                                                             .build() );
        return new Ids( result.getContentIds().stream().map( ContentId::toString ).toList(), result.getTotalHits() );
    }

    /**
     * The number of content items that {@code query} matches.
     */
    long count( final Map<String, Object> query )
    {
        return find( ContentQuery.create().queryExpr( QueryExpr.from( DslExpr.from( PropertyTree.fromMap( query ) ) ) ).size( 0 ).build() )
            .getTotalHits();
    }

    /**
     * The distinct {@code _references} values, lowercased, of the content that {@code query} matches, up to {@code size}; {@code cut}
     * is set when there may be more.
     */
    Referenced referenced( final Map<String, Object> query, final int size )
    {
        final FindContentIdsByQueryResult result = find( ContentQuery.create()
                                                             .queryExpr( QueryExpr.from( DslExpr.from( PropertyTree.fromMap( query ) ) ) )
                                                             .aggregationQuery(
                                                                 TermsAggregationQuery.create( BUCKETS ).fieldName( REFERENCES ).size( size ).build() )
                                                             .size( 0 )
                                                             .build() );
        final Set<String> keys = new LinkedHashSet<>();
        final Aggregation aggregation = result.getAggregations() != null ? result.getAggregations().get( BUCKETS ) : null;
        if ( aggregation instanceof BucketAggregation buckets && buckets.getBuckets() != null )
        {
            for ( final Bucket bucket : buckets.getBuckets() )
            {
                keys.add( normalize( bucket.getKey() ) );
            }
        }
        return new Referenced( result.getTotalHits(), keys, keys.size() >= size );
    }

    /**
     * The ids XP reads as references of {@code id}: in data, page and component configs, mixins and rich text, plus the original of a
     * variant. Missing targets are included. {@code null} when the source itself is missing or not readable.
     */
    List<String> outbound( final String id )
    {
        callsLeft--;
        final ContentIds ids;
        try
        {
            ids = target.callWith( () -> contentService.getOutboundDependencies( ContentId.from( id ) ) );
        }
        catch ( ContentNotFoundException e )
        {
            return null;
        }
        return ids == null ? List.of() : ids.stream().map( ContentId::toString ).toList();
    }

    /**
     * The readable content among {@code ids}, keyed by id; one call.
     */
    Map<String, Content> readable( final Collection<String> ids )
    {
        callsLeft--;
        final Map<String, Content> result = new LinkedHashMap<>();
        if ( ids.isEmpty() )
        {
            return result;
        }
        final Iterable<Content> contents = target.callWith( () -> contentService.getByIds(
            GetContentByIdsParams.create().contentIds( ContentIds.from( ids.stream().map( ContentId::from ).toList() ) ).build() ) );
        if ( contents != null )
        {
            contents.forEach( content -> result.put( content.getId().toString(), content ) );
        }
        return result;
    }

    /**
     * {@code {_id, _path, displayName, type}} of each readable content among {@code ids}, in their order; one call.
     */
    List<Map<String, Object>> items( final List<String> ids )
    {
        final Map<String, Content> readable = readable( ids );
        final List<Map<String, Object>> result = new ArrayList<>();
        for ( final String id : ids )
        {
            final Content content = readable.get( id );
            if ( content != null )
            {
                result.add( item( content ) );
            }
        }
        return result;
    }

    static Map<String, Object> item( final Content content )
    {
        final Map<String, Object> item = new LinkedHashMap<>();
        item.put( ID, content.getId().toString() );
        item.put( PATH, content.getPath().toString() );
        item.put( DISPLAY_NAME, content.getDisplayName() );
        item.put( TYPE, content.getType().toString() );
        return item;
    }

    static String normalize( final String id )
    {
        return id.toLowerCase( Locale.ROOT );
    }

    private FindContentIdsByQueryResult find( final ContentQuery query )
    {
        callsLeft--;
        return target.callWith( () -> contentService.find( query ) );
    }

    record Ids(List<String> ids, long total)
    {
    }

    record Referenced(long sources, Set<String> keys, boolean cut)
    {
    }
}
