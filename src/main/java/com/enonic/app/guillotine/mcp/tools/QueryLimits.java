package com.enonic.app.guillotine.mcp.tools;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.Instrumentation;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.Node;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.language.Value;
import graphql.language.VariableReference;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.InputValueWithState;

import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;
import com.enonic.app.guillotine.mcp.shape.Cursors;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ToolError;

/**
 * Limits of the {@code query} tool, checked on the document before it runs, except depth, which an instrumentation checks after
 * validation.
 * <p>
 * The cost bound counts what the backend does, not how many fields are selected. A field reads content rows when it takes
 * {@code first} and either takes {@code sort} (queryDsl, getChildren, children and their connections) or lists content (a repeated
 * content, image or media selector, which loads each referenced item); it reads its page size of rows, once per item of every list
 * around it. Every field that takes {@code first} multiplies what is below it by its page size, so a repeated item set counts once
 * per item for the selectors inside it; a list of plain values, such as a repeated text field, has nothing below it and costs
 * nothing. Each aggregation counts its buckets, multiplied by the buckets of the aggregations around it and by the items of the
 * lists around the field that runs it. Every fragment spread counts.
 */
final class QueryLimits
{
    static final int MAX_DEPTH = 10;

    static final int MAX_FIRST = 100;

    static final int MAX_AGGREGATION_SIZE = 100;

    static final long MAX_ROWS = 2_000;

    static final long MAX_BUCKETS = 1_000;

    /**
     * What Guillotine's index lists return when {@code first} is not passed.
     */
    static final int DEFAULT_FIRST = 10;

    /**
     * What a repeated form item without {@code first} counts. It returns every stored value, a number the query does not show, so this
     * is an estimate: the most {@code first} could ask for, not a bound on what is stored.
     */
    static final int UNPAGED_FIRST = MAX_FIRST;

    /**
     * What a terms aggregation returns when {@code size} is not passed.
     */
    static final int DEFAULT_TERMS_SIZE = 10;

    /**
     * What a date histogram counts. Its number of buckets depends on the data and the interval, not on the query, so this is an
     * estimate; {@code minDocCount} of at least one keeps out the empty buckets that would otherwise fill the whole range.
     */
    static final int HISTOGRAM_BUCKETS = 100;

    static final String UNORDERED_TERMS_NOTE = "A terms aggregation without order returns the smallest buckets first; pass order: " +
        "\"_count DESC\" for the largest.";

    private static final String FIRST_ARG = "first";

    private static final String SORT_ARG = "sort";

    private static final String CONTENT_TYPE = "Content";

    private static final String GUILLOTINE_FIELD = "guillotine";

    private static final Set<String> SCOPE_ARGS = Set.of( ProjectBranch.PROJECT, ProjectBranch.BRANCH );

    private static final Set<String> WINDOW_ARGS = Set.of( "offset", "after" );

    private static final String AGGREGATIONS_ARG = "aggregations";

    private static final String SUB_AGGREGATIONS = "subAggregations";

    private static final String TERMS = "terms";

    private static final String SIZE = "size";

    private static final String ORDER = "order";

    private static final String MIN_DOC_COUNT = "minDocCount";

    private static final String DATE_HISTOGRAM = "dateHistogram";

    private static final Set<String> RANGES = Set.of( "range", "dateRange", "geoDistance" );

    private final Map<String, Object> variables;

    private final Map<String, Value<?>> defaults = new HashMap<>();

    private final Map<String, FragmentDefinition> fragments = new HashMap<>();

    private final Map<String, Cost> fragmentCosts = new HashMap<>();

    private final Set<String> visiting = new HashSet<>();

    private GraphQLSchema schema;

    private boolean unorderedTerms;

    private QueryLimits( final Document document, final OperationDefinition operation, final Map<String, Object> variables )
    {
        this.variables = variables;
        operation.getVariableDefinitions()
            .stream()
            .filter( definition -> definition.getDefaultValue() != null )
            .forEach( definition -> defaults.put( definition.getName(), definition.getDefaultValue() ) );
        document.getDefinitionsOfType( FragmentDefinition.class ).forEach( fragment -> fragments.putIfAbsent( fragment.getName(), fragment ) );
    }

    /**
     * Throws a {@link ToolError} for: {@code project} or {@code branch} on the {@code guillotine} field, {@code first} above
     * {@link #MAX_FIRST}, {@code offset} or {@code after} plus {@code first} beyond {@link Cursors#MAX_WINDOW}, an aggregation
     * {@code size} above {@link #MAX_AGGREGATION_SIZE}, a {@code dateHistogram} without {@code minDocCount} of at least 1, and more than {@link #MAX_ROWS} rows
     * or {@link #MAX_BUCKETS} buckets. Variables resolve to the values passed, else to the defaults of {@code operation}. The schema is
     * fetched only once the arguments pass.
     *
     * @return notes for the result: a terms aggregation without {@code order}
     */
    static List<String> check( final Document document, final OperationDefinition operation, final Map<String, Object> variables,
                               final Supplier<GraphQLSchema> schema )
    {
        final QueryLimits limits = new QueryLimits( document, operation, variables );
        limits.checkArguments( document );
        limits.schema = schema.get();

        final Cost cost = limits.cost( operation.getSelectionSet(), limits.schema != null ? limits.schema.getQueryType() : null );
        if ( cost.rows > MAX_ROWS )
        {
            throw new ToolError( "Query reads up to " + count( cost.rows ) + " content items; the limit is " + MAX_ROWS +
                                     ". Each content list, including a repeated content selector, counts its first (when not given, " +
                                     DEFAULT_FIRST + " for a search list and " + UNPAGED_FIRST + " for a repeated field), once per " +
                                     "item of every list around it; lists of plain values count nothing.",
                                 "Use a smaller first on the outer lists and page with after, or split the query into several " +
                                     "calls." );
        }
        if ( cost.buckets > MAX_BUCKETS )
        {
            throw new ToolError( "Query asks for up to " + count( cost.buckets ) + " aggregation buckets; the limit is " + MAX_BUCKETS +
                                     ". A terms aggregation counts its size (" + DEFAULT_TERMS_SIZE + " when not given), a " +
                                     "dateHistogram " + HISTOGRAM_BUCKETS + ", and subAggregations once per bucket around them.",
                                 "Use a smaller size or fewer nested subAggregations, or split the query into several calls." );
        }
        return limits.unorderedTerms ? List.of( UNORDERED_TERMS_NOTE ) : List.of();
    }

    static Instrumentation depthLimit()
    {
        return new DepthLimit();
    }

    private void checkArguments( final Document document )
    {
        final Deque<Node<?>> nodes = new ArrayDeque<>();
        document.getDefinitions().forEach( nodes::push );
        while ( !nodes.isEmpty() )
        {
            final Node<?> node = nodes.pop();
            if ( node instanceof Field field )
            {
                checkField( field );
            }
            else if ( node instanceof ObjectField objectField )
            {
                checkInput( objectField.getName(), literal( objectField.getValue() ) );
            }
            node.getChildren().forEach( nodes::push );
        }
        checkInputs( variables );
    }

    private void checkField( final Field field )
    {
        long start = 0;
        long first = DEFAULT_FIRST;
        for ( final Argument argument : field.getArguments() )
        {
            final String name = argument.getName();
            if ( GUILLOTINE_FIELD.equals( field.getName() ) && SCOPE_ARGS.contains( name ) )
            {
                throw new ToolError( "Pass project and branch as tool arguments, not inside the query.",
                                     "Remove the " + name + " argument of the guillotine field; scopeKey is allowed." );
            }
            if ( FIRST_ARG.equals( name ) && literal( argument.getValue() ) instanceof Number number )
            {
                if ( new BigDecimal( number.toString() ).compareTo( BigDecimal.valueOf( MAX_FIRST ) ) > 0 )
                {
                    throw new ToolError( "first must not exceed " + MAX_FIRST + " in this tool.",
                                         "Use first <= " + MAX_FIRST + " and page with after or offset." );
                }
                first = Math.max( 0, number.longValue() );
            }
            if ( WINDOW_ARGS.contains( name ) )
            {
                start = Math.max( start, windowStart( name, literal( argument.getValue() ) ) );
            }
        }
        if ( start > 0 && start > Cursors.MAX_WINDOW - first )
        {
            throw new ToolError( "offset or after plus first must not exceed " + Cursors.MAX_WINDOW +
                                     " in this tool: the search index cannot page beyond that.",
                                 "Narrow the query with a filter or a different sort instead of paging this deep." );
        }
    }

    private void checkInputs( final Object value )
    {
        if ( value instanceof Map<?, ?> map )
        {
            map.forEach( ( key, item ) -> {
                checkInput( String.valueOf( key ), item );
                checkInputs( item );
            } );
        }
        else if ( value instanceof List<?> list )
        {
            list.forEach( this::checkInputs );
        }
    }

    private void checkInput( final String name, final Object value )
    {
        if ( DATE_HISTOGRAM.equals( name ) && value instanceof Map<?, ?> histogram &&
            !( histogram.get( MIN_DOC_COUNT ) instanceof Number minDocCount && minDocCount.longValue() >= 1 ) )
        {
            throw new ToolError( "dateHistogram needs minDocCount of at least 1 in this tool.",
                                 "Pass minDocCount in each dateHistogram, e.g. minDocCount: 1 to leave out empty buckets." );
        }
        if ( TERMS.equals( name ) && value instanceof Map<?, ?> terms )
        {
            if ( terms.get( SIZE ) instanceof Number size &&
                new BigDecimal( size.toString() ).compareTo( BigDecimal.valueOf( MAX_AGGREGATION_SIZE ) ) > 0 )
            {
                throw new ToolError( "Aggregation size must not exceed " + MAX_AGGREGATION_SIZE + " in this tool.",
                                     "Use size <= " + MAX_AGGREGATION_SIZE + ", or narrow the query so fewer buckets are needed." );
            }
            if ( !( terms.get( ORDER ) instanceof String order ) || order.isBlank() )
            {
                unorderedTerms = true;
            }
        }
    }

    private static long windowStart( final String argument, final Object value )
    {
        if ( "offset".equals( argument ) )
        {
            return value instanceof Number number ? Math.max( 0, number.longValue() ) : 0;
        }
        if ( value instanceof String cursor )
        {
            try
            {
                return Long.parseLong( ConnectionHelper.decodeCursor( cursor ).strip() ) + 1;
            }
            catch ( IllegalArgumentException e )
            {
                return 0;
            }
        }
        return 0;
    }

    private Cost cost( final SelectionSet selectionSet, final GraphQLCompositeType parent )
    {
        Cost total = Cost.NONE;
        if ( selectionSet != null )
        {
            for ( final Selection<?> selection : selectionSet.getSelections() )
            {
                total = total.plus( cost( selection, parent ) );
            }
        }
        return total;
    }

    private Cost cost( final Selection<?> selection, final GraphQLCompositeType parent )
    {
        if ( selection instanceof Field field )
        {
            final GraphQLFieldDefinition definition =
                parent instanceof GraphQLFieldsContainer container ? container.getFieldDefinition( field.getName() ) : null;
            final GraphQLCompositeType type =
                definition != null && GraphQLTypeUtil.unwrapAll( definition.getType() ) instanceof GraphQLCompositeType composite
                    ? composite
                    : null;
            final Cost below = cost( field.getSelectionSet(), type );
            final Cost own = new Cost( 0, aggregationBuckets( field ) );
            if ( definition == null || definition.getArgument( FIRST_ARG ) == null )
            {
                return own.plus( below );
            }
            final long items = pageSize( field, definition );
            return own.plus( new Cost( readsContent( definition ) ? items : 0, 0 ) ).plus( below.times( items ) );
        }
        if ( selection instanceof InlineFragment inlineFragment )
        {
            final TypeName condition = inlineFragment.getTypeCondition();
            return cost( inlineFragment.getSelectionSet(), condition != null ? compositeType( condition ) : parent );
        }
        if ( selection instanceof FragmentSpread spread )
        {
            final Cost known = fragmentCosts.get( spread.getName() );
            if ( known != null )
            {
                return known;
            }
            final FragmentDefinition fragment = fragments.get( spread.getName() );
            if ( fragment == null || !visiting.add( spread.getName() ) )
            {
                return Cost.NONE;
            }
            final Cost fragmentCost = cost( fragment.getSelectionSet(), compositeType( fragment.getTypeCondition() ) );
            visiting.remove( spread.getName() );
            fragmentCosts.put( spread.getName(), fragmentCost );
            return fragmentCost;
        }
        return Cost.NONE;
    }

    private static boolean readsContent( final GraphQLFieldDefinition definition )
    {
        return definition.getArgument( SORT_ARG ) != null || isContent( GraphQLTypeUtil.unwrapAll( definition.getType() ) );
    }

    private static boolean isContent( final GraphQLType type )
    {
        return type instanceof GraphQLInterfaceType contentInterface && CONTENT_TYPE.equals( contentInterface.getName() ) ||
            type instanceof GraphQLObjectType object &&
                object.getInterfaces().stream().anyMatch( implemented -> CONTENT_TYPE.equals( implemented.getName() ) );
    }

    private long pageSize( final Field field, final GraphQLFieldDefinition definition )
    {
        for ( final Argument argument : field.getArguments() )
        {
            if ( FIRST_ARG.equals( argument.getName() ) && literal( argument.getValue() ) instanceof Number number )
            {
                return Math.max( 0, number.longValue() );
            }
        }
        final GraphQLArgument argument = definition.getArgument( FIRST_ARG );
        final InputValueWithState declared = argument.getArgumentDefaultValue();
        final Object value = declared.isLiteral() && declared.getValue() instanceof IntValue intValue
            ? intValue.getValue()
            : declared.isExternal() ? declared.getValue() : null;
        if ( value instanceof Number number )
        {
            return Math.max( 0, number.longValue() );
        }
        return definition.getArgument( SORT_ARG ) != null ? DEFAULT_FIRST : UNPAGED_FIRST;
    }

    private long aggregationBuckets( final Field field )
    {
        long total = 0;
        for ( final Argument argument : field.getArguments() )
        {
            if ( AGGREGATIONS_ARG.equals( argument.getName() ) )
            {
                total = Cost.add( total, buckets( literal( argument.getValue() ) ) );
            }
        }
        return total;
    }

    /**
     * Buckets of a list of aggregations (a single one counts as a list of one): each counts its own buckets and, once per bucket,
     * those of its sub-aggregations.
     */
    private static long buckets( final Object aggregations )
    {
        final List<?> list = aggregations instanceof Map<?, ?> single ? List.of( single ) : aggregations instanceof List<?> items ? items
            : List.of();
        long total = 0;
        for ( final Object item : list )
        {
            if ( item instanceof Map<?, ?> aggregation )
            {
                final long own = ownBuckets( aggregation );
                total = Cost.add( total, Cost.add( own, Cost.multiply( own, buckets( aggregation.get( SUB_AGGREGATIONS ) ) ) ) );
            }
        }
        return total;
    }

    private static long ownBuckets( final Map<?, ?> aggregation )
    {
        if ( aggregation.get( TERMS ) instanceof Map<?, ?> terms )
        {
            return terms.get( SIZE ) instanceof Number size ? Math.max( 1, size.longValue() ) : DEFAULT_TERMS_SIZE;
        }
        if ( aggregation.get( DATE_HISTOGRAM ) != null )
        {
            return HISTOGRAM_BUCKETS;
        }
        for ( final String range : RANGES )
        {
            if ( aggregation.get( range ) instanceof Map<?, ?> ranged )
            {
                return ranged.get( "ranges" ) instanceof List<?> ranges ? Math.max( 1, ranges.size() ) : 1;
            }
        }
        return 1;
    }

    private GraphQLCompositeType compositeType( final TypeName name )
    {
        return schema != null && name != null && schema.getType( name.getName() ) instanceof GraphQLCompositeType type ? type : null;
    }

    /**
     * {@code value} as a variable holds it: strings, numbers, booleans, lists and maps. A variable resolves to its value, else to the
     * default of the checked operation.
     */
    private Object literal( final Value<?> value )
    {
        if ( value instanceof VariableReference reference )
        {
            return variables.containsKey( reference.getName() ) ? variables.get( reference.getName() )
                : literal( defaults.get( reference.getName() ) );
        }
        if ( value instanceof StringValue string )
        {
            return string.getValue();
        }
        if ( value instanceof IntValue intValue )
        {
            return intValue.getValue();
        }
        if ( value instanceof FloatValue floatValue )
        {
            return floatValue.getValue();
        }
        if ( value instanceof BooleanValue booleanValue )
        {
            return booleanValue.isValue();
        }
        if ( value instanceof ArrayValue array )
        {
            final List<Object> result = new ArrayList<>();
            array.getValues().forEach( item -> result.add( literal( item ) ) );
            return result;
        }
        if ( value instanceof ObjectValue object )
        {
            final Map<String, Object> result = new LinkedHashMap<>();
            object.getObjectFields().forEach( objectField -> result.put( objectField.getName(), literal( objectField.getValue() ) ) );
            return result;
        }
        return null;
    }

    private static String count( final long value )
    {
        return value == Long.MAX_VALUE ? "more than " + Long.MAX_VALUE : String.valueOf( value );
    }

    /**
     * Content rows read and aggregation buckets, saturating at {@link Long#MAX_VALUE}.
     */
    private record Cost(long rows, long buckets)
    {
        static final Cost NONE = new Cost( 0, 0 );

        Cost plus( final Cost other )
        {
            return new Cost( add( rows, other.rows ), add( buckets, other.buckets ) );
        }

        Cost times( final long factor )
        {
            return new Cost( multiply( rows, factor ), multiply( buckets, factor ) );
        }

        static long add( final long a, final long b )
        {
            final long sum = a + b;
            return sum < 0 ? Long.MAX_VALUE : sum;
        }

        static long multiply( final long a, final long b )
        {
            return b != 0 && a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
        }
    }

    private static final class DepthLimit
        extends MaxQueryDepthInstrumentation
    {
        DepthLimit()
        {
            super( MAX_DEPTH );
        }

        @Override
        protected AbortExecutionException mkAbortException( final int depth, final int maxDepth )
        {
            return new AbortExecutionException( "Query depth " + depth + " exceeds the limit of " + maxDepth + " nested levels. " +
                                                    "Select fewer nested fields, or split the query into several calls." );
        }
    }
}
