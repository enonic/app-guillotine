package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import com.enonic.app.guillotine.GuillotineConfigService;

/**
 * Keeps a structured result within {@code mcp.maxResultChars}, measured as the length of its compact JSON. The text content repeats
 * that JSON, so the whole result is about twice the budget, plus escaping. A cut result gets top-level {@code truncated: true} and a
 * {@code hint}; JSON is never cut mid-way.
 * <p>
 * Paged results drop trailing items and move {@code nextCursor} to the first item not returned; when the first item alone does not
 * fit, the page is empty and {@code nextCursor} skips that item. Tools that return one free-form
 * object instead ({@code get}, {@code query}) call {@link #fitSubtrees}, which cuts the value under one key top-down: lists lose
 * trailing items and large values become {@code {"truncated": true, "chars": N}} markers. A result that still does not fit keeps only
 * project, branch and total.
 */
public final class ResultBudget
{
    public static final int MIN_RESULT_CHARS = 1000;

    public static final String TRUNCATED = "truncated";

    public static final String HINT = "hint";

    public static final String NEXT_CURSOR = "nextCursor";

    public static final String NOTE = "note";

    public static final String TOTAL = "total";

    public static final String DEFAULT_NARROW_HINT = "Narrow the request.";

    private static final List<String> KEPT_KEYS = List.of( ProjectBranch.PROJECT, ProjectBranch.BRANCH, TOTAL );

    private static final int MAX_HINT_CUTS = 3;

    private final int maxResultChars;

    private final String narrowHint;

    private final boolean keepNullFields;

    public ResultBudget( final int maxResultChars )
    {
        this( maxResultChars, DEFAULT_NARROW_HINT, false );
    }

    private ResultBudget( final int maxResultChars, final String narrowHint, final boolean keepNullFields )
    {
        this.maxResultChars = Math.max( MIN_RESULT_CHARS, maxResultChars );
        this.narrowHint = narrowHint;
        this.keepNullFields = keepNullFields;
    }

    public static ResultBudget fromConfig( final GuillotineConfigService configService )
    {
        return new ResultBudget( configService.getMcpMaxResultChars() );
    }

    /**
     * The same budget with {@code narrowHint}, which says how to ask for less, as the last sentence of the {@code hint} it adds.
     */
    public ResultBudget withNarrowHint( final String narrowHint )
    {
        return new ResultBudget( maxResultChars, narrowHint, keepNullFields );
    }

    /**
     * The same budget without removing null object fields, for GraphQL data, where a null field is an answer.
     */
    public ResultBudget keepingNullFields()
    {
        return new ResultBudget( maxResultChars, narrowHint, true );
    }

    public int maxResultChars()
    {
        return maxResultChars;
    }

    /**
     * The MCP result for {@code structured} when it fits, else for the project, branch and total only.
     */
    public Map<String, Object> fit( final Map<String, ?> structured )
    {
        final Map<String, Object> result = copy( structured );
        return McpResult.envelope( fits( result ) ? result : fallback( result, null ) );
    }

    /**
     * The MCP result for one page of {@code listKey} items, with {@code nextCursor} set for the item after the last one returned.
     * Trailing items are dropped until the result fits. The list must hold no {@code null} items: they are removed before counting,
     * so the cursor would move back.
     */
    public Map<String, Object> fitPage( final Map<String, ?> structured, final String listKey, final Page page, final long total )
    {
        return fitItems( structured, listKey, returned -> page.nextCursor( total, returned ) );
    }

    /**
     * {@link #fitPage(Map, String, Page, long)} for a page whose items do not map one to one to positions in the listing, such as the
     * matches found among the items scanned: {@code nextCursor} gives the cursor when the first {@code returned} items are kept.
     */
    public Map<String, Object> fitPage( final Map<String, ?> structured, final String listKey, final IntFunction<String> nextCursor )
    {
        return fitItems( structured, listKey, nextCursor );
    }

    /**
     * The MCP result for the whole list under {@code listKey}, for a tool that takes no cursor: trailing items are dropped until the
     * result fits, and the {@code hint} says how many. Unlike a page, an emptied list keeps the other fields when they fit.
     */
    public Map<String, Object> fitList( final Map<String, ?> structured, final String listKey )
    {
        return fitItems( structured, listKey, null );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fitItems( final Map<String, ?> structured, final String listKey, final IntFunction<String> nextCursor )
    {
        final Map<String, Object> result = copy( structured );
        final List<Object> items = result.get( listKey ) instanceof List<?> list ? (List<Object>) list : new ArrayList<>();
        result.put( listKey, items );
        putOrRemove( result, NEXT_CURSOR, nextCursor != null ? nextCursor.apply( items.size() ) : null );
        if ( fits( result ) )
        {
            return McpResult.envelope( result );
        }

        final Object first = items.isEmpty() ? null : items.get( 0 );
        int dropped = 0;
        int length = McpJson.length( result );
        while ( true )
        {
            int removed = 0;
            while ( !items.isEmpty() && removed < length - maxResultChars )
            {
                removed += McpJson.length( items.remove( items.size() - 1 ) ) + 1;
                dropped++;
            }
            if ( items.isEmpty() && nextCursor != null )
            {
                return McpResult.envelope( skipFirst( result, listKey, first, nextCursor ) );
            }
            putOrRemove( result, NEXT_CURSOR, nextCursor != null ? nextCursor.apply( items.size() ) : null );
            markTruncated( result, "dropped " + dropped + " trailing item(s) of '" + listKey + "'" );
            length = McpJson.length( result );
            if ( length <= maxResultChars )
            {
                return McpResult.envelope( result );
            }
            if ( items.isEmpty() )
            {
                break;
            }
        }
        return McpResult.envelope( fallback( result, listKey ) );
    }

    /**
     * An emptied page. When {@code first} alone does not fit even next to only project, branch and total, no page can ever return it,
     * so {@code nextCursor} moves past it instead of repeating the same page; the other fields stay when they fit. Otherwise the
     * other fields are what crowd it out, and skipping would lose items, so the result is the {@link #fallback}.
     */
    private Map<String, Object> skipFirst( final Map<String, Object> result, final String listKey, final Object first,
                                           final IntFunction<String> nextCursor )
    {
        final Map<String, Object> alone = new LinkedHashMap<>();
        KEPT_KEYS.stream().filter( result::containsKey ).forEach( key -> alone.put( key, result.get( key ) ) );
        alone.put( listKey, first == null ? List.of() : List.of( first ) );
        if ( first == null || fits( alone ) )
        {
            return fallback( result, listKey );
        }
        final String skip = nextCursor.apply( 1 );
        putOrRemove( result, NEXT_CURSOR, skip );
        result.put( TRUNCATED, true );
        result.put( HINT, "Result truncated to fit " + maxResultChars + " characters: the first item of '" + listKey + "' alone is " +
            "larger, so this page has no items. " + narrowHint + ( skip != null ? " Continue with nextCursor to skip that item." : "" ) );
        return fits( result ) ? result : fallback( result, listKey );
    }

    /**
     * The MCP result for {@code structured} with the value under {@code key} cut until it fits.
     */
    public Map<String, Object> fitSubtrees( final Map<String, ?> structured, final String key )
    {
        final Map<String, Object> result = copy( structured );
        if ( fits( result ) )
        {
            return McpResult.envelope( result );
        }

        final List<String> cuts = new ArrayList<>();
        while ( true )
        {
            markTruncated( result, describe( cuts ) );
            final int length = McpJson.length( result );
            if ( length <= maxResultChars )
            {
                return McpResult.envelope( result );
            }
            if ( !result.containsKey( key ) || !cut( new Slot( result, key ), key, length - maxResultChars, cuts ) )
            {
                return McpResult.envelope( fallback( result, null ) );
            }
        }
    }

    private Map<String, Object> copy( final Map<String, ?> structured )
    {
        return keepNullFields ? Nulls.keepingNullFields( structured ) : Nulls.clean( structured );
    }

    private boolean fits( final Map<String, Object> result )
    {
        return McpJson.length( result ) <= maxResultChars;
    }

    /**
     * Makes one cut in {@code slot} that removes about {@code excess} characters, top-down and measuring each child once: the largest
     * child is cut further when it alone suffices; else trailing list items are dropped when they suffice; else children are replaced
     * by markers largest-first when that suffices; else trailing items or fields are dropped.
     */
    private static boolean cut( final Slot slot, final String path, final int excess, final List<String> cuts )
    {
        final Object value = slot.get();
        final List<Slot> children = new ArrayList<>();
        final List<String> paths = new ArrayList<>();
        if ( value instanceof Map<?, ?> map )
        {
            map.keySet().forEach( key -> {
                children.add( new Slot( map, key ) );
                paths.add( path + "." + key );
            } );
        }
        else if ( value instanceof List<?> list )
        {
            for ( int i = 0; i < list.size(); i++ )
            {
                children.add( new Slot( list, i ) );
                paths.add( path + "[" + i + "]" );
            }
        }
        if ( children.isEmpty() )
        {
            final int length = McpJson.length( value );
            return length > markerLength( length ) && replace( slot, path, length, cuts );
        }

        final int[] lengths = new int[children.size()];
        final int[] savings = new int[children.size()];
        int largest = 0;
        long trailing = 0;
        long replaceable = 0;
        for ( int i = 0; i < savings.length; i++ )
        {
            lengths[i] = McpJson.length( children.get( i ).get() );
            savings[i] = lengths[i] - markerLength( lengths[i] );
            largest = savings[i] > savings[largest] ? i : largest;
            trailing += i > 0 ? lengths[i] + 1 : 0;
            replaceable += Math.max( 0, savings[i] );
        }

        if ( savings[largest] >= excess )
        {
            return cut( children.get( largest ), paths.get( largest ), excess, cuts );
        }
        if ( value instanceof List<?> && trailing >= excess )
        {
            return dropTrailing( value, path, excess, cuts );
        }
        if ( replaceable >= excess )
        {
            final List<Integer> order = new ArrayList<>();
            for ( int i = 0; i < savings.length; i++ )
            {
                order.add( i );
            }
            order.sort( ( left, right ) -> Integer.compare( savings[right], savings[left] ) );
            int saved = 0;
            for ( final int i : order )
            {
                if ( saved >= excess )
                {
                    break;
                }
                replace( children.get( i ), paths.get( i ), lengths[i], cuts );
                saved += savings[i];
            }
            return true;
        }
        if ( children.size() > 1 )
        {
            return dropTrailing( value, path, excess, cuts );
        }
        final int length = McpJson.length( value );
        return length > markerLength( length ) && replace( slot, path, length, cuts );
    }

    /**
     * Drops trailing list items or object fields, keeping the first one, until about {@code excess} characters are gone.
     */
    @SuppressWarnings("unchecked")
    private static boolean dropTrailing( final Object value, final String path, final int excess, final List<String> cuts )
    {
        int removed = 0;
        int dropped = 0;
        if ( value instanceof List<?> list )
        {
            final List<Object> items = (List<Object>) list;
            while ( items.size() > 1 && removed < excess )
            {
                removed += McpJson.length( items.remove( items.size() - 1 ) ) + 1;
                dropped++;
            }
            cuts.add( "dropped " + dropped + " trailing item(s) of '" + path + "'" );
        }
        else
        {
            final Map<Object, Object> fields = (Map<Object, Object>) value;
            final List<Object> keys = new ArrayList<>( fields.keySet() );
            for ( int i = keys.size() - 1; i > 0 && removed < excess; i-- )
            {
                removed += McpJson.length( fields.remove( keys.get( i ) ) ) + McpJson.length( String.valueOf( keys.get( i ) ) ) + 2;
                dropped++;
            }
            cuts.add( "dropped " + dropped + " trailing field(s) of '" + path + "'" );
        }
        return true;
    }

    private static boolean replace( final Slot slot, final String path, final int length, final List<String> cuts )
    {
        slot.set( marker( length ) );
        cuts.add( "replaced '" + path + "' (" + length + " chars)" );
        return true;
    }

    private Map<String, Object> fallback( final Map<String, Object> result, final String listKey )
    {
        final Map<String, Object> reduced = new LinkedHashMap<>();
        for ( final String key : KEPT_KEYS )
        {
            if ( result.containsKey( key ) )
            {
                reduced.put( key, result.get( key ) );
            }
        }
        if ( listKey != null )
        {
            reduced.put( listKey, List.of() );
        }
        reduced.put( TRUNCATED, true );
        reduced.put( HINT, "Result does not fit " + maxResultChars + " characters. " + narrowHint );
        return reduced;
    }

    private void markTruncated( final Map<String, Object> result, final String what )
    {
        result.put( TRUNCATED, true );
        result.put( HINT, "Result truncated to fit " + maxResultChars + " characters: " + what + ". " + narrowHint +
            ( result.get( NEXT_CURSOR ) != null ? " Continue with nextCursor." : "" ) );
    }

    private static String describe( final List<String> cuts )
    {
        final String listed = String.join( "; ", cuts.subList( 0, Math.min( cuts.size(), MAX_HINT_CUTS ) ) );
        return listed + ( cuts.size() > MAX_HINT_CUTS ? "; and " + ( cuts.size() - MAX_HINT_CUTS ) + " more" : "" );
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
}
