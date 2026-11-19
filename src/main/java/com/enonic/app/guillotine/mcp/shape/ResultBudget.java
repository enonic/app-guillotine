package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.enonic.app.guillotine.GuillotineConfigService;

/**
 * Keeps the structured object of a tool result within a character budget, measured as the length of its compact JSON. The result
 * carries that JSON twice (as {@code structuredContent} and, escaped, in the {@code content} text after a summary of at most a
 * quarter of the budget, escaped); escaping at most doubles compact JSON, so the whole result stays within three and a quarter times
 * the budget plus a few characters of envelope. Oversized parts of the structured object are replaced with a
 * {@code {"truncated": true, "chars": N}} marker or trailing list items are dropped; the object then gets top-level
 * {@code truncated: true} and a {@code hint}. JSON is never cut mid-way.
 */
public final class ResultBudget
{
    public static final int MIN_RESULT_CHARS = 1000;

    public static final int DEFAULT_MAX_SUBTREE_CHARS = 6000;

    public static final String TRUNCATED = "truncated";

    public static final String HINT = "hint";

    public static final String NEXT_CURSOR = "nextCursor";

    public static final String DEFAULT_NARROW_HINT = "Narrow the request.";

    public static final String NOTE = "note";

    private static final String CHILDREN_SUFFIX = ".*";

    private static final Set<String> PROTECTED_KEYS =
        Set.of( "project", "branch", "total", NEXT_CURSOR, McpResult.ERRORS, TRUNCATED, HINT, NOTE );

    private static final int MAX_HINT_DETAILS = 3;

    private static final int[] REQUIRED_STRING_CHARS = {Integer.MAX_VALUE, 200, 20};

    private static final int MAX_HINT_PATH = 80;

    private final int maxResultChars;

    private final int maxSubtreeChars;

    private final Set<String> subtreeKeys;

    private final Set<String> childSubtreeKeys;

    private final String listOnlyKey;

    private final Set<String> requiredKeys;

    private final String narrowHint;

    private final boolean keepNulls;

    /**
     * {@code subtreeKeys} name keys, at any depth, whose value must not exceed {@code maxSubtreeChars}; a key written as
     * {@code "data.*"} applies the limit to each child of a {@code data} object instead of to the object as a whole.
     */
    public ResultBudget( final int maxResultChars, final int maxSubtreeChars, final Collection<String> subtreeKeys )
    {
        this( maxResultChars, maxSubtreeChars, subtreeKeys, null, Set.of(), DEFAULT_NARROW_HINT, false );
    }

    private ResultBudget( final int maxResultChars, final int maxSubtreeChars, final Collection<String> subtreeKeys,
                          final String listOnlyKey, final Set<String> requiredKeys, final String narrowHint, final boolean keepNulls )
    {
        this.maxResultChars = Math.max( MIN_RESULT_CHARS, maxResultChars );
        this.maxSubtreeChars = Math.max( 1, maxSubtreeChars );
        this.subtreeKeys = Set.copyOf( subtreeKeys );
        this.childSubtreeKeys = subtreeKeys.stream()
            .filter( key -> key.endsWith( CHILDREN_SUFFIX ) )
            .map( key -> key.substring( 0, key.length() - CHILDREN_SUFFIX.length() ) )
            .collect( Collectors.toUnmodifiableSet() );
        this.listOnlyKey = listOnlyKey;
        this.requiredKeys = Set.copyOf( requiredKeys );
        this.narrowHint = narrowHint;
        this.keepNulls = keepNulls;
    }

    public static ResultBudget of( final int maxResultChars, final String... subtreeKeys )
    {
        return new ResultBudget( maxResultChars, DEFAULT_MAX_SUBTREE_CHARS, List.of( subtreeKeys ) );
    }

    public static ResultBudget fromConfig( final GuillotineConfigService configService, final String... subtreeKeys )
    {
        return of( configService.getMcpMaxResultChars(), subtreeKeys );
    }

    /**
     * A budget that only drops trailing items of the top-level {@code listKey} list (possibly all of them) and never replaces values
     * with markers, so the remaining items keep their declared schema. When the result does not fit even without items, it keeps
     * {@code listKey} and the {@code requiredKeys} of the output schema, with strings shortened if they alone do not fit.
     */
    public static ResultBudget listOnly( final int maxResultChars, final String listKey, final String narrowHint,
                                         final String... requiredKeys )
    {
        final Set<String> required = new LinkedHashSet<>( List.of( requiredKeys ) );
        required.add( listKey );
        return new ResultBudget( maxResultChars, DEFAULT_MAX_SUBTREE_CHARS, List.of(), listKey, required, narrowHint, false );
    }

    /**
     * The same budget with {@code narrowHint} as the last sentence of the {@code hint} it adds.
     */
    public ResultBudget withNarrowHint( final String narrowHint )
    {
        return new ResultBudget( maxResultChars, maxSubtreeChars, subtreeKeys, listOnlyKey, requiredKeys, narrowHint, keepNulls );
    }

    /**
     * The same budget without removing null-valued fields, for results that declare no output schema.
     */
    public ResultBudget keepingNulls()
    {
        return new ResultBudget( maxResultChars, maxSubtreeChars, subtreeKeys, listOnlyKey, requiredKeys, narrowHint, true );
    }

    public int maxResultChars()
    {
        return maxResultChars;
    }

    public int maxSubtreeChars()
    {
        return maxSubtreeChars;
    }

    /**
     * Builds the complete MCP result for {@code summary} and {@code structured}, with {@code structured} cut down so its compact JSON
     * fits the budget.
     */
    public Map<String, Object> apply( final String summary, final Map<String, ?> structured )
    {
        return apply( summary, structured, null, null, 0 );
    }

    /**
     * Same as {@link #apply(String, Map)}, and when trailing items of the top-level {@code pageListKey} list are dropped,
     * {@code nextCursor} is recomputed so the next page starts right after the last item returned.
     */
    public Map<String, Object> apply( final String summary, final Map<String, ?> structured, final String pageListKey, final Page page,
                                      final long total )
    {
        return apply( result -> summary, structured, pageListKey, page, total );
    }

    /**
     * Same as {@link #apply(String, Map, String, Page, long)}, with the summary computed from the structured object as it is finally
     * returned (after cuts), so the text can tell how many items it actually holds.
     */
    public Map<String, Object> apply( final Function<Map<String, Object>, String> summary, final Map<String, ?> structured,
                                      final String pageListKey, final Page page, final long total )
    {
        final Map<String, Object> result = keepNulls ? Nulls.copy( structured ) : Nulls.clean( structured );
        final Cuts cuts = new Cuts();

        cutSubtrees( result, "", cuts );
        if ( cuts.isEmpty() && McpJson.length( result ) <= maxResultChars )
        {
            return McpResult.envelope( fitSummary( summary.apply( result ) ), result );
        }

        while ( true )
        {
            markTruncated( result, cuts );
            final int length = McpJson.length( result );
            if ( length <= maxResultChars )
            {
                return McpResult.envelope( fitSummary( summary.apply( result ) ), result );
            }
            if ( !cutLargest( result, length - maxResultChars, cuts ) )
            {
                final Map<String, Object> reduced = fallback( result, cuts );
                return McpResult.envelope( fitSummary( summary.apply( reduced ) ), reduced );
            }
            if ( pageListKey != null && page != null && result.get( pageListKey ) instanceof List<?> items )
            {
                putOrRemove( result, NEXT_CURSOR, page.nextCursor( total, items.size() ) );
            }
        }
    }

    private String fitSummary( final String summary )
    {
        final int max = maxResultChars / 4;
        if ( summary == null )
        {
            return "";
        }
        String fitted = McpJson.shorten( summary, max );
        int escaped = McpJson.length( fitted ) - 2;
        while ( escaped > max )
        {
            fitted = McpJson.shorten( summary, (int) ( (long) fitted.length() * max / escaped ) );
            escaped = McpJson.length( fitted ) - 2;
        }
        return fitted;
    }

    private void cutSubtrees( final Object node, final String path, final Cuts cuts )
    {
        if ( node instanceof Map<?, ?> map )
        {
            @SuppressWarnings("unchecked") final Map<String, Object> object = (Map<String, Object>) map;
            for ( final Map.Entry<String, Object> entry : object.entrySet() )
            {
                final String childPath = child( path, entry.getKey() );
                if ( subtreeKeys.contains( entry.getKey() ) && entry.getValue() != null )
                {
                    cutSubtree( entry, childPath, cuts );
                }
                else if ( childSubtreeKeys.contains( entry.getKey() ) && entry.getValue() instanceof Map<?, ?> children )
                {
                    @SuppressWarnings("unchecked") final Map<String, Object> values = (Map<String, Object>) children;
                    for ( final Map.Entry<String, Object> value : values.entrySet() )
                    {
                        if ( value.getValue() != null )
                        {
                            cutSubtree( value, child( childPath, value.getKey() ), cuts );
                        }
                    }
                }
                else
                {
                    cutSubtrees( entry.getValue(), childPath, cuts );
                }
            }
        }
        else if ( node instanceof List<?> list )
        {
            for ( int i = 0; i < list.size(); i++ )
            {
                cutSubtrees( list.get( i ), path + "[" + i + "]", cuts );
            }
        }
    }

    private void cutSubtree( final Map.Entry<String, Object> entry, final String path, final Cuts cuts )
    {
        final int length = McpJson.length( entry.getValue() );
        if ( length > maxSubtreeChars )
        {
            entry.setValue( marker( length ) );
            cuts.replaced( path, length + " chars, over the " + maxSubtreeChars + "-char limit for one value" );
        }
        else
        {
            cutSubtrees( entry.getValue(), path, cuts );
        }
    }

    private boolean cutLargest( final Map<String, Object> root, final int excess, final Cuts cuts )
    {
        if ( listOnlyKey != null )
        {
            return dropTrailing( root, excess, cuts );
        }

        final Map<String, Integer> lengths = new LinkedHashMap<>();
        root.forEach( ( key, value ) -> {
            if ( !PROTECTED_KEYS.contains( key ) )
            {
                lengths.put( key, McpJson.length( value ) );
            }
        } );

        final List<Map.Entry<String, Integer>> candidates = new ArrayList<>( lengths.entrySet() );
        candidates.sort( Map.Entry.<String, Integer>comparingByValue().reversed() );
        for ( final Map.Entry<String, Integer> candidate : candidates )
        {
            if ( cut( new Slot( root, candidate.getKey() ), candidate.getValue(), candidate.getKey(), excess, cuts ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean cut( final Slot slot, final int length, final String path, final int excess, final Cuts cuts )
    {
        final Object value = slot.get();

        if ( value instanceof Map<?, ?> map && !map.isEmpty() )
        {
            @SuppressWarnings("unchecked") final Map<String, Object> object = (Map<String, Object>) map;
            String largestKey = null;
            int largestLength = -1;
            for ( final Map.Entry<String, Object> entry : object.entrySet() )
            {
                final int childLength = McpJson.length( entry.getValue() );
                if ( childLength > largestLength )
                {
                    largestKey = entry.getKey();
                    largestLength = childLength;
                }
            }
            if ( largestLength - markerLength( largestLength ) >= excess &&
                cut( new Slot( object, largestKey ), largestLength, child( path, largestKey ), excess, cuts ) )
            {
                return true;
            }
        }
        else if ( value instanceof List<?> list && !list.isEmpty() )
        {
            @SuppressWarnings("unchecked") final List<Object> items = (List<Object>) list;
            if ( items.size() > 1 )
            {
                int removed = 0;
                int dropped = 0;
                while ( items.size() > 1 && removed < excess )
                {
                    removed += McpJson.length( items.remove( items.size() - 1 ) ) + 1;
                    dropped++;
                }
                cuts.dropped( path, dropped );
                return true;
            }
            if ( cut( new Slot( items, 0 ), McpJson.length( items.get( 0 ) ), path + "[0]", excess, cuts ) )
            {
                return true;
            }
        }

        if ( length <= markerLength( length ) )
        {
            return false;
        }
        slot.set( marker( length ) );
        cuts.replaced( path, length );
        return true;
    }

    private boolean dropTrailing( final Map<String, Object> root, final int excess, final Cuts cuts )
    {
        if ( !( root.get( listOnlyKey ) instanceof List<?> list ) || list.isEmpty() )
        {
            return false;
        }
        @SuppressWarnings("unchecked") final List<Object> items = (List<Object>) list;
        int removed = 0;
        int dropped = 0;
        while ( !items.isEmpty() && removed < excess )
        {
            removed += McpJson.length( items.remove( items.size() - 1 ) ) + 1;
            dropped++;
        }
        cuts.dropped( listOnlyKey, dropped );
        return true;
    }

    private Map<String, Object> fallback( final Map<String, Object> result, final Cuts cuts )
    {
        final Map<String, Object> reduced = new LinkedHashMap<>();
        for ( final Map.Entry<String, Object> entry : result.entrySet() )
        {
            if ( PROTECTED_KEYS.contains( entry.getKey() ) && !NEXT_CURSOR.equals( entry.getKey() ) ||
                requiredKeys.contains( entry.getKey() ) )
            {
                reduced.put( entry.getKey(), entry.getValue() );
            }
        }
        cuts.replaced( "", McpJson.length( result ) );
        markTruncated( reduced, cuts );
        if ( McpJson.length( reduced ) <= maxResultChars )
        {
            return reduced;
        }

        Map<String, Object> minimal = null;
        for ( final int maxChars : REQUIRED_STRING_CHARS )
        {
            minimal = new LinkedHashMap<>();
            for ( final String key : requiredKeys )
            {
                if ( result.containsKey( key ) )
                {
                    minimal.put( key, shortenStrings( result.get( key ), maxChars ) );
                }
            }
            minimal.put( TRUNCATED, true );
            minimal.put( HINT, "Result does not fit " + maxResultChars + " characters. " + narrowHint );
            if ( requiredKeys.isEmpty() || McpJson.length( minimal ) <= maxResultChars )
            {
                return minimal;
            }
        }
        return minimal;
    }

    private static Object shortenStrings( final Object value, final int maxChars )
    {
        if ( value instanceof String text )
        {
            return McpJson.shorten( text, maxChars );
        }
        if ( value instanceof Map<?, ?> map )
        {
            final Map<Object, Object> result = new LinkedHashMap<>();
            map.forEach( ( key, item ) -> result.put( key, shortenStrings( item, maxChars ) ) );
            return result;
        }
        if ( value instanceof List<?> list )
        {
            return list.stream().map( item -> shortenStrings( item, maxChars ) ).toList();
        }
        return value;
    }

    private void markTruncated( final Map<String, Object> result, final Cuts cuts )
    {
        result.put( TRUNCATED, true );
        result.put( HINT, "Result truncated to fit " + maxResultChars + " characters: " + cuts.describe() + ". " + narrowHint +
            ( result.containsKey( NEXT_CURSOR ) ? " Continue with nextCursor." : "" ) );
    }

    private static Map<String, Object> marker( final int length )
    {
        final Map<String, Object> marker = new LinkedHashMap<>();
        marker.put( TRUNCATED, true );
        marker.put( "chars", length );
        return marker;
    }

    private static int markerLength( final int length )
    {
        return McpJson.length( marker( length ) );
    }

    private static void putOrRemove( final Map<String, Object> map, final String key, final Object value )
    {
        if ( value == null )
        {
            map.remove( key );
        }
        else
        {
            map.put( key, value );
        }
    }

    private static String child( final String path, final String key )
    {
        return path.isEmpty() ? key : path + "." + key;
    }

    private record Slot(Object container, Object key)
    {
        @SuppressWarnings("unchecked")
        Object get()
        {
            return container instanceof Map<?, ?> map ? map.get( key ) : ( (List<Object>) container ).get( (Integer) key );
        }

        @SuppressWarnings("unchecked")
        void set( final Object value )
        {
            if ( container instanceof Map<?, ?> )
            {
                ( (Map<Object, Object>) container ).put( key, value );
            }
            else
            {
                ( (List<Object>) container ).set( (Integer) key, value );
            }
        }
    }

    private static final class Cuts
    {
        private final Map<String, String> replaced = new LinkedHashMap<>();

        private final Map<String, Integer> dropped = new LinkedHashMap<>();

        void replaced( final String path, final int length )
        {
            replaced( path, length + " chars" );
        }

        void replaced( final String path, final String what )
        {
            replaced.put( path, what );
        }

        void dropped( final String path, final int count )
        {
            dropped.merge( path, count, Integer::sum );
        }

        boolean isEmpty()
        {
            return replaced.isEmpty() && dropped.isEmpty();
        }

        String describe()
        {
            final List<String> details = new ArrayList<>();
            dropped.forEach( ( path, count ) -> details.add( "dropped " + count + " trailing item(s) of '" + shorten( path ) + "'" ) );
            replaced.forEach( ( path, what ) -> details.add(
                path.isEmpty() ? "dropped all content (" + what + ")" : "replaced '" + shorten( path ) + "' (" + what + ")" ) );
            if ( details.size() <= MAX_HINT_DETAILS )
            {
                return String.join( "; ", details );
            }
            return String.join( "; ", details.subList( 0, MAX_HINT_DETAILS ) ) + "; and " + ( details.size() - MAX_HINT_DETAILS ) +
                " more";
        }

        private static String shorten( final String path )
        {
            return path.length() <= MAX_HINT_PATH ? path : "..." + path.substring( path.length() - MAX_HINT_PATH );
        }
    }
}
