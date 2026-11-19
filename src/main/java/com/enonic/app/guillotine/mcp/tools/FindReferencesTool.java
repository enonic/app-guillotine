package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.enonic.xp.aggregation.Aggregation;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.query.aggregation.TermsAggregationQuery;
import com.enonic.xp.query.expr.CompareExpr;
import com.enonic.xp.query.expr.FieldExpr;
import com.enonic.xp.query.expr.ValueExpr;
import com.enonic.xp.query.filter.BooleanFilter;
import com.enonic.xp.query.filter.ValueFilter;

/**
 * What references a content (inbound), what it references (outbound), and media that nothing references. Searches run in the
 * caller's context on the {@code _references} index field, so references held by content the caller cannot read are neither listed
 * nor counted.
 */
public final class FindReferencesTool
    implements McpTool
{
    static final int MAX_MEDIA = 10000;

    static final int MAX_OUTBOUND = 200;

    static final int MEDIA_CHUNK = 500;

    static final int MAX_REFERENCE_BUCKETS = 10000;

    static final int MAX_SPLIT_QUERIES = 200;

    static final int MAX_OUTBOUND_SCAN = 5000;

    private static final String REFERENCED = "referenced";

    private static final String NARROW_HINT = "Use a smaller count or narrow with types.";

    private static final String OUTBOUND_HINT = "Narrow with types.";

    private final ContentLinks links;

    private final ProjectBranchResolver resolver;

    private final GuillotineConfigService configService;

    private final int maxReferenceBuckets;

    private final int maxSplitQueries;

    FindReferencesTool( final ContentLinks links, final ProjectBranchResolver resolver, final GuillotineConfigService configService,
                        final int maxReferenceBuckets, final int maxSplitQueries )
    {
        this.links = links;
        this.resolver = resolver;
        this.configService = configService;
        this.maxReferenceBuckets = maxReferenceBuckets;
        this.maxSplitQueries = maxSplitQueries;
    }

    public FindReferencesTool( final ContentService contentService, final ProjectBranchResolver resolver,
                               final GuillotineConfigService configService )
    {
        this( new ContentLinks( contentService ), resolver, configService, MAX_REFERENCE_BUCKETS, MAX_SPLIT_QUERIES );
    }

    @Override
    public String name()
    {
        return "find-references";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final boolean unusedMedia = unusedMedia( args.get( "unusedMedia" ) );
        final String direction = direction( args.get( "direction" ) );
        if ( unusedMedia && args.get( "key" ) != null )
        {
            throw new ToolError( "Pass either key or unusedMedia, not both.",
                                 "Use unusedMedia: true alone to list media nothing references, or key to see references of one item." );
        }
        if ( unusedMedia && args.get( "direction" ) != null )
        {
            throw new ToolError( "direction does not apply to unusedMedia.", "Remove direction, or pass key instead of unusedMedia." );
        }
        if ( !unusedMedia && args.get( "key" ) == null )
        {
            throw new ToolError( "key is required unless unusedMedia is true.",
                                 "Pass the _id or _path of a content (call search to find it), or unusedMedia: true." );
        }
        final String key = unusedMedia ? null : ContentKeys.key( args.get( "key" ) );
        final List<String> types = ContentLinks.types( args.get( "types" ), unusedMedia );

        final ProjectBranch target = resolver.resolve( args );
        final Page page = Cursors.page( args, target, "only the total" );

        return unusedMedia ? unused( target, types, page ) : references( target, key, direction, types, page );
    }

    private Map<String, Object> references( final ProjectBranch target, final String key, final String direction,
                                            final List<String> types, final Page page )
    {
        final Content content = links.get( target, key );
        if ( content == null && key.startsWith( "/" ) )
        {
            throw notFound( target, key );
        }
        if ( content == null && "outbound".equals( direction ) )
        {
            throw notFound( target, key );
        }
        final String id = content != null ? content.getId().toString() : key;

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        final Map<String, Object> described = content != null ? ContentLinks.summary( content ) : missingTarget( id );
        structured.put( "target", described );
        final String name = content != null ? content.getPath().toString() : id;

        long total = 0;
        if ( !"outbound".equals( direction ) )
        {
            final FindContentIdsByQueryResult result = links.find( target, inboundQuery( id, types, page ) );
            total = result.getTotalHits();
            page.requireWithin( total );
            structured.put( "total", total );
            if ( page.count() > 0 )
            {
                final List<Map<String, Object>> inbound = summaries( target, result.getContentIds().stream().map( Object::toString ).toList() );
                structured.put( "inbound", inbound );
                structured.put( ResultBudget.NEXT_CURSOR, page.nextCursor( total, inbound.size() ) );
            }
        }
        if ( content != null && !"inbound".equals( direction ) )
        {
            outbound( target, content, types, structured );
        }

        final long inboundTotal = total;
        final String narrowHint = "outbound".equals( direction ) ? OUTBOUND_HINT : NARROW_HINT;
        return McpResult.success( result -> {
            final List<String> summary = new ArrayList<>();
            if ( result.containsKey( "total" ) )
            {
                summary.add( inboundSummary( name, inboundTotal, page, size( result.get( "inbound" ) ) ) );
            }
            if ( result.get( "outbound" ) instanceof List<?> outbound )
            {
                final long missing = outbound.stream().filter( entry -> entry instanceof Map<?, ?> map && Boolean.TRUE.equals( map.get( "missing" ) ) ).count();
                final String listed = result.get( "outboundTotal" ) instanceof Number all && all.longValue() > outbound.size()
                    ? "the first " + outbound.size() + " of " + all + " items it references"
                    : outbound.size() + ( outbound.size() == 1 ? " item it references" : " items it references" );
                summary.add( name + ": listing " + listed + ( missing > 0 ? ", " + missing + " missing" : "" ) + "." );
                if ( result.get( "outboundHint" ) != null )
                {
                    summary.add( String.valueOf( result.get( "outboundHint" ) ) );
                }
            }
            if ( content == null )
            {
                summary.add( "The target itself is missing or not readable." );
            }
            return String.join( " ", summary ) + " (" + target.project() + ", " + target.branch() + ")";
        }, structured, ResultBudget.fromConfig( configService, "text" ).withNarrowHint( narrowHint ), "inbound", page, total );
    }

    private static int size( final Object list )
    {
        return list instanceof List<?> items ? items.size() : 0;
    }

    private static ContentQuery inboundQuery( final String id, final List<String> types, final Page page )
    {
        return ContentQuery.create()
            .queryExpr( ContentLinks.byPath( ContentLinks.typeConstraint( types ) ) )
            .queryFilter( BooleanFilter.create()
                              .must( ValueFilter.create().fieldName( ContentLinks.REFERENCES ).addValues( id ).build() )
                              .mustNot( ValueFilter.create().fieldName( "_id" ).addValues( id ).build() )
                              .build() )
            .from( page.offset() )
            .size( page.count() )
            .build();
    }

    /**
     * Puts {@code outbound} (at most {@link #MAX_OUTBOUND} entries) into {@code structured}, with {@code outboundTotal},
     * {@code outboundTruncated} and {@code outboundHint} when more were found. Type filtering happens before the cut.
     */
    private void outbound( final ProjectBranch target, final Content content, final List<String> types,
                           final Map<String, Object> structured )
    {
        final List<ContentLinks.Ref> refs = ContentLinks.outbound( content );
        final List<ContentLinks.Ref> scanned = refs.subList( 0, Math.min( refs.size(), MAX_OUTBOUND_SCAN ) );
        final Map<String, Content> readable = links.readable( target, scanned.stream().map( ContentLinks.Ref::id ).toList() );

        final List<Map<String, Object>> result = new ArrayList<>();
        for ( final ContentLinks.Ref ref : scanned )
        {
            final Content referenced = readable.get( ref.id() );
            if ( referenced != null && !types.isEmpty() && !matches( referenced, types ) )
            {
                continue;
            }
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put( "field", ref.field() );
            entry.put( "kind", ContentLinks.kind( ref, referenced ) );
            if ( referenced != null )
            {
                entry.putAll( ContentLinks.summary( referenced ) );
            }
            else
            {
                entry.put( "_id", ref.id() );
                entry.put( "missing", true );
                entry.put( "reason", ref.valid() ? null : ContentLinks.INVALID_REASON );
            }
            entry.put( "text", ref.text() );
            result.add( entry );
        }

        final boolean scanLimited = refs.size() > scanned.size();
        if ( result.size() > MAX_OUTBOUND || scanLimited )
        {
            structured.put( "outbound", result.subList( 0, Math.min( result.size(), MAX_OUTBOUND ) ) );
            structured.put( "outboundTotal", scanLimited ? refs.size() : result.size() );
            structured.put( "outboundTruncated", true );
            structured.put( "outboundHint", "Only the first " + MAX_OUTBOUND + " references are listed; pass types to narrow them, " +
                "or call check-links with path " + content.getPath() + " to list only the broken ones." );
        }
        else
        {
            structured.put( "outbound", result );
        }
    }

    private Map<String, Object> unused( final ProjectBranch target, final List<String> types, final Page page )
    {
        final FindContentIdsByQueryResult media = links.find( target, ContentQuery.create()
            .queryExpr( ContentLinks.byPath( types.isEmpty()
                                                 ? CompareExpr.like( FieldExpr.from( "type" ), ValueExpr.string( "media:*" ) )
                                                 : ContentLinks.typeConstraint( types ) ) )
            .from( 0 )
            .size( MAX_MEDIA )
            .build() );
        final List<String> mediaIds = media.getContentIds().stream().map( Object::toString ).toList();

        final Usage usage = new Usage( maxSplitQueries );
        for ( int from = 0; from < mediaIds.size(); from += MEDIA_CHUNK )
        {
            referenced( target, mediaIds.subList( from, Math.min( mediaIds.size(), from + MEDIA_CHUNK ) ), usage );
        }
        final List<String> unused = mediaIds.stream()
            .map( ContentLinks::normalizeId )
            .filter( id -> !usage.referenced.contains( id ) && !usage.undecided.contains( id ) )
            .toList();

        final long total = unused.size();
        page.requireWithin( total );
        final List<String> pageIds = unused.subList( Math.min( unused.size(), page.offset() ),
                                                     (int) Math.min( unused.size(), (long) page.offset() + page.count() ) );

        final boolean limited = media.getTotalHits() > mediaIds.size();
        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( "mediaChecked", mediaIds.size() );
        structured.put( "mediaLimitReached", limited ? Boolean.TRUE : null );
        if ( !usage.undecided.isEmpty() )
        {
            structured.put( "incomplete", true );
            structured.put( "unchecked", usage.undecided.size() );
        }
        structured.put( "total", total );
        structured.put( ResultBudget.NOTE, unusedNote( limited, usage.undecided.size() ) );
        if ( page.count() > 0 )
        {
            final List<Map<String, Object>> items = summaries( target, pageIds );
            structured.put( "unused", items );
            structured.put( ResultBudget.NEXT_CURSOR, page.nextCursor( total, items.size() ) );
        }

        final long totalMedia = media.getTotalHits();
        return McpResult.success( result -> {
            final int shown = size( result.get( "unused" ) );
            String summary = total + " of " + mediaIds.size() + " media items are not referenced by any content the current user " +
                "can read in " + target.project() + " (" + target.branch() + ")";
            summary += shown == 0 ? "." : "; showing " + ( page.offset() + 1 ) + "-" + ( page.offset() + shown ) + ".";
            if ( limited )
            {
                summary += " Only the first " + MAX_MEDIA + " of " + totalMedia + " media items (by path) were checked; narrow with types.";
            }
            if ( !usage.undecided.isEmpty() )
            {
                summary += " Incomplete: " + usage.undecided.size() + " media items could not be checked within the query limit and " +
                    "are not listed; narrow with types.";
            }
            return summary;
        }, structured, ResultBudget.fromConfig( configService ).withNarrowHint( NARROW_HINT ), "unused", page, total );
    }

    private static String unusedNote( final boolean limited, final int undecided )
    {
        String note = "Unused means not referenced by any content readable to the current user.";
        if ( limited )
        {
            note += " Only the first " + MAX_MEDIA + " media items by path were checked; narrow with types.";
        }
        if ( undecided > 0 )
        {
            note += " Incomplete: " + undecided + " media items could not be checked and are not listed; narrow with types.";
        }
        return note;
    }

    /**
     * Referenced media ids, and ids left undecided when the extra queries for halving chunks ran out.
     */
    private static final class Usage
    {
        final Set<String> referenced = new HashSet<>();

        final Set<String> undecided = new HashSet<>();

        int splitQueriesLeft;

        Usage( final int splitQueries )
        {
            this.splitQueriesLeft = splitQueries;
        }
    }

    /**
     * Adds the ids among {@code chunk} that readable content references. One terms aggregation over {@code _references} answers a
     * chunk; when its buckets may be cut off, the chunk is halved until a single id is decided by its hit count. Halving stops when
     * the extra queries are used up, leaving the rest of the chunk undecided.
     */
    private void referenced( final ProjectBranch target, final List<String> chunk, final Usage usage )
    {
        final FindContentIdsByQueryResult result = links.find( target, ContentQuery.create()
            .queryFilter( ValueFilter.create().fieldName( ContentLinks.REFERENCES ).addValues( chunk ).build() )
            .aggregationQuery( TermsAggregationQuery.create( REFERENCED )
                                   .fieldName( ContentLinks.REFERENCES )
                                   .size( maxReferenceBuckets )
                                   .build() )
            .size( 0 )
            .build() );
        if ( result.getTotalHits() == 0 )
        {
            return;
        }
        if ( chunk.size() == 1 )
        {
            usage.referenced.add( ContentLinks.normalizeId( chunk.get( 0 ) ) );
            return;
        }

        final Set<String> keys = new HashSet<>();
        final Aggregation aggregation = result.getAggregations() != null ? result.getAggregations().get( REFERENCED ) : null;
        int buckets = 0;
        if ( aggregation instanceof BucketAggregation bucketAggregation && bucketAggregation.getBuckets() != null )
        {
            for ( final Bucket bucket : bucketAggregation.getBuckets() )
            {
                keys.add( ContentLinks.normalizeId( bucket.getKey() ) );
                buckets++;
            }
        }
        final List<String> found = chunk.stream().map( ContentLinks::normalizeId ).filter( keys::contains ).toList();
        if ( buckets < maxReferenceBuckets && !found.isEmpty() )
        {
            usage.referenced.addAll( found );
            return;
        }
        if ( usage.splitQueriesLeft < 2 )
        {
            usage.undecided.addAll( chunk.stream().map( ContentLinks::normalizeId ).filter( id -> !usage.referenced.contains( id ) ).toList() );
            return;
        }
        usage.splitQueriesLeft -= 2;
        final int half = chunk.size() / 2;
        referenced( target, chunk.subList( 0, half ), usage );
        referenced( target, chunk.subList( half, chunk.size() ), usage );
    }

    private List<Map<String, Object>> summaries( final ProjectBranch target, final List<String> ids )
    {
        final Map<String, Content> readable = links.readable( target, ids );
        return ids.stream().map( readable::get ).filter( content -> content != null ).map( ContentLinks::summary ).toList();
    }

    private static boolean matches( final Content content, final List<String> types )
    {
        final String type = content.getType().toString();
        return types.stream().anyMatch( name -> name.contains( ":" ) ? name.equals( type ) : type.endsWith( ":" + name ) );
    }

    private static Map<String, Object> missingTarget( final String id )
    {
        final Map<String, Object> target = new LinkedHashMap<>();
        target.put( "_id", id );
        target.put( "missing", true );
        target.put( "reason", ContentLinks.MISSING_REASON );
        return target;
    }

    private static ToolError notFound( final ProjectBranch target, final String key )
    {
        return new ToolError( "Content " + McpJson.write( key ) + " not found in project '" + target.project() + "' on branch '" +
                                  target.branch() + "', or not readable by the current user.",
                              "Pass an _id or _path returned by search; master holds only published content. " +
                                  "An id that no longer exists can still be passed with direction inbound." );
    }

    private static String inboundSummary( final String name, final long total, final Page page, final int returned )
    {
        if ( total == 0 )
        {
            return "Nothing references " + name + ".";
        }
        final String found = total + ( total == 1 ? " item references " : " items reference " ) + name;
        return returned == 0 ? found + "." : found + "; showing " + ( page.offset() + 1 ) + "-" + ( page.offset() + returned ) + ".";
    }

    private static boolean unusedMedia( final Object value )
    {
        if ( value == null )
        {
            return false;
        }
        if ( value instanceof Boolean flag )
        {
            return flag;
        }
        throw new ToolError( "Invalid unusedMedia: expected true or false." );
    }

    private static String direction( final Object value )
    {
        if ( value == null || "".equals( value ) )
        {
            return "both";
        }
        if ( "inbound".equals( value ) || "outbound".equals( value ) || "both".equals( value ) )
        {
            return (String) value;
        }
        throw new ToolError( "Invalid direction: " + McpJson.write( value ) + ".",
                             "Use inbound (what references the key), outbound (what the key references) or both (default)." );
    }
}
