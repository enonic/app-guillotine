package com.enonic.app.guillotine.mcp.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.security.PrincipalKey;

/**
 * The arguments of {@code search} as Guillotine query DSL variables ({@code QueryDSLInput}, {@code SortDslInput},
 * {@code AggregationInput}). The input schema already fixes the argument types and bounds; this class checks what it cannot: field
 * names against the data fields in scope, dates, and which filter operators go together.
 */
final class SearchQuery
{
    static final List<String> TEXT_FIELDS = List.of( "displayName", "_name", "_allText" );

    static final int DEFAULT_BUCKETS = 10;

    static final int MAX_SORT_FIELDS = 3;

    static final String REFERENCES_FIELD = "_references";

    static final List<String> NOT_READY_STATES = List.of( "IN_PROGRESS", "PENDING_APPROVAL", "REJECTED" );

    private static final Pattern INSTANT =
        Pattern.compile( "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?(Z|[+-]\\d{2}:\\d{2})" );

    private static final Pattern DATE = Pattern.compile( "\\d{4}-\\d{2}-\\d{2}" );

    private static final Pattern SORT_ITEM = Pattern.compile( "\\s*(\\S+)(?:\\s+(\\S+))?\\s*" );

    private static final Set<String> DATE_FIELDS =
        Set.of( "createdtime", "modifiedtime", "archivedtime", "publish.from", "publish.to", "publish.first" );

    /**
     * Names in {@code fields} that every hit carries anyway, lower case.
     */
    private static final Set<String> HIT_FIELDS = Set.of( "_id", "_path", "displayname", "type", "modifiedtime", "workflow",
                                                          "workflow.state" );

    private static final Set<String> PUBLISH_FIELDS = Set.of( "publish", "publish.from", "publish.to", "publish.first" );

    private static final Map<String, String> SORT_ALIASES = Map.of( "name", "_name", "path", "_path", "score", "_score" );

    private static final Set<String> SORT_FIELDS =
        Set.of( "_score", "_name", "_path", "displayName", "type", "language", "createdTime", "modifiedTime", "publish.first",
                "publish.from", "publish.to", "workflow.state", "modifier", "owner", "creator" );

    private static final String SORT_HINT = "Use \"<field> [ASC|DESC]\", up to " + MAX_SORT_FIELDS + " comma-separated, with one of " +
        "displayName, name, path, type, createdTime, modifiedTime, publish.from, publish.first, score, or a data field such as " +
        "data.title.";

    private static final String DATE_HINT = "Use ISO 8601, such as 2026-01-31 or 2026-01-31T12:00:00Z.";

    private static final String FILTER_HINT = "Each filter is one of {field, equals}, {field, from, to}, {field, exists} or " +
        "{references, field?}.";

    final Map<String, Object> query;

    final List<Map<String, Object>> sort;

    final List<Map<String, Object>> aggregations;

    final List<Aggregate> aggregates;

    final List<String> fields;

    /**
     * Whether {@code fields} asked for the publish dates.
     */
    final boolean publish;

    /**
     * Index field names in {@code fields} that hits do not carry, as given.
     */
    final List<String> notReturned;

    record Aggregate(String name, String by, int size)
    {
    }

    /**
     * What the query needs from outside the arguments.
     *
     * @param caller the signed-in caller, {@code null} when anonymous
     * @param types  the clause for the {@code types} argument, {@code null} when absent
     * @param scope  the data fields of the types searched
     * @param idOf   the id of the content at a path, {@code null} when it is missing or not readable
     */
    record Inputs(PrincipalKey caller, Map<String, Object> types, DataFieldScope scope, Function<String, String> idOf)
    {
    }

    private SearchQuery( final Map<String, Object> query, final List<Map<String, Object>> sort, final List<Aggregate> aggregates,
                         final List<String> fields, final boolean publish, final List<String> notReturned )
    {
        this.publish = publish;
        this.notReturned = notReturned;
        this.query = query;
        this.sort = sort;
        this.aggregates = aggregates;
        this.aggregations = aggregates.isEmpty() ? null : aggregates.stream().map( SearchQuery::aggregation ).toList();
        this.fields = fields;
    }

    static SearchQuery from( final Map<String, ?> args, final Inputs inputs )
    {
        final List<Map<String, Object>> must = new ArrayList<>();
        final List<Map<String, Object>> mustNot = new ArrayList<>();
        final DataFieldScope scope = inputs.scope();

        final String text = args.get( "text" ) instanceof String value && !value.isBlank() ? value : null;
        if ( text != null )
        {
            must.add( Map.of( "fulltext", map( "fields", TEXT_FIELDS, "query", text, "operator", "AND" ) ) );
        }
        if ( inputs.types() != null )
        {
            must.add( inputs.types() );
        }
        final String path = ContentKeys.scopePath( args.get( "path" ) instanceof String value ? value : null );
        if ( path != null )
        {
            must.add( pathMatch( path ) );
        }
        if ( Boolean.TRUE.equals( args.get( "mine" ) ) )
        {
            if ( inputs.caller() == null )
            {
                throw new ToolError( "mine needs a signed-in caller, and this call is anonymous.",
                                     "Ask the user to sign in to Enonic XP, or omit mine." );
            }
            must.add( term( "modifier", inputs.caller().toString() ) );
        }
        if ( "IN_PROGRESS".equals( args.get( "workflow" ) ) )
        {
            must.add( term( "workflow.state", "IN_PROGRESS" ) );
        }
        else if ( "READY".equals( args.get( "workflow" ) ) )
        {
            mustNot.add( Map.of( "in", map( "field", "workflow.state", "stringValues", NOT_READY_STATES ) ) );
        }
        filters( list( args.get( "filters" ) ), must, mustNot, inputs );

        final boolean anonymous = inputs.caller() == null;
        final List<Map<String, Object>> sort =
            sort( args.get( "sort" ) instanceof String value ? value : null, text != null, scope, anonymous );
        final List<Aggregate> aggregates = aggregates( list( args.get( "aggregate" ) ), scope, anonymous );
        final Set<String> fields = new LinkedHashSet<>();
        final Set<String> notReturned = new LinkedHashSet<>();
        boolean publish = false;
        for ( final Object item : list( args.get( "fields" ) ) )
        {
            final String field = String.valueOf( item ).strip();
            final String lower = field.toLowerCase( Locale.ROOT );
            final String dataPath = field.startsWith( DataFieldScope.DATA_PREFIX ) ? null : scope.dataFieldPath( field );
            if ( dataPath != null )
            {
                fields.add( dataPath );
            }
            else if ( PUBLISH_FIELDS.contains( lower ) )
            {
                publish = true;
            }
            else if ( !HIT_FIELDS.contains( lower ) )
            {
                if ( DataFieldScope.isIndexField( field ) && !field.startsWith( DataFieldScope.DATA_PREFIX ) )
                {
                    notReturned.add( field );
                }
                else
                {
                    fields.add( scope.resolveDataField( field, "fields" ) );
                }
            }
        }

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
        return new SearchQuery( Map.of( "boolean", bool ), sort, aggregates, List.copyOf( fields ), publish, List.copyOf( notReturned ) );
    }

    /**
     * The clause for the {@code types} argument: full names as given, matched against registered spellings by {@code spelling}, and
     * local names such as {@code article} in any application. {@code null} for no types.
     */
    static Map<String, Object> types( final List<?> requested, final Function<String, String> spelling )
    {
        return types( requested, spelling, "stringValues" );
    }

    /**
     * The clause that matches {@code path}, a scope path from {@link ContentKeys#scopePath}, and everything below it.
     */
    static Map<String, Object> pathMatch( final String path )
    {
        return Map.of( "pathMatch", map( "field", "_path", "path", "/content" + path, "minimumMatch",
                                         path.substring( 1 ).split( "/" ).length + 1 ) );
    }

    /**
     * {@link #types(List, Function)} with the values of its {@code in} clause under {@code valuesKey}: {@code stringValues} in
     * Guillotine's query DSL input, {@code values} in XP's query DSL.
     */
    static Map<String, Object> types( final List<?> requested, final Function<String, String> spelling, final String valuesKey )
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
                clauses.add( Map.of( "like", map( "field", "type", "value", "*:" + type ) ) );
            }
        }
        if ( !fullNames.isEmpty() )
        {
            clauses.add( 0, Map.of( "in", map( "field", "type", valuesKey, fullNames ) ) );
        }
        if ( clauses.isEmpty() )
        {
            return null;
        }
        return clauses.size() == 1 ? clauses.get( 0 ) : Map.of( "boolean", Map.of( "should", clauses ) );
    }

    private static void filters( final List<?> filters, final List<Map<String, Object>> must, final List<Map<String, Object>> mustNot,
                                 final Inputs inputs )
    {
        for ( int i = 0; i < filters.size(); i++ )
        {
            final String where = "filters[" + i + "]";
            final Map<?, ?> filter = filters.get( i ) instanceof Map<?, ?> map ? map : Map.of();
            final Object equals = filter.get( "equals" );
            final Object from = filter.get( "from" );
            final Object to = filter.get( "to" );
            final Object exists = filter.get( "exists" );
            final Object references = filter.get( "references" );
            final int operators = ( equals != null ? 1 : 0 ) + ( from != null || to != null ? 1 : 0 ) + ( exists != null ? 1 : 0 ) +
                ( references != null ? 1 : 0 );
            if ( operators != 1 )
            {
                throw new ToolError( "Invalid " + where + ": pass exactly one of equals, from/to, exists or references.", FILTER_HINT );
            }
            final Object rawField = filter.get( "field" );
            if ( references != null )
            {
                must.add( references( references, rawField, where, inputs ) );
                continue;
            }
            if ( !( rawField instanceof String name ) || name.isBlank() )
            {
                throw new ToolError( "Invalid " + where + ": field is required.", FILTER_HINT );
            }
            final String field = inputs.scope().resolve( name.strip(), where + ".field" );
            PrincipalFields.requireAllowed( field, where + ".field", inputs.caller() == null );
            if ( exists != null )
            {
                ( Boolean.TRUE.equals( exists ) ? must : mustNot ).add( Map.of( "exists", Map.of( "field", field ) ) );
                continue;
            }
            final boolean dateField = DATE_FIELDS.contains( field.toLowerCase( Locale.ROOT ) );
            if ( equals != null && dateField )
            {
                // a stored date field holds instants, so equality means the whole day
                if ( !( equals instanceof String day && DATE.matcher( day ).matches() ) )
                {
                    throw new ToolError( "Invalid " + where + ".equals: " + field + " is a date field; equals takes a day such as " +
                                             "2026-01-31.", "Use from and to for a time range." );
                }
                final Map<String, Object> range = new LinkedHashMap<>();
                range.put( "field", field );
                range.put( "gte", value( equals, where + ".equals", field ) );
                range.put( "lt", Map.of( "instant", LocalDate.parse( (String) equals ).plusDays( 1 ) + "T00:00:00Z" ) );
                must.add( Map.of( "range", range ) );
                continue;
            }
            if ( equals != null )
            {
                final boolean plainDate = equals instanceof String date && DATE.matcher( date ).matches();
                // XP also indexes date values as text, so a plain date in a data field matches as text
                must.add( Map.of( "term", map( "field", field, "value", plainDate ? Map.of( "string", equals )
                    : value( equals, where + ".equals", null ) ) ) );
                continue;
            }
            Map<String, Object> lower = from != null ? value( from, where + ".from", dateField ? field : null ) : null;
            Map<String, Object> upper = to != null ? value( to, where + ".to", dateField ? field : null ) : null;
            if ( lower != null && upper != null && !lower.keySet().equals( upper.keySet() ) )
            {
                if ( !isNumber( lower ) || !isNumber( upper ) )
                {
                    throw new ToolError( "Invalid " + where + ": from and to must be of the same kind.",
                                         "Pass two dates, two numbers or two strings." );
                }
                lower = asDouble( lower );
                upper = asDouble( upper );
            }
            final Map<String, Object> range = new LinkedHashMap<>();
            range.put( "field", field );
            if ( lower != null )
            {
                range.put( "gte", lower );
            }
            if ( upper != null )
            {
                range.put( "lt", upper );
            }
            must.add( Map.of( "range", range ) );
        }
    }

    /**
     * A {@code references} filter: content whose {@code field}, or any reference when no field is given, holds the id of the content
     * at the key. A path is resolved to its id here, so the model does not pass ids around.
     */
    private static Map<String, Object> references( final Object value, final Object rawField, final String where, final Inputs inputs )
    {
        final boolean anyField = !( rawField instanceof String name && !name.isBlank() && !REFERENCES_FIELD.equals( name.strip() ) );
        final String field = anyField ? REFERENCES_FIELD : inputs.scope().resolve( ( (String) rawField ).strip(), where + ".field" );
        PrincipalFields.requireAllowed( field, where + ".field", inputs.caller() == null );
        final String key = ContentKeys.key( value, where + ".references" );
        final String id = ContentKeys.isPath( key ) ? inputs.idOf().apply( key ) : key;
        if ( id == null )
        {
            throw new ToolError( "Invalid " + where + ".references: no content at " + McpJson.preview( key ) +
                                     " that the caller can read.", "Find the item with search and pass its _path or _id." );
        }
        if ( !anyField )
        {
            if ( inputs.scope().isDataField( field ) && !inputs.scope().isReference( field ) )
            {
                throw new ToolError( "Invalid " + where + ".field: " + field + " does not hold references.",
                                     "Pass a content, image or media selector field, or omit field to match any reference." );
            }
            if ( inputs.scope().isDataField( field ) && !inputs.scope().isReferenceEverywhere( field ) )
            {
                // other types hold plain values under the same name, which could equal the id
                return Map.of( "boolean", Map.of( "must", List.of( term( field, id ), Map.of( "in", map( "field", "type", "stringValues",
                                                                                                          inputs.scope()
                                                                                                              .referenceTypes( field ) ) ) ) ) );
            }
        }
        return term( field, id );
    }

    private static List<Map<String, Object>> sort( final String value, final boolean hasText, final DataFieldScope scope,
                                                   final boolean anonymous )
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
                throw new ToolError( "Invalid sort: at most " + MAX_SORT_FIELDS + " fields.", SORT_HINT );
            }
            for ( final String item : items )
            {
                final Matcher matcher = SORT_ITEM.matcher( item );
                if ( !matcher.matches() )
                {
                    throw new ToolError( "Invalid sort: " + McpJson.preview( value ) + ".", SORT_HINT );
                }
                final String requested = matcher.group( 1 );
                final String field = scope.resolve( SORT_ALIASES.getOrDefault( requested, requested ), "sort" );
                PrincipalFields.requireAllowed( field, "sort", anonymous );
                if ( !SORT_FIELDS.contains( field ) && !field.startsWith( DataFieldScope.DATA_PREFIX ) && !field.startsWith( "x." ) )
                {
                    throw new ToolError( "Invalid sort field: " + McpJson.preview( requested ) + ".", SORT_HINT );
                }
                final String direction = matcher.group( 2 ) == null ? "ASC" : matcher.group( 2 ).toUpperCase( Locale.ROOT );
                if ( !"ASC".equals( direction ) && !"DESC".equals( direction ) )
                {
                    throw new ToolError( "Invalid sort direction: " + McpJson.preview( matcher.group( 2 ) ) + ".",
                                         "Use ASC or DESC after the field name, such as \"modifiedTime DESC\"." );
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

    private static List<Aggregate> aggregates( final List<?> requested, final DataFieldScope scope, final boolean anonymous )
    {
        final List<Aggregate> result = new ArrayList<>();
        for ( int i = 0; i < requested.size(); i++ )
        {
            final Map<?, ?> item = requested.get( i ) instanceof Map<?, ?> map ? map : Map.of();
            final String where = "aggregate[" + i + "]";
            if ( !( item.get( "by" ) instanceof String by ) || by.isBlank() )
            {
                throw new ToolError( "Invalid " + where + ": by is required.", "Name the field to group by, such as data.author." );
            }
            final int size = item.get( "size" ) instanceof Number number ? number.intValue() : DEFAULT_BUCKETS;
            final String field = scope.resolve( by.strip(), where + ".by" );
            PrincipalFields.requireAllowed( field, where + ".by", anonymous );
            result.add( new Aggregate( "a" + i, field, size ) );
        }
        return result;
    }

    private static Map<String, Object> aggregation( final Aggregate aggregate )
    {
        return map( "name", aggregate.name(), "terms", map( "field", aggregate.by(), "size", aggregate.size(), "order", "_count DESC" ) );
    }

    /**
     * A {@code DSLExpressionValueInput}: ISO date-times become {@code instant}, plain dates the start of that day in UTC; integers
     * outside the int range become {@code double}. {@code dateField} names the date field the value is for, which then must be a date.
     */
    private static Map<String, Object> value( final Object value, final String where, final String dateField )
    {
        if ( dateField != null &&
            !( value instanceof String text && ( INSTANT.matcher( text ).matches() || DATE.matcher( text ).matches() ) ) )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.preview( value ) + " is not an ISO 8601 date, and " + dateField +
                                     " is a date field.", DATE_HINT );
        }
        if ( value instanceof Boolean bool )
        {
            return Map.of( "boolean", bool );
        }
        if ( value instanceof Number number )
        {
            try
            {
                return Map.of( "long", new BigDecimal( number.toString() ).intValueExact() );
            }
            catch ( ArithmeticException | NumberFormatException e )
            {
                return Map.of( "double", number.doubleValue() );
            }
        }
        final String text = String.valueOf( value );
        if ( INSTANT.matcher( text ).matches() )
        {
            requireValidDate( text, where, true );
            return Map.of( "instant", text );
        }
        if ( DATE.matcher( text ).matches() )
        {
            requireValidDate( text, where, false );
            return Map.of( "instant", text + "T00:00:00Z" );
        }
        return Map.of( "string", text );
    }

    private static void requireValidDate( final String text, final String where, final boolean withTime )
    {
        try
        {
            if ( withTime )
            {
                OffsetDateTime.parse( text );
            }
            else
            {
                LocalDate.parse( text );
            }
        }
        catch ( DateTimeParseException e )
        {
            throw new ToolError( "Invalid " + where + ": " + McpJson.preview( text ) + " is not a valid date.", DATE_HINT );
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

    private static List<?> list( final Object value )
    {
        return value instanceof List<?> list ? list : List.of();
    }

    private static Map<String, Object> term( final String field, final String value )
    {
        return Map.of( "term", map( "field", field, "value", Map.of( "string", value ) ) );
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
