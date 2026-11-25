package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.Deadline;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.schema.content.ContentTypeService;

/**
 * Content of the given types, media by default, that no content the caller can read references. Candidates are read by path, up to
 * {@link #MAX_CANDIDATES}, and checked in chunks against the {@code _references} index field, which holds every reference XP stores:
 * data, page and component configs, mixins and rich text. A reference of an item to itself does not count. One call makes at most
 * {@link #MAX_CALLS} backend queries and stops starting new ones at the {@link Deadline}; candidates left unchecked make the result
 * {@code incomplete}.
 */
public final class FindUnreferencedTool
    implements McpTool
{
    static final int DEFAULT_COUNT = 20;

    static final int MAX_COUNT = 50;

    static final int MAX_CANDIDATES = 10000;

    static final int CHUNK = 500;

    static final int MAX_BUCKETS = 10000;

    static final int MAX_CALLS = 50;

    static final String MEDIA = "media:*";

    static final String ITEMS = "items";

    private static final String NARROW_HINT = "Narrow with path or types, or pass a smaller count.";

    private final ContentService contentService;

    private final ContentTypeService contentTypeService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    private final int maxBuckets;

    private final LongSupplier nanoTime;

    FindUnreferencedTool( final ContentService contentService, final ContentTypeService contentTypeService,
                          final GuillotineConfigService configService, final ProjectBranchResolver resolver, final int maxBuckets,
                          final LongSupplier nanoTime )
    {
        this.contentService = contentService;
        this.contentTypeService = contentTypeService;
        this.configService = configService;
        this.resolver = resolver;
        this.maxBuckets = maxBuckets;
        this.nanoTime = nanoTime;
    }

    public FindUnreferencedTool( final ContentService contentService, final ContentTypeService contentTypeService,
                                 final GuillotineConfigService configService, final ProjectBranchResolver resolver )
    {
        this( contentService, contentTypeService, configService, resolver, MAX_BUCKETS, System::nanoTime );
    }

    @Override
    public McpGroup group()
    {
        return McpGroup.REFERENCES;
    }

    @Override
    public String name()
    {
        return "find-unreferenced";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final Deadline deadline = Deadline.start( nanoTime );
        final ProjectBranch target = resolver.resolve( args );
        final int count = args.get( "count" ) instanceof Number number ? Math.clamp( number.intValue(), 0, MAX_COUNT ) : DEFAULT_COUNT;
        final String path = ContentKeys.scopePath( args.get( "path" ) instanceof String value ? value : null );
        final List<?> types = args.get( "types" ) instanceof List<?> list ? list : List.of();

        final ReferenceQueries queries = new ReferenceQueries( contentService, target, MAX_CALLS, deadline );
        final ReferenceQueries.Ids candidates =
            queries.ids( ReferenceQueries.scope( types, MEDIA, path, contentTypeService ), 0, MAX_CANDIDATES );

        final Scan scan = new Scan( queries, count > 0 ? 1 : 0 );
        final List<String> ids = candidates.ids();
        for ( int from = 0; from < ids.size(); from += CHUNK )
        {
            scan.chunk( ids.subList( from, Math.min( ids.size(), from + CHUNK ) ) );
        }
        final List<String> unreferenced = ids.stream().filter( scan.unreferenced::contains ).toList();
        final int checked = ids.size() - scan.undecided.size();

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( ResultBudget.TOTAL, unreferenced.size() );
        structured.put( "checked", checked );
        structured.put( "candidates", candidates.total() );
        structured.put( ITEMS, count > 0 && !unreferenced.isEmpty() ? queries.items( unreferenced.subList( 0, Math.min( count,
                                                                                                                   unreferenced.size() ) ) )
            : List.of() );
        if ( checked < candidates.total() )
        {
            structured.put( "incomplete", true );
        }
        structured.put( ResultBudget.NOTE, note( unreferenced.size(), count, checked, candidates.total(), queries.timedOut() ) );
        return ResultBudget.fromConfig( configService ).withNarrowHint( NARROW_HINT ).fitList( structured, ITEMS );
    }

    private static String note( final int total, final int count, final int checked, final long candidates, final boolean timedOut )
    {
        final List<String> notes = new ArrayList<>();
        notes.add( "Covers only content the caller can read: a reference held by content the caller cannot read is not seen, so a " +
                       "listed item may still be in use." );
        if ( checked < candidates )
        {
            notes.add( "Incomplete: checked the first " + checked + " of " + candidates + " items by path within the per-call " +
                           ( timedOut ? "time limit of " + Deadline.LIMIT_SECONDS + " seconds" : "limit" ) +
                           "; narrow with path or types to check the rest." );
        }
        if ( count > 0 && total > count )
        {
            notes.add( "Listed the first " + count + " of " + total + " by path; narrow with path or types to list " +
                           "others." );
        }
        return String.join( " ", notes );
    }

    /**
     * Sorts candidate ids into unreferenced and undecided ones, chunk by chunk. A chunk takes one aggregation over the
     * {@code _references} of the content that references any of its ids. Ids found there are referenced, unless the item itself is
     * among those referencing the chunk: then, and when the buckets may be cut off, one count per id decides, excluding the item itself.
     */
    private final class Scan
    {
        final ReferenceQueries queries;

        final int reserved;

        final Set<String> unreferenced = new LinkedHashSet<>();

        final Set<String> undecided = new LinkedHashSet<>();

        Scan( final ReferenceQueries queries, final int reserved )
        {
            this.queries = queries;
            this.reserved = reserved;
        }

        private boolean canCall()
        {
            return queries.canCall( 1 + reserved );
        }

        void chunk( final List<String> chunk )
        {
            if ( !canCall() )
            {
                undecided.addAll( chunk );
                return;
            }
            final Map<String, String> byKey = new LinkedHashMap<>();
            chunk.forEach( id -> byKey.put( ReferenceQueries.normalize( id ), id ) );
            final ReferenceQueries.Referenced referenced =
                queries.referenced( ReferenceQueries.in( ReferenceQueries.REFERENCES, chunk ), maxBuckets );
            if ( referenced.sources() == 0 )
            {
                unreferenced.addAll( chunk );
                return;
            }

            final List<String> found = byKey.keySet().stream().filter( referenced.keys()::contains ).map( byKey::get ).toList();
            final Set<String> exact = new LinkedHashSet<>();
            final Set<String> decided = new LinkedHashSet<>( found );
            if ( !found.isEmpty() )
            {
                if ( !canCall() )
                {
                    undecided.addAll( chunk );
                    return;
                }
                final List<String> selfReferencing = queries.ids(
                    ReferenceQueries.and( List.of( ReferenceQueries.in( ReferenceQueries.ID, found ),
                                                   ReferenceQueries.in( ReferenceQueries.REFERENCES, chunk ) ), List.of() ), 0,
                    found.size() ).ids();
                exact.addAll( selfReferencing.stream().map( ReferenceQueries::normalize ).filter( byKey::containsKey ).map( byKey::get )
                                  .toList() );
            }
            if ( referenced.cut() )
            {
                chunk.stream().filter( id -> !decided.contains( id ) ).forEach( exact::add );
            }
            decided.removeAll( exact );

            for ( final String id : chunk )
            {
                if ( decided.contains( id ) )
                {
                    continue;
                }
                if ( !exact.contains( id ) )
                {
                    unreferenced.add( id );
                }
                else if ( !canCall() )
                {
                    undecided.add( id );
                }
                else if ( queries.count( ReferenceQueries.and( List.of( ReferenceQueries.in( ReferenceQueries.REFERENCES, List.of( id ) ) ),
                                                               List.of( ReferenceQueries.in( ReferenceQueries.ID, List.of( id ) ) ) ) ) ==
                    0 )
                {
                    unreferenced.add( id );
                }
            }
        }
    }
}
