package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.Cursors;
import com.enonic.app.guillotine.mcp.shape.Deadline;
import com.enonic.app.guillotine.mcp.shape.Page;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.schema.content.ContentTypeService;

/**
 * Content under a path or of given types that references content which is missing or which the caller cannot read. Each item in scope
 * is read, by path, through XP's outbound dependencies (data, page and component configs, mixins, rich text and the original of a
 * variant), and the targets are looked up as the caller. One call checks at most {@link #SOURCES_PER_CALL} items and
 * {@link #MAX_TARGETS} distinct targets and stops at the {@link Deadline}, except that the first item is always checked so that every
 * call moves on; {@code nextCursor} continues with the next item. URL strings are not references and are not checked.
 */
public final class FindBrokenReferencesTool
    implements McpTool
{
    static final int SOURCES_PER_CALL = 100;

    static final int MAX_TARGETS = 5000;

    static final int TARGET_BATCH = 500;

    static final int MAX_LISTED = 20;

    static final String ITEMS = "items";

    static final String MISSING = "missing";

    static final String SCOPE_NOTE = "missing lists referenced ids that are missing or that the caller cannot read. Checked: " +
        "references to content in data, page and component configs, mixins and rich text. Not checked: external URLs and URL strings " +
        "in component configs. To see where a reference sits, run query with get(key: <_id>) on the item and select dataAsJson, " +
        "pageAsJson or components.";

    private static final String NARROW_HINT = "Narrow with path or types.";

    private final ContentService contentService;

    private final ContentTypeService contentTypeService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    private final int maxTargets;

    private final LongSupplier nanoTime;

    FindBrokenReferencesTool( final ContentService contentService, final ContentTypeService contentTypeService,
                              final GuillotineConfigService configService, final ProjectBranchResolver resolver, final int maxTargets,
                              final LongSupplier nanoTime )
    {
        this.contentService = contentService;
        this.contentTypeService = contentTypeService;
        this.configService = configService;
        this.resolver = resolver;
        this.maxTargets = maxTargets;
        this.nanoTime = nanoTime;
    }

    public FindBrokenReferencesTool( final ContentService contentService, final ContentTypeService contentTypeService,
                                     final GuillotineConfigService configService, final ProjectBranchResolver resolver )
    {
        this( contentService, contentTypeService, configService, resolver, MAX_TARGETS, System::nanoTime );
    }

    @Override
    public McpGroup group()
    {
        return McpGroup.REFERENCES;
    }

    @Override
    public String name()
    {
        return "find-broken-references";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final Deadline deadline = Deadline.start( nanoTime );
        final ProjectBranch target = resolver.resolve( args );
        final String path = ContentKeys.scopePath( args.get( "path" ) instanceof String value ? value : null );
        final List<?> types = args.get( "types" ) instanceof List<?> list ? list : List.of();
        final Page page = Cursors.page( Collections.singletonMap( Cursors.CURSOR_ARG, args.get( Cursors.CURSOR_ARG ) ), SOURCES_PER_CALL );

        final ReferenceQueries queries = new ReferenceQueries( contentService, target, Integer.MAX_VALUE, deadline );
        final ReferenceQueries.Ids sources =
            queries.ids( ReferenceQueries.scope( types, null, path, contentTypeService ), page.offset(), page.count() );
        page.requireWithin( sources.total() );

        final Map<String, List<String>> outbound = new LinkedHashMap<>();
        final Set<String> targets = new LinkedHashSet<>();
        int scanned = 0;
        int firstTargets = 0;
        for ( final String id : sources.ids() )
        {
            if ( scanned > 0 && !queries.canCall( 1 ) )
            {
                break;
            }
            final List<String> references = queries.outbound( id );
            if ( scanned > 0 && references != null &&
                targets.size() + references.stream().filter( ref -> !targets.contains( ref ) ).distinct().count() > maxTargets )
            {
                break;
            }
            scanned++;
            if ( references != null && !references.isEmpty() )
            {
                outbound.put( id, references );
                targets.addAll( references );
            }
            if ( scanned == 1 )
            {
                firstTargets = targets.size();
            }
        }

        final Set<String> readable = new LinkedHashSet<>();
        final List<String> all = List.copyOf( targets );
        int looked = 0;
        // One batch always runs, and every batch the first item needs, so that the reads made before the deadline give a result.
        while ( looked < all.size() && ( looked == 0 || looked < firstTargets || queries.canCall( 1 ) ) )
        {
            final List<String> batch = all.subList( looked, Math.min( all.size(), looked + TARGET_BATCH ) );
            queries.readable( batch ).keySet().forEach( id -> readable.add( ReferenceQueries.normalize( id ) ) );
            looked += batch.size();
        }
        if ( looked < all.size() )
        {
            final Set<String> lookedUp = new LinkedHashSet<>( all.subList( 0, looked ) );
            scanned = checkedPrefix( sources.ids().subList( 0, scanned ), outbound, lookedUp );
            outbound.keySet().retainAll( sources.ids().subList( 0, scanned ) );
        }

        final Map<String, List<String>> broken = new LinkedHashMap<>();
        outbound.forEach( ( source, references ) -> {
            final List<String> missing =
                references.stream().filter( id -> !readable.contains( ReferenceQueries.normalize( id ) ) ).distinct().toList();
            if ( !missing.isEmpty() )
            {
                broken.put( source, missing );
            }
        } );

        final Map<String, Content> labelled = broken.isEmpty() ? Map.of() : queries.readable( broken.keySet() );
        final List<Map<String, Object>> items = new ArrayList<>();
        final List<Integer> positions = new ArrayList<>();
        broken.forEach( ( source, missing ) -> {
            final Content content = labelled.get( source );
            if ( content == null )
            {
                return;
            }
            final Map<String, Object> item = ReferenceQueries.item( content );
            item.put( MISSING, missing.subList( 0, Math.min( MAX_LISTED, missing.size() ) ) );
            if ( missing.size() > MAX_LISTED )
            {
                item.put( "missingCount", missing.size() );
            }
            items.add( item );
            positions.add( sources.ids().indexOf( source ) );
        } );

        final int checked = scanned;
        final String nextCursor = page.nextCursor( sources.total(), checked );
        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( ResultBudget.TOTAL, sources.total() );
        structured.put( "checked", checked );
        structured.put( ITEMS, items );
        final boolean incomplete = page.offset() + checked < sources.total();
        if ( incomplete )
        {
            structured.put( "incomplete", true );
        }
        structured.put( ResultBudget.NOTE,
                        note( sources.total(), page.offset(), checked, incomplete, nextCursor != null, queries.timedOut() ) );
        return ResultBudget.fromConfig( configService )
            .withNarrowHint( NARROW_HINT )
            .fitPage( structured, ITEMS, returned -> returned >= positions.size()
                ? nextCursor
                : page.nextCursor( sources.total(), positions.get( returned ) ) );
    }

    /**
     * The number of leading {@code scanned} items whose targets were all looked up. Targets are looked up in the order the items
     * were read, so these items come first.
     */
    private static int checkedPrefix( final List<String> scanned, final Map<String, List<String>> outbound, final Set<String> lookedUp )
    {
        int checked = 0;
        for ( final String id : scanned )
        {
            if ( !lookedUp.containsAll( outbound.getOrDefault( id, List.of() ) ) )
            {
                break;
            }
            checked++;
        }
        return checked;
    }

    private static String note( final long total, final int offset, final int checked, final boolean incomplete,
                                final boolean more, final boolean timedOut )
    {
        if ( total == 0 )
        {
            return "No content in scope that the caller can read. " + SCOPE_NOTE;
        }
        final String range = checked == 0 ? "none" : ( offset + 1 ) + "-" + ( offset + checked );
        final String stopped = timedOut ? " (stopped at the per-call time limit of " + Deadline.LIMIT_SECONDS + " seconds)" : "";
        final String rest = !incomplete ? ". "
            : more ? stopped + "; continue with nextCursor for the rest. "
            : "; the rest is beyond the " + Cursors.MAX_WINDOW + " items a listing can page through: narrow with path or types. ";
        return "Checked items " + range + " of " + total + " by path" + rest + SCOPE_NOTE;
    }
}
