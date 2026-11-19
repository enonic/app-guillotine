package com.enonic.app.guillotine.mcp.tools;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.enonic.app.guillotine.mcp.shape.ContentKeys;
import com.enonic.app.guillotine.mcp.shape.McpArgs;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.security.PrincipalKey;

/**
 * Translates the arguments of the search tool into Guillotine query DSL variables ({@code QueryDSLInput}, {@code SortDslInput},
 * {@code AggregationInput}). Every value is validated here, so a malformed argument fails with a message about that argument.
 */
final class SearchQuery
{
    static final List<String> TEXT_FIELDS = List.of( "displayName", "_name", "_allText" );

    static final int MAX_TEXT_LENGTH = 500;

    static final int MAX_TYPES = 20;

    static final int MAX_FILTERS = 10;

    static final int MAX_AGGREGATES = 5;

    static final int MAX_BUCKETS = 100;

    static final int DEFAULT_BUCKETS = 10;

    /**
     * Guillotine fails on a date histogram without minDocCount.
     */
    static final int DATE_HISTOGRAM_MIN_DOC_COUNT = 1;

    static final int MAX_SORT_FIELDS = 3;

    static final List<String> NOT_READY_STATES = List.of( "IN_PROGRESS", "PENDING_APPROVAL", "REJECTED" );

    private static final Pattern FIELD = Pattern.compile( "[A-Za-z_][A-Za-z0-9_\\-]*(\\.[A-Za-z0-9_\\-]+)*" );

    private static final Pattern TYPE = Pattern.compile( "[A-Za-z0-9._\\-]+(:[A-Za-z0-9._\\-]+)?" );

    private static final Pattern INTERVAL = Pattern.compile( "[1-9][0-9]{0,3}[smhdwMy]" );

    private static final Pattern INSTANT =
        Pattern.compile( "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?(Z|[+-]\\d{2}:\\d{2})" );

    private static final Pattern DATE = Pattern.compile( "\\d{4}-\\d{2}-\\d{2}" );

    private static final Pattern SORT_ITEM = Pattern.compile( "\\s*(\\S+)(?:\\s+(\\S+))?\\s*" );

    /**
     * Index fields holding principal keys: Guillotine hides creator, modifier and owner from non-CMS callers, archivedBy is of the same
     * kind, and _permissions_* list who may do what.
     */
    private static final Set<String> PRINCIPAL_FIELDS = Set.of( "creator", "modifier", "owner", "archivedby" );

    private static final String PERMISSIONS_FIELD = "_permissions_read";

    private static final Set<String> DATE_FIELDS =
        Set.of( "createdtime", "modifiedtime", "archivedtime", "publish.from", "publish.to", "publish.first" );

    private static final String DATE_HINT = "Use ISO 8601, e.g. 2026-01-31 or 2026-01-31T12:00:00Z.";

    private static final Pattern FIELD_ROOT = Pattern.compile( "[.^]" );

    private static final Map<String, String> SORT_ALIASES =
        Map.of( "name", "_name", "path", "_path", "score", "_score", "id", "_id" );

    private static final Set<String> SORT_FIELDS =
        Set.of( "_score", "_name", "_path", "_id", "displayName", "type", "language", "createdTime", "modifiedTime", "publish.first",
                "publish.from", "publish.to", "workflow.state", "modifier", "owner", "creator" );

    private static final String SORT_FIELDS_HINT =
        "Use \"<field> [ASC|DESC]\" (up to " + MAX_SORT_FIELDS + ", comma-separated) with one of: displayName, name, path, type, " +
            "createdTime, modifiedTime, publish.from, publish.first, score, or a data field such as data.title " +
            "(call describe-type to see the fields of a type).";

    private static final String FIELD_HINT =
        "Use an indexed field name such as type, modifiedTime, publish.from or data.<field> (call describe-type to see data fields).";

    final Map<String, Object> query;

    final List<Map<String, Object>> sort;

    final List<Map<String, Object>> aggregations;

    final List<Aggregate> aggregates;

    record Aggregate(String name, String by, int size, String interval)
    {
    }

    private SearchQuery( final Map<String, Object> query, final List<Map<String, Object>> sort,
                         final List<Map<String, Object>> aggregations, final List<Aggregate> aggregates )
    {
        this.query = query;
        this.sort = sort;
        this.aggregations = aggregations;
        this.aggregates = aggregates;
    }

    /**
     * @param modifier          the key of the current user; only read when {@code mine} is true, which the caller has already checked
     * @param principalsAllowed whether the caller may see principal fields (creator, modifier, owner, ...); when not, filtering, sorting
     *                          and aggregating on them is rejected, since it would reveal the values hidden from hits
     */
    static SearchQuery from( final Map<String, ?> args, final PrincipalKey modifier, final boolean principalsAllowed )
    {
        final List<Map<String, Object>> must = new ArrayList<>();
        final List<Map<String, Object>> mustNot = new ArrayList<>();

        final String text = optionalString( args, "text" );
        if ( text != null && !text.isBlank() )
        {
            if ( text.length() > MAX_TEXT_LENGTH )
            {
                throw new ToolError( "Invalid text: longer than " + MAX_TEXT_LENGTH + " characters.", "Pass a few keywords." );
            }
            must.add( Map.of( "fulltext", map( "fields", TEXT_FIELDS, "query", text, "operator", "AND" ) ) );
        }

        final Map<String, Object> types = types( args.get( "types" ) );
        if ( types != null )
        {
            must.add( types );
        }

        path( optionalString( args, "path" ), must, mustNot );

        if ( Boolean.TRUE.equals( args.get( "mine" ) ) )
        {
            must.add( term( "modifier", stringValue( modifier.toString() ) ) );
        }
        else if ( args.get( "mine" ) != null && !( args.get( "mine" ) instanceof Boolean ) )
        {
            throw new ToolError( "Invalid mine: " + McpJson.write( args.get( "mine" ) ) + ".", "Pass true or false." );
        }

        workflow( args.get( "workflow" ), must, mustNot );
        filters( args.get( "filters" ), must, principalsAllowed );

        final List<Map<String, Object>> sort =
            sort( optionalString( args, "sort" ), text != null && !text.isBlank(), principalsAllowed );
        final List<Aggregate> aggregates = aggregates( args.get( "aggregate" ), principalsAllowed );

        if ( must.isEmpty() )
        {
            must.add( Map.of( "matchAll", Map.of() ) );
        }
        final Map<String, Object> bool = new LinkedHashMap<>();
        bool.put( "must", must );
        if ( !mustNot.isEmpty() )
        {
            bool.put( "mustNot", mustNot );
        }

        return new SearchQuery( Map.of( "boolean", bool ), sort, aggregations( aggregates ), aggregates );
    }

    private static Map<String, Object> types( final Object value )
    {
        if ( value == null )
        {
            return null;
        }
        if ( !( value instanceof List<?> list ) || list.size() > MAX_TYPES )
        {
            throw new ToolError( "Invalid types: expected a list of up to " + MAX_TYPES + " content type names.",
                                 "Call list-types to see the types in the project." );
        }
        final List<String> fullNames = new ArrayList<>();
        final List<Map<String, Object>> clauses = new ArrayList<>();
        for ( final Object item : list )
        {
            if ( !( item instanceof String type ) || !TYPE.matcher( type ).matches() )
            {
                throw new ToolError( "Invalid type: " + McpJson.write( item ) + ".",
                                     "Use a name from list-types, e.g. \"com.example.app:article\" or the short form \"article\"." );
            }
            if ( type.contains( ":" ) )
            {
                fullNames.add( type );
            }
            else
            {
                clauses.add( Map.of( "like", map( "field", "type", "value", "*:" + type ) ) );
            }
        }
        if ( list.isEmpty() )
        {
            return null;
        }
        if ( !fullNames.isEmpty() )
        {
            clauses.add( 0, Map.of( "in", map( "field", "type", "stringValues", fullNames ) ) );
        }
        return clauses.size() == 1 ? clauses.get( 0 ) : Map.of( "boolean", Map.of( "should", clauses ) );
    }

    private static void path( final String path, final List<Map<String, Object>> must, final List<Map<String, Object>> mustNot )
    {
        final String normalized = ContentKeys.scopePath( path );
        if ( normalized == null )
        {
            return;
        }
        final String nodePath = "/content" + normalized;
        final int elements = nodePath.substring( 1 ).split( "/" ).length;
        must.add( Map.of( "pathMatch", map( "field", "_path", "path", nodePath, "minimumMatch", elements ) ) );
        mustNot.add( term( "_path", stringValue( nodePath ) ) );
    }

    private static void workflow( final Object value, final List<Map<String, Object>> must, final List<Map<String, Object>> mustNot )
    {
        if ( value == null || "".equals( value ) )
        {
            return;
        }
        if ( "IN_PROGRESS".equals( value ) )
        {
            must.add( term( "workflow.state", stringValue( "IN_PROGRESS" ) ) );
        }
        else if ( "READY".equals( value ) )
        {
            mustNot.add( Map.of( "in", map( "field", "workflow.state", "stringValues", NOT_READY_STATES ) ) );
        }
        else
        {
            throw new ToolError( "Invalid workflow: " + McpJson.write( value ) + ".",
                                 "Use \"IN_PROGRESS\" (work in progress) or \"READY\" (ready to publish), or omit workflow." );
        }
    }

    private static void filters( final Object value, final List<Map<String, Object>> must, final boolean principalsAllowed )
    {
        if ( value == null )
        {
            return;
        }
        if ( !( value instanceof List<?> list ) || list.size() > MAX_FILTERS )
        {
            throw new ToolError(
                "Invalid filters: expected a list of up to " + MAX_FILTERS + " objects {field, equals} or {field, from, to}.", FIELD_HINT );
        }
        for ( int i = 0; i < list.size(); i++ )
        {
            final String where = "filters[" + i + "]";
            if ( !( list.get( i ) instanceof Map<?, ?> filter ) )
            {
                throw new ToolError( "Invalid " + where + ": expected an object {field, equals} or {field, from, to}.", FIELD_HINT );
            }
            final String field = field( filter.get( "field" ), where + ".field", principalsAllowed );
            final Object equals = filter.get( "equals" );
            final Object from = filter.get( "from" );
            final Object to = filter.get( "to" );
            if ( equals != null && ( from != null || to != null ) || equals == null && from == null && to == null )
            {
                throw new ToolError( "Invalid " + where + ": pass either equals, or from and/or to.",
                                     "Example: {\"field\": \"modifiedTime\", \"from\": \"2026-01-01\"}." );
            }
            if ( DATE_FIELDS.contains( field.toLowerCase( Locale.ROOT ) ) )
            {
                requireDate( field, equals, where + ".equals" );
                requireDate( field, from, where + ".from" );
                requireDate( field, to, where + ".to" );
            }
            if ( equals != null )
            {
                must.add( term( field, value( equals, where + ".equals" ) ) );
                continue;
            }
            final Map<String, Object> range = new LinkedHashMap<>();
            range.put( "field", field );
            Map<String, Object> fromValue = from != null ? value( from, where + ".from" ) : null;
            Map<String, Object> toValue = to != null ? value( to, where + ".to" ) : null;
            if ( fromValue != null && toValue != null && !fromValue.keySet().equals( toValue.keySet() ) )
            {
                if ( !isNumber( fromValue ) || !isNumber( toValue ) )
                {
                    throw new ToolError( "Invalid " + where + ": from and to must be of the same kind.",
                                         "Pass two dates, two numbers or two strings." );
                }
                fromValue = asDouble( fromValue );
                toValue = asDouble( toValue );
            }
            if ( fromValue != null )
            {
                range.put( "gte", fromValue );
            }
            if ( toValue != null )
            {
                range.put( "lt", toValue );
            }
            must.add( Map.of( "range", range ) );
        }
    }

    private static List<Map<String, Object>> sort( final String value, final boolean hasText, final boolean principalsAllowed )
    {
        final List<Map<String, Object>> result = new ArrayList<>();
        if ( value == null || value.isBlank() )
        {
            result.add( sortItem( hasText ? "_score" : "modifiedTime", "DESC" ) );
        }
        else
        {
            final String[] items = value.split( "," );
            if ( items.length > MAX_SORT_FIELDS )
            {
                throw new ToolError( "Invalid sort: at most " + MAX_SORT_FIELDS + " fields.", SORT_FIELDS_HINT );
            }
            for ( final String item : items )
            {
                final Matcher matcher = SORT_ITEM.matcher( item );
                if ( !matcher.matches() )
                {
                    throw new ToolError( "Invalid sort: " + McpJson.write( value ) + ".", SORT_FIELDS_HINT );
                }
                final String requested = matcher.group( 1 );
                final String field = SORT_ALIASES.getOrDefault( requested, requested );
                checkPrincipal( field, "sort", principalsAllowed );
                if ( !SORT_FIELDS.contains( field ) &&
                    !( ( field.startsWith( "data." ) || field.startsWith( "x." ) ) && FIELD.matcher( field ).matches() ) )
                {
                    throw new ToolError( "Invalid sort field: " + McpJson.write( requested ) + ".", SORT_FIELDS_HINT );
                }
                final String direction = matcher.group( 2 ) == null ? "ASC" : matcher.group( 2 ).toUpperCase( Locale.ROOT );
                if ( !"ASC".equals( direction ) && !"DESC".equals( direction ) )
                {
                    throw new ToolError( "Invalid sort direction: " + McpJson.write( matcher.group( 2 ) ) + ".",
                                         "Use ASC or DESC after the field name, e.g. \"modifiedTime DESC\"." );
                }
                result.add( sortItem( field, direction ) );
            }
        }
        // a unique last key keeps pages stable when the requested keys tie
        if ( result.stream().noneMatch( item -> "_path".equals( item.get( "field" ) ) ) )
        {
            result.add( sortItem( "_path", "ASC" ) );
        }
        return result;
    }

    private static List<Aggregate> aggregates( final Object value, final boolean principalsAllowed )
    {
        final List<Aggregate> result = new ArrayList<>();
        if ( value == null )
        {
            return result;
        }
        if ( !( value instanceof List<?> list ) || list.size() > MAX_AGGREGATES )
        {
            throw new ToolError( "Invalid aggregate: expected a list of up to " + MAX_AGGREGATES + " objects {by, size?, interval?}.",
                                 FIELD_HINT );
        }
        for ( int i = 0; i < list.size(); i++ )
        {
            final String where = "aggregate[" + i + "]";
            if ( !( list.get( i ) instanceof Map<?, ?> item ) )
            {
                throw new ToolError( "Invalid " + where + ": expected an object {by, size?, interval?}.", FIELD_HINT );
            }
            final String by = field( item.get( "by" ), where + ".by", principalsAllowed );
            final Object interval = item.get( "interval" );
            if ( interval != null && !( interval instanceof String text && INTERVAL.matcher( text ).matches() ) )
            {
                throw new ToolError( "Invalid " + where + ".interval: " + McpJson.write( interval ) + ".",
                                     "Use a number and a unit: s, m, h, d, w, M (month) or y, e.g. \"1M\" or \"7d\"." );
            }
            final int size = interval == null ? size( item.get( "size" ), where + ".size" ) : 0;
            result.add( new Aggregate( "a" + i, by, size, (String) interval ) );
        }
        return result;
    }

    private static List<Map<String, Object>> aggregations( final List<Aggregate> aggregates )
    {
        return aggregates.isEmpty() ? null : aggregates.stream().map( SearchQuery::aggregation ).toList();
    }

    private static Map<String, Object> aggregation( final Aggregate aggregate )
    {
        if ( aggregate.interval() != null )
        {
            return map( "name", aggregate.name(), "dateHistogram",
                        map( "field", aggregate.by(), "interval", aggregate.interval(), "minDocCount",
                             DATE_HISTOGRAM_MIN_DOC_COUNT ) );
        }
        return map( "name", aggregate.name(), "terms",
                    map( "field", aggregate.by(), "size", aggregate.size(), "order", "_count DESC" ) );
    }

    static int size( final Object value, final String where )
    {
        if ( value == null )
        {
            return DEFAULT_BUCKETS;
        }
        final Integer size = McpArgs.toInt( value );
        if ( size == null || size < 1 || size > MAX_BUCKETS )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.write( value ) + ".",
                                 "Use an integer from 1 to " + MAX_BUCKETS + " (default " + DEFAULT_BUCKETS + ")." );
        }
        return size;
    }

    private static String field( final Object value, final String where, final boolean principalsAllowed )
    {
        if ( !( value instanceof String field ) || !FIELD.matcher( field ).matches() )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.write( value ) + ".", FIELD_HINT );
        }
        checkPrincipal( field, where, principalsAllowed );
        return field;
    }

    /**
     * Whether {@code field} reads a principal field or one of its subfields ({@code modifier._orderby}); a boost suffix
     * ({@code owner^2}) and wildcards in the first path element ({@code mod*}) are taken into account.
     */
    static boolean isPrincipalField( final String field )
    {
        final String name = FIELD_ROOT.split( field.strip().toLowerCase( Locale.ROOT ), 2 )[0];
        if ( name.contains( "*" ) || name.contains( "?" ) )
        {
            final Pattern glob = Pattern.compile( Pattern.quote( name ).replace( "*", "\\E.*\\Q" ).replace( "?", "\\E.\\Q" ) );
            return PRINCIPAL_FIELDS.stream().anyMatch( principal -> glob.matcher( principal ).matches() ) ||
                glob.matcher( PERMISSIONS_FIELD ).matches();
        }
        return PRINCIPAL_FIELDS.contains( name ) || name.startsWith( "_permissions" );
    }

    private static void checkPrincipal( final String field, final String where, final boolean principalsAllowed )
    {
        checkPrincipal( field, where, principalsAllowed, "Use mine to find your own changes, or call the tool as a CMS user." );
    }

    static void checkPrincipal( final String field, final String where, final boolean principalsAllowed, final String hint )
    {
        if ( !principalsAllowed && isPrincipalField( field ) )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.preview( field ) +
                                     " holds user keys, which the current caller may not see.", hint );
        }
    }

    /**
     * A {@code DSLExpressionValueInput}: ISO date-times become {@code instant}, plain dates the start of that day in UTC.
     */
    private static Map<String, Object> value( final Object value, final String where )
    {
        if ( value instanceof Boolean bool )
        {
            return Map.of( "boolean", bool );
        }
        if ( value instanceof Integer || value instanceof Long || value instanceof BigInteger )
        {
            final Integer exact = McpArgs.toInt( value );
            return exact != null ? Map.of( "long", exact ) : Map.of( "double", Double.parseDouble( value.toString() ) );
        }
        if ( value instanceof Double || value instanceof Float || value instanceof BigDecimal )
        {
            return Map.of( "double", ( (Number) value ).doubleValue() );
        }
        if ( value instanceof String text )
        {
            if ( INSTANT.matcher( text ).matches() )
            {
                parseDate( text, where, () -> OffsetDateTime.parse( text ) );
                return Map.of( "instant", text );
            }
            if ( DATE.matcher( text ).matches() )
            {
                parseDate( text, where, () -> LocalDate.parse( text ) );
                return Map.of( "instant", text + "T00:00:00Z" );
            }
            return stringValue( text );
        }
        throw new ToolError( "Invalid " + where + ": " + McpJson.write( value ) + ".",
                             "Use a string, a number, a boolean or an ISO date such as 2026-01-31." );
    }

    private static void requireDate( final String field, final Object value, final String where )
    {
        if ( value != null && !( value instanceof String text && ( INSTANT.matcher( text ).matches() || DATE.matcher( text ).matches() ) ) )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.preview( value ) + " is not an ISO 8601 date, and " + field +
                                     " is a date field.", DATE_HINT );
        }
    }

    private static boolean isNumber( final Map<String, Object> value )
    {
        return value.containsKey( "long" ) || value.containsKey( "double" );
    }

    private static Map<String, Object> asDouble( final Map<String, Object> value )
    {
        return Map.of( "double", ( (Number) value.getOrDefault( "long", value.get( "double" ) ) ).doubleValue() );
    }

    private static void parseDate( final String text, final String where, final Supplier<Object> parser )
    {
        try
        {
            parser.get();
        }
        catch ( DateTimeParseException e )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.write( text ) + " is not a valid date.",
                                 DATE_HINT );
        }
    }

    private static String optionalString( final Map<String, ?> args, final String name )
    {
        final Object value = args.get( name );
        if ( value == null || value instanceof String )
        {
            return (String) value;
        }
        throw new ToolError( "Invalid " + name + ": expected a string, got " + McpJson.write( value ) + ".", null );
    }

    private static Map<String, Object> term( final String field, final Map<String, Object> value )
    {
        return Map.of( "term", map( "field", field, "value", value ) );
    }

    private static Map<String, Object> stringValue( final String value )
    {
        return Map.of( "string", value );
    }

    private static Map<String, Object> sortItem( final String field, final String direction )
    {
        return map( "field", field, "direction", direction );
    }

    private static Map<String, Object> map( final Object... keyValues )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        for ( int i = 0; i < keyValues.length; i += 2 )
        {
            result.put( (String) keyValues[i], keyValues[i + 1] );
        }
        return result;
    }
}
