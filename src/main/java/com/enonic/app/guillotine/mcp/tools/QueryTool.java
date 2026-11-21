package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.validation.GoodFaithIntrospectionExceeded;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.GraphQlErrors;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.app.guillotine.mcp.shape.ToolError;

/**
 * Runs one read-only GraphQL operation against the Guillotine schema, within {@link QueryLimits}. The resolved project and branch
 * become the context repository and branch, which the {@code guillotine} field reads when the document passes no {@code project}
 * or {@code branch} of its own; {@link QueryLimits} refuses those.
 */
public final class QueryTool
    implements McpTool
{
    static final String QUERY_ARG = "query";

    static final String VARIABLES_ARG = "variables";

    static final String OPERATION_NAME_ARG = "operationName";

    static final String DATA = "data";

    private static final Parser PARSER = new Parser();

    private static final int MAX_LISTED_OPERATIONS = 10;

    private static final int MAX_NAME_CHARS = 100;

    private static final int MAX_LISTED_FIELDS = 30;

    private static final int MAX_HINTED_TYPES = 3;

    private static final String PRINCIPAL_KEY_TYPE = "PrincipalKey";

    private static final String PRINCIPAL_ADVICE = "Filters, sorts and aggregations on the modifier, creator and owner index " +
        "fields work without that role: for the caller's own edits use search with mine, and to count per person aggregate on modifier.";

    private static final String ANONYMOUS_PRINCIPAL_ADVICE = "Filters, sorts and aggregations on the modifier, creator and owner " +
        "index fields need a signed-in caller too.";

    /**
     * The type in GraphQL validation errors about selections: "Field 'x' in type 'T' is undefined" and "Subselection required for
     * type 'T' of field 'x'", where T may carry list and non-null wrappers such as [Content]!.
     */
    private static final Pattern SELECTION_ERROR_TYPE =
        Pattern.compile( "(?:FieldUndefined|SubselectionRequired)@.*?type '([\\[\\]!\\w]+)'" );

    private static final String CONTENT_TYPE = "Content";

    private final SchemaProvider schemaProvider;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    public QueryTool( final SchemaProvider schemaProvider, final GuillotineConfigService configService,
                      final ProjectBranchResolver resolver )
    {
        this.schemaProvider = schemaProvider;
        this.configService = configService;
        this.resolver = resolver;
    }

    @Override
    public McpGroup group()
    {
        return McpGroup.GRAPHQL;
    }

    @Override
    public String name()
    {
        return "query";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final String query = requireQuery( args.get( QUERY_ARG ) );
        final Map<String, Object> variables = variables( args.get( VARIABLES_ARG ) );
        final String operationName = operationName( args.get( OPERATION_NAME_ARG ) );

        final Document document = parse( query );
        final OperationDefinition operation = selectOperation( document, operationName );
        final boolean anonymous = !ProjectBranchResolver.isAuthenticated();
        final List<String> notes =
            new ArrayList<>( QueryLimits.check( document, operation, variables, anonymous, schemaProvider::getSchema ) );
        final ProjectBranch projectBranch = resolver.resolve( args );

        final Instrumentation instrumentation = new ChainedInstrumentation( QueryLimits.depthLimit(), GraphQlErrors.hideInternalErrors() );
        final Map<String, Object> response;
        try
        {
            response = projectBranch.callWith(
                () -> schemaProvider.executeToSpecification( query, variables, operation.getName(), instrumentation ) );
        }
        catch ( GoodFaithIntrospectionExceeded e )
        {
            throw new ToolError( "Introspection limit exceeded: " + withoutPeriod(
                McpJson.shorten( e.toBadFaithError().getMessage(), McpJson.PREVIEW_CHARS ) ) + ".",
                                 "Call describe-type for the fields of one content type instead of introspecting the schema." );
        }

        final Object data = response.get( DATA );
        final List<?> rawErrors = response.get( McpResult.ERRORS ) instanceof List<?> list ? list : List.of();
        final List<String> messages = GraphQlErrors.messages( rawErrors );
        final List<Map<String, Object>> errors =
            GraphQlErrors.partialErrors( data, rawErrors, selectionHint( messages, schemaProvider.getSchema() ) );
        if ( messages.contains( GraphQlErrors.PRINCIPAL_DENIED ) )
        {
            notes.add( principalAdvice() );
        }

        final Map<String, Object> structured = projectBranch.putInto( new LinkedHashMap<>() );
        structured.put( DATA, data );
        if ( !errors.isEmpty() )
        {
            structured.put( McpResult.ERRORS, errors );
        }
        if ( !notes.isEmpty() )
        {
            structured.put( ResultBudget.NOTE, String.join( " ", notes ) );
        }
        return ResultBudget.fromConfig( configService )
            .keepingNullFields()
            .withNarrowHint( "Select fewer fields or pass a smaller first." )
            .fitSubtrees( structured, DATA );
    }

    /**
     * For a document that selects a field its type does not have, or selects nothing below an object field: the fields of those types.
     * The Content interface also gets the inline fragment its type-specific fields need, and PrincipalKey the rights its values need.
     */
    static String selectionHint( final List<String> messages, final GraphQLSchema schema )
    {
        final Set<String> types = new LinkedHashSet<>();
        for ( final String message : messages )
        {
            final Matcher matcher = SELECTION_ERROR_TYPE.matcher( message );
            while ( matcher.find() && types.size() < MAX_HINTED_TYPES )
            {
                types.add( matcher.group( 1 ).replaceAll( "[\\[\\]!]", "" ) );
            }
        }
        final List<String> parts = new ArrayList<>();
        for ( final String name : types )
        {
            if ( schema.getType( name ) instanceof GraphQLFieldsContainer container )
            {
                final List<String> fields = container.getFieldDefinitions().stream().map( GraphQLFieldDefinition::getName ).toList();
                parts.add( name + " has " + String.join( ", ", fields.subList( 0, Math.min( fields.size(), MAX_LISTED_FIELDS ) ) ) +
                               ( fields.size() > MAX_LISTED_FIELDS ? " and " + ( fields.size() - MAX_LISTED_FIELDS ) + " more" : "" ) +
                               "." );
                if ( CONTENT_TYPE.equals( name ) )
                {
                    parts.add( "Fields of one content type, such as data, need an inline fragment: ... on <graphqlType from list-types>." );
                }
                else if ( container instanceof GraphQLInterfaceType type )
                {
                    final List<String> implementations = schema.getImplementations( type ).stream().map( GraphQLObjectType::getName ).toList();
                    parts.add( "Fields of one implementation need an inline fragment on one of " + String.join( ", ",
                        implementations.subList( 0, Math.min( implementations.size(), MAX_LISTED_FIELDS ) ) ) + "." );
                }
                if ( PRINCIPAL_KEY_TYPE.equals( name ) )
                {
                    parts.add( "Its values need Content Studio access or an administrator role. " + principalAdvice() );
                }
            }
        }
        return parts.isEmpty() ? null : String.join( " ", parts );
    }

    private static String principalAdvice()
    {
        return ProjectBranchResolver.isAuthenticated() ? PRINCIPAL_ADVICE : ANONYMOUS_PRINCIPAL_ADVICE;
    }

    private static String requireQuery( final Object query )
    {
        if ( !( query instanceof String text ) || text.isBlank() )
        {
            throw new ToolError( "query is required and must be a GraphQL document string.",
                                 "Send a document such as { guillotine { queryDsl(query: {matchAll: {}}, first: 5) { _path } } }." );
        }
        return text;
    }

    private static Map<String, Object> variables( final Object variables )
    {
        if ( variables == null )
        {
            return Map.of();
        }
        if ( variables instanceof Map<?, ?> map )
        {
            final Map<String, Object> result = new LinkedHashMap<>();
            map.forEach( ( key, value ) -> result.put( String.valueOf( key ), value ) );
            return result;
        }
        throw new ToolError( "Invalid variables: expected a JSON object.",
                             "Pass variables as a JSON object keyed by variable name, without the $." );
    }

    private static String operationName( final Object operationName )
    {
        if ( operationName == null || "".equals( operationName ) )
        {
            return null;
        }
        if ( operationName instanceof String name )
        {
            return name;
        }
        throw new ToolError( "Invalid operationName: " + McpJson.preview( operationName ) + ".",
                             "Pass the name of one query operation from the document." );
    }

    private Document parse( final String query )
    {
        final ParserOptions options = ParserOptions.newParserOptions().maxTokens( configService.getMaxQueryTokens() ).build();
        try
        {
            return PARSER.parseDocument( ParserEnvironment.newParserEnvironment().document( query ).parserOptions( options ).build() );
        }
        catch ( InvalidSyntaxException e )
        {
            throw new ToolError( "Invalid GraphQL document: " + withoutPeriod( McpJson.shorten( e.getMessage(), McpJson.PREVIEW_CHARS ) ) +
                                     ".", "Fix the syntax and send the query again." );
        }
    }

    private static OperationDefinition selectOperation( final Document document, final String operationName )
    {
        final List<OperationDefinition> operations = document.getDefinitionsOfType( OperationDefinition.class );

        for ( final OperationDefinition operation : operations )
        {
            if ( operation.getOperation() != OperationDefinition.Operation.QUERY )
            {
                final String kind = operation.getOperation().name().toLowerCase( Locale.ROOT );
                throw new ToolError( "Only query operations are allowed, but the document contains a " + kind +
                                         ( operation.getName() != null ? " '" + shortName( operation.getName() ) + "'" : "" ) + ".",
                                     "This tool is read-only: remove the " + kind + " and send query operations only." );
            }
        }

        if ( operations.isEmpty() )
        {
            throw new ToolError( "The document contains no query operation.", "Send a document such as { guillotine { ... } }." );
        }

        if ( operationName != null )
        {
            return operations.stream()
                .filter( operation -> operationName.equals( operation.getName() ) )
                .findFirst()
                .orElseThrow( () -> new ToolError( "Operation '" + shortName( operationName ) + "' is not in the document.",
                                                   "Operations in the document: " + describe( operations ) + "." ) );
        }

        if ( operations.size() > 1 )
        {
            throw new ToolError( "The document contains " + operations.size() + " operations; operationName is required.",
                                 "Pass operationName, one of: " + describe( operations ) + "." );
        }
        return operations.get( 0 );
    }

    private static String describe( final List<OperationDefinition> operations )
    {
        final String names = operations.stream()
            .limit( MAX_LISTED_OPERATIONS )
            .map( operation -> operation.getName() != null ? shortName( operation.getName() ) : "(anonymous)" )
            .collect( Collectors.joining( ", " ) );
        return operations.size() <= MAX_LISTED_OPERATIONS ? names : names + " and " + ( operations.size() - MAX_LISTED_OPERATIONS ) +
            " more";
    }

    private static String shortName( final String name )
    {
        return McpJson.shorten( name, MAX_NAME_CHARS );
    }

    private static String withoutPeriod( final String message )
    {
        return message.replaceAll( "[.!]+$", "" );
    }
}
