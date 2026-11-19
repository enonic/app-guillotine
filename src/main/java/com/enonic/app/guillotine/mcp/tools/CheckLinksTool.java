package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.ContentKeys;
import com.enonic.app.guillotine.mcp.shape.Cursors;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.Page;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.query.filter.ExistsFilter;

/**
 * Finds internal links and selector values whose target content is missing. Only content holding references ({@code _references})
 * is scanned, page by page in path order; nothing outside the repository is requested.
 */
public final class CheckLinksTool
    implements McpTool
{
    static final int MAX_BROKEN_PER_SOURCE = 50;

    static final int DEFAULT_COUNT = 50;

    private static final String TRUNCATED_SOURCES_NOTE = "Some sources have more broken links than fit (see truncatedSources); " +
        "call find-references with their sourceId as key and direction outbound to list all of their targets.";

    private final ContentLinks links;

    private final ProjectBranchResolver resolver;

    private final GuillotineConfigService configService;

    CheckLinksTool( final ContentLinks links, final ProjectBranchResolver resolver, final GuillotineConfigService configService )
    {
        this.links = links;
        this.resolver = resolver;
        this.configService = configService;
    }

    public CheckLinksTool( final ContentService contentService, final ProjectBranchResolver resolver,
                           final GuillotineConfigService configService )
    {
        this( new ContentLinks( contentService ), resolver, configService );
    }

    @Override
    public String name()
    {
        return "check-links";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final String path = args.get( "path" ) instanceof String value ? value : null;
        if ( args.get( "path" ) != null && path == null )
        {
            throw new ToolError( "Invalid path: " + McpJson.write( args.get( "path" ) ) + ".",
                                 "Pass a content path that starts with /, such as /landings." );
        }
        final String scope = ContentKeys.scopePath( path );
        final ContentQuery.Builder query = ContentQuery.create()
            .queryExpr( ContentLinks.byPath( ContentLinks.subtree( path ),
                                             ContentLinks.typeConstraint( ContentLinks.types( args.get( "types" ), false ) ) ) )
            .queryFilter( ExistsFilter.create().fieldName( ContentLinks.REFERENCES ).build() );

        final ProjectBranch target = resolver.resolve( args );
        final Page page = Cursors.page( name(), args, target, DEFAULT_COUNT, 1 );
        if ( scope != null && links.get( target, scope ) == null )
        {
            throw new ToolError( "Path " + McpJson.write( scope ) + " not found or not readable in project '" + target.project() +
                                     "' on branch '" + target.branch() + "'.",
                                 "Call search to find paths; master holds only published content." );
        }

        final FindContentIdsByQueryResult result = links.find( target, query.from( page.offset() ).size( page.count() ).build() );
        final long total = result.getTotalHits();
        page.requireWithin( total );

        final List<String> ids = result.getContentIds().stream().map( Object::toString ).toList();
        final Map<String, Content> sources = links.readable( target, ids );
        final Map<String, List<ContentLinks.Ref>> refs = new LinkedHashMap<>();
        final Set<String> targetIds = new LinkedHashSet<>();
        for ( final String id : ids )
        {
            final Content source = sources.get( id );
            if ( source != null )
            {
                final List<ContentLinks.Ref> outbound = ContentLinks.outbound( source );
                refs.put( id, outbound );
                outbound.forEach( ref -> targetIds.add( ref.id() ) );
            }
        }
        final Map<String, Content> existing = links.readable( target, targetIds );

        final List<List<Map<String, Object>>> brokenBySource = new ArrayList<>();
        final List<Integer> checkedBySource = new ArrayList<>();
        for ( final String id : ids )
        {
            final List<Map<String, Object>> sourceBroken = new ArrayList<>();
            final List<ContentLinks.Ref> sourceRefs = refs.getOrDefault( id, List.of() );
            for ( final ContentLinks.Ref ref : sourceRefs )
            {
                if ( !existing.containsKey( ref.id() ) )
                {
                    sourceBroken.add( broken( sources.get( id ), ref ) );
                }
            }
            brokenBySource.add( sourceBroken );
            checkedBySource.add( sourceRefs.size() );
        }

        final Map<String, Object> structured = fit( target, path, total, page, brokenBySource, checkedBySource );
        return McpResult.success( fitted -> summary( target, path, total, page, fitted ), structured,
                                  ResultBudget.fromConfig( configService, "text" )
                                      .withNarrowHint( "Narrow with path or types, or use a smaller count." ), null, page, total );
    }

    /**
     * The largest page that fits the result budget as a whole, so nothing is cut afterwards: whole sources are added in order, and a
     * page always holds at least one source. When even one source's broken links do not fit (or exceed
     * {@link #MAX_BROKEN_PER_SOURCE}), it is listed partly and named in {@code truncatedSources}; the cursor still moves past it,
     * and find-references with direction outbound lists all of its targets.
     */
    private Map<String, Object> fit( final ProjectBranch target, final String path, final long total, final Page page,
                                     final List<List<Map<String, Object>>> brokenBySource, final List<Integer> checkedBySource )
    {
        final int max = configService.getMcpMaxResultChars();
        Map<String, Object> accepted = null;
        for ( int sources = 1; sources <= brokenBySource.size(); sources++ )
        {
            final Map<String, Object> candidate = page( target, path, total, page, brokenBySource, checkedBySource, sources, MAX_BROKEN_PER_SOURCE );
            if ( fits( candidate, max ) )
            {
                accepted = candidate;
                continue;
            }
            if ( sources > 1 )
            {
                return accepted;
            }
            for ( int listed = Math.min( brokenBySource.get( 0 ).size(), MAX_BROKEN_PER_SOURCE ) - 1; listed >= 0; listed-- )
            {
                accepted = page( target, path, total, page, brokenBySource, checkedBySource, 1, listed );
                if ( listed == 0 || fits( accepted, max ) )
                {
                    break;
                }
            }
            return accepted;
        }
        return accepted != null ? accepted : page( target, path, total, page, brokenBySource, checkedBySource, 0, MAX_BROKEN_PER_SOURCE );
    }

    private static boolean fits( final Map<String, Object> structured, final int max )
    {
        return McpJson.length( structured ) <= max;
    }

    private static Map<String, Object> page( final ProjectBranch target, final String path, final long total, final Page page,
                                             final List<List<Map<String, Object>>> brokenBySource, final List<Integer> checkedBySource,
                                             final int sources, final int perSource )
    {
        final List<Map<String, Object>> broken = new ArrayList<>();
        final List<Map<String, Object>> truncatedSources = new ArrayList<>();
        int checked = 0;
        for ( int i = 0; i < sources; i++ )
        {
            final List<Map<String, Object>> sourceBroken = brokenBySource.get( i );
            final int listed = Math.min( sourceBroken.size(), perSource );
            broken.addAll( sourceBroken.subList( 0, listed ) );
            checked += checkedBySource.get( i );
            if ( listed < sourceBroken.size() )
            {
                final Map<String, Object> truncated = new LinkedHashMap<>();
                truncated.put( "source", sourceBroken.get( 0 ).get( "source" ) );
                truncated.put( "sourceId", sourceBroken.get( 0 ).get( "sourceId" ) );
                truncated.put( "brokenTotal", sourceBroken.size() );
                truncated.put( "listed", listed );
                truncatedSources.add( truncated );
            }
        }

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( "path", path );
        structured.put( "total", total );
        structured.put( "scanned", sources );
        structured.put( "linksChecked", checked );
        structured.put( "brokenOnPage", broken.size() );
        structured.put( "broken", broken );
        if ( !truncatedSources.isEmpty() )
        {
            structured.put( "truncatedSources", truncatedSources );
            structured.put( ResultBudget.NOTE, TRUNCATED_SOURCES_NOTE );
        }
        final String nextCursor = page.nextCursor( total, sources );
        structured.put( "complete", nextCursor == null );
        structured.put( ResultBudget.NEXT_CURSOR, nextCursor );
        return structured;
    }

    private static Map<String, Object> broken( final Content source, final ContentLinks.Ref ref )
    {
        final Map<String, Object> entry = new LinkedHashMap<>();
        entry.put( "source", source.getPath().toString() );
        entry.put( "sourceId", source.getId().toString() );
        entry.put( "field", ref.field() );
        entry.put( "kind", ContentLinks.kind( ref, null ) );
        entry.put( "targetId", ref.id() );
        entry.put( "text", ref.text() );
        entry.put( "reason", ref.valid() ? ContentLinks.MISSING_REASON : ContentLinks.INVALID_REASON );
        return entry;
    }

    private static String summary( final ProjectBranch target, final String path, final long total, final Page page,
                                   final Map<String, Object> structured )
    {
        final String where = ( path != null && !path.isBlank() ? " under " + path : "" ) + " in " + target.project() + " (" +
            target.branch() + ")";
        if ( total == 0 )
        {
            return "No content with internal links or references" + where + ".";
        }
        final String scope = total + ( total == 1 ? " item" : " items" ) + " with internal links or references" + where;
        final int scanned = structured.get( "scanned" ) instanceof Number number ? number.intValue() : 0;
        if ( scanned == 0 )
        {
            return scope + ".";
        }
        final int broken = structured.get( "broken" ) instanceof List<?> list ? list.size() : 0;
        String summary = scope + "; checked " + ( page.offset() + 1 ) + "-" + ( page.offset() + scanned ) + ": " + broken +
            ( broken == 1 ? " broken link listed." : " broken links listed." );
        if ( structured.get( ResultBudget.NEXT_CURSOR ) != null )
        {
            summary += " Not complete: continue with nextCursor.";
        }
        if ( structured.get( "truncatedSources" ) instanceof List<?> truncated && !truncated.isEmpty() )
        {
            summary += " " + TRUNCATED_SOURCES_NOTE;
        }
        return summary;
    }
}
