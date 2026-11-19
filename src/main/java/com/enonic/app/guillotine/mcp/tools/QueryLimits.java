package com.enonic.app.guillotine.mcp.tools;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.Instrumentation;
import graphql.language.Argument;
import graphql.language.ArrayValue;
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
import graphql.language.Value;
import graphql.language.TypeName;
import graphql.language.VariableReference;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.InputValueWithState;

import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ToolError;

/**
 * Limits for queries run by MCP tools. Arguments, page sizes and complexity are checked on the document before execution; depth is
 * checked by an instrumentation after validation. Complexity is computed here rather than with graphql-java's
 * {@code MaxQueryComplexityInstrumentation}, which counts the fields of a named fragment only at its first spread.
 */
final class QueryLimits
{
    static final String FIRST_ARG = "first";

    static final int MAX_FIRST = 100;

    /**
     * What Guillotine's list fields (queryDsl, children, their connections and others) return when {@code first} is not passed.
     */
    static final int DEFAULT_FIRST = 10;

    private static final String GUILLOTINE_FIELD = "guillotine";

    private static final Set<String> SCOPE_ARGS = Set.of( ProjectBranchResolver.PROJECT_ARG, ProjectBranchResolver.BRANCH_ARG );

    private static final String SORT_ARG = "sort";

    /**
     * Input fields of Guillotine's query DSL, aggregation, highlight and sort inputs that name an index field.
     */
    private static final Set<String> FIELD_NAME_KEYS = Set.of( "field", "fields", "propertyName" );

    /**
     * Every name-like run in a sort string, so that neither the separators XP accepts (any Unicode whitespace) nor function forms
     * such as {@code geoDistance('owner', ...)} hide a field.
     */
    private static final Pattern SORT_TOKEN = Pattern.compile( "[A-Za-z0-9_.^*?]+" );

    private static final String PRINCIPAL_HINT =
        "Filter, aggregate and sort on other fields; only CMS users and administrators may use creator, modifier, owner and " +
            "permissions.";

    private final Document document;

    private final Map<String, Object> variables;

    private final GraphQLSchema schema;

    private final boolean principalsAllowed;

    private final Map<String, Value<?>> defaults = new HashMap<>();

    private final Map<String, FragmentDefinition> fragments = new HashMap<>();

    private final Map<String, Long> fragmentCosts = new HashMap<>();

    private final Set<String> visiting = new HashSet<>();

    private QueryLimits( final Document document, final OperationDefinition operation, final Map<String, Object> variables,
                         final GraphQLSchema schema, final boolean principalsAllowed )
    {
        this.document = document;
        this.variables = variables;
        this.schema = schema;
        this.principalsAllowed = principalsAllowed;
        operation.getVariableDefinitions()
            .stream()
            .filter( definition -> definition.getDefaultValue() != null )
            .forEach( definition -> defaults.put( definition.getName(), definition.getDefaultValue() ) );
        document.getDefinitionsOfType( FragmentDefinition.class )
            .forEach( fragment -> fragments.putIfAbsent( fragment.getName(), fragment ) );
    }

    static Instrumentation depthLimit( final int maxDepth )
    {
        return new DepthLimit( Math.max( 1, maxDepth ) );
    }

    /**
     * Rejects {@code project}/{@code branch} arguments of the {@code guillotine} field (they would bypass the resolved project and its
     * access check), {@code first} above {@link #MAX_FIRST}, principal fields (creator, modifier, owner, permissions) in filters,
     * aggregations, highlights and sorts unless {@code principalsAllowed}, and an
     * {@code operation} whose complexity exceeds {@code maxComplexity}. Variables resolve with the defaults of {@code operation}.
     * A field costs one plus the cost of its selection multiplied by its {@code first} argument; a field of {@code schema} that takes
     * {@code first} but is not given one counts with the argument's default, else {@link #DEFAULT_FIRST}. Every fragment spread counts.
     * The schema is only fetched once the arguments are known to be acceptable.
     */
    static void check( final Document document, final OperationDefinition operation, final Map<String, Object> variables,
                       final int maxComplexity, final boolean principalsAllowed, final Supplier<GraphQLSchema> schema )
    {
        new QueryLimits( document, operation, variables, null, principalsAllowed ).checkArguments();
        final QueryLimits limits = new QueryLimits( document, operation, variables, schema.get(), principalsAllowed );

        final long complexity = limits.cost( operation.getSelectionSet(), limits.schema != null ? limits.schema.getQueryType() : null );
        final int max = Math.max( 1, maxComplexity );
        if ( complexity > max )
        {
            throw new ToolError( "Query complexity " + ( complexity == Long.MAX_VALUE ? "overflow" : complexity ) +
                                     " exceeds the limit of " + max + " (mcp.maxComplexity); every field counts, multiplied by the " +
                                     "first argument of the lists around it.",
                                 "Select fewer fields, use a smaller first and paginate, or split the query into several calls." );
        }
    }

    private void checkArguments()
    {
        final Deque<Node<?>> nodes = new ArrayDeque<>();
        document.getDefinitions().forEach( nodes::add );
        while ( !nodes.isEmpty() )
        {
            final Node<?> node = nodes.pop();
            if ( node instanceof Field field )
            {
                for ( final Argument argument : field.getArguments() )
                {
                    if ( GUILLOTINE_FIELD.equals( field.getName() ) && SCOPE_ARGS.contains( argument.getName() ) )
                    {
                        throw new ToolError( "Pass project and branch as tool arguments, not inside the query.",
                                             "Remove the " + argument.getName() +
                                                 " argument of the guillotine field; scopeKey is allowed." );
                    }
                    if ( FIRST_ARG.equals( argument.getName() ) && exceedsMaxFirst( value( argument.getValue() ) ) )
                    {
                        throw new ToolError( "first must not exceed " + MAX_FIRST + " in this tool.",
                                             "Use first <= " + MAX_FIRST +
                                                 " and paginate with after, or use the search tool for paging." );
                    }
                    if ( SORT_ARG.equals( argument.getName() ) && literal( argument.getValue() ) instanceof String sort )
                    {
                        checkSort( sort );
                    }
                }
            }
            else if ( node instanceof ObjectField objectField )
            {
                checkInput( objectField.getName(), literal( objectField.getValue() ) );
            }
            node.getChildren().forEach( nodes::add );
        }
        checkInputs( variables );
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
        if ( FIELD_NAME_KEYS.contains( name ) )
        {
            if ( value instanceof List<?> list )
            {
                list.forEach( this::checkFieldName );
            }
            else
            {
                checkFieldName( value );
            }
        }
    }

    private void checkFieldName( final Object value )
    {
        if ( value instanceof String name )
        {
            SearchQuery.checkPrincipal( name, "field", principalsAllowed, PRINCIPAL_HINT );
        }
    }

    private void checkSort( final String sort )
    {
        final Matcher token = SORT_TOKEN.matcher( sort );
        while ( token.find() )
        {
            SearchQuery.checkPrincipal( token.group(), "sort", principalsAllowed, PRINCIPAL_HINT );
        }
    }

    private long cost( final SelectionSet selectionSet, final GraphQLCompositeType parent )
    {
        if ( selectionSet == null )
        {
            return 0;
        }
        long total = 0;
        for ( final Selection<?> selection : selectionSet.getSelections() )
        {
            total = add( total, cost( selection, parent ) );
        }
        return total;
    }

    private long cost( final Selection<?> selection, final GraphQLCompositeType parent )
    {
        if ( selection instanceof Field field )
        {
            final GraphQLFieldDefinition definition =
                parent instanceof GraphQLFieldsContainer container ? container.getFieldDefinition( field.getName() ) : null;
            final GraphQLCompositeType type = definition != null &&
                GraphQLTypeUtil.unwrapAll( definition.getType() ) instanceof GraphQLCompositeType composite ? composite : null;
            return add( 1, multiply( cost( field.getSelectionSet(), type ), multiplier( field, definition ) ) );
        }
        if ( selection instanceof InlineFragment inlineFragment )
        {
            final TypeName condition = inlineFragment.getTypeCondition();
            return cost( inlineFragment.getSelectionSet(), condition != null ? compositeType( condition ) : parent );
        }
        if ( selection instanceof FragmentSpread spread )
        {
            final Long known = fragmentCosts.get( spread.getName() );
            if ( known != null )
            {
                return known;
            }
            final FragmentDefinition fragment = fragments.get( spread.getName() );
            if ( fragment == null || !visiting.add( spread.getName() ) )
            {
                return 0;
            }
            final long fragmentCost = cost( fragment.getSelectionSet(), compositeType( fragment.getTypeCondition() ) );
            visiting.remove( spread.getName() );
            fragmentCosts.put( spread.getName(), fragmentCost );
            return fragmentCost;
        }
        return 0;
    }

    private long multiplier( final Field field, final GraphQLFieldDefinition definition )
    {
        final Argument first = field.getArguments()
            .stream()
            .filter( argument -> FIRST_ARG.equals( argument.getName() ) )
            .findFirst()
            .orElse( null );
        if ( first != null && value( first.getValue() ) instanceof Number number )
        {
            return Math.max( 1, number.longValue() );
        }
        final GraphQLArgument argument = definition != null ? definition.getArgument( FIRST_ARG ) : null;
        if ( argument == null )
        {
            return 1;
        }
        final InputValueWithState declared = argument.getArgumentDefaultValue();
        final Object defaultValue = declared.isLiteral() && declared.getValue() instanceof IntValue intValue
            ? intValue.getValue()
            : declared.isExternal() ? declared.getValue() : null;
        return defaultValue instanceof Number number ? Math.max( 1, number.longValue() ) : DEFAULT_FIRST;
    }

    private GraphQLCompositeType compositeType( final TypeName name )
    {
        return schema != null && name != null && schema.getType( name.getName() ) instanceof GraphQLCompositeType type ? type : null;
    }

    /**
     * The value of {@code value} as a variable would hold it: strings, numbers, booleans, lists and maps; a variable resolves to its
     * value, else to the default of the checked operation.
     */
    private Object literal( final Value<?> value )
    {
        if ( value instanceof VariableReference reference )
        {
            if ( variables.containsKey( reference.getName() ) )
            {
                return variables.get( reference.getName() );
            }
            return literal( defaults.get( reference.getName() ) );
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

    private Object value( final Value<?> value )
    {
        if ( value instanceof VariableReference reference )
        {
            if ( variables.containsKey( reference.getName() ) )
            {
                return variables.get( reference.getName() );
            }
            final Value<?> defaultValue = defaults.get( reference.getName() );
            return defaultValue instanceof IntValue intValue ? intValue.getValue() : null;
        }
        return value instanceof IntValue intValue ? intValue.getValue() : null;
    }

    private static boolean exceedsMaxFirst( final Object first )
    {
        return first instanceof Number number && new BigDecimal( number.toString() ).compareTo( BigDecimal.valueOf( MAX_FIRST ) ) > 0;
    }

    private static long add( final long a, final long b )
    {
        final long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    private static long multiply( final long a, final long b )
    {
        return b != 0 && a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }

    private static final class DepthLimit
        extends MaxQueryDepthInstrumentation
    {
        DepthLimit( final int maxDepth )
        {
            super( maxDepth );
        }

        @Override
        protected AbortExecutionException mkAbortException( final int depth, final int maxDepth )
        {
            return new AbortExecutionException(
                "Query depth " + depth + " exceeds the limit of " + maxDepth + " nested levels (mcp.maxDepth). " +
                    "Select fewer nested fields, or split the query into several calls." );
        }
    }
}
