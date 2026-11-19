package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import graphql.Scalars;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.SchemaPrinter;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.app.guillotine.mcp.shape.ToolError;

/**
 * Describes one GraphQL type as compact SDL. {@code type} may be a full content type name ({@code app:name}), a GraphQL type name,
 * or the local part of a content type name when exactly one content type has it. Content types are recognised by the
 * {@code "<title> - <app:name>"} description Guillotine gives their GraphQL types.
 */
public final class DescribeTypeTool
    implements McpTool
{
    static final String TYPE_ARG = "type";

    static final int MAX_NESTING = 2;

    static final int MAX_SDL_CHARS = 8000;

    /**
     * {@code first} of repeated object fields in the example query, which would otherwise count every occurrence for complexity.
     */
    private static final int EXAMPLE_FIRST = 3;

    private static final int EXAMPLE_PAGE = 10;

    private static final String CONTENT_INTERFACE = "Content";

    private static final String CONTENT_FIELDS_LINE = "  # plus the fields of the Content interface\n";

    private static final Pattern CONTENT_TYPE_NAME = Pattern.compile( "[\\w.\\-]+:[\\w.\\-]+" );

    private static final Set<String> BUILT_IN_SCALARS =
        Set.of( Scalars.GraphQLString.getName(), Scalars.GraphQLInt.getName(), Scalars.GraphQLFloat.getName(),
                Scalars.GraphQLBoolean.getName(), Scalars.GraphQLID.getName() );

    private static final int MAX_SUGGESTIONS = 5;

    private static final int MAX_EXAMPLE_LEAVES = 6;

    private static final SchemaPrinter PRINTER =
        new SchemaPrinter( SchemaPrinter.Options.defaultOptions().includeDirectives( false ).descriptionsAsHashComments( true ) );

    private final SchemaProvider schemaProvider;

    private final GuillotineConfigService configService;

    public DescribeTypeTool( final SchemaProvider schemaProvider, final GuillotineConfigService configService )
    {
        this.schemaProvider = schemaProvider;
        this.configService = configService;
    }

    @Override
    public String name()
    {
        return "describe-type";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final Object typeArg = args.get( TYPE_ARG );
        if ( !( typeArg instanceof String requested ) || requested.isBlank() )
        {
            throw new ToolError( "type is required.", "Pass a content type name from list-types, such as \"app.key:article\"." );
        }

        final GraphQLSchema schema = schemaProvider.getSchema();
        final Map<String, GraphQLObjectType> contentTypes = contentTypes( schema );
        final Resolved resolved = resolve( schema, contentTypes, requested.trim() );

        final Map<String, Object> structured = new LinkedHashMap<>();
        if ( resolved.contentType() != null )
        {
            structured.put( "contentType", resolved.contentType() );
        }
        structured.put( "graphqlType", resolved.type().getName() );

        final GraphQLObjectType dataType = dataType( resolved.type() );
        if ( dataType != null )
        {
            structured.put( "dataType", dataType.getName() );
            structured.put( "dataFields", dataType.getFieldDefinitions()
                .stream()
                .map( field -> Map.of( "name", field.getName(), "type", GraphQLTypeUtil.simplePrint( field.getType() ) ) )
                .toList() );
        }

        final Set<String> shared = sharedTypes( schema, contentTypes );
        final Sdl sdl = sdl( resolved.type(), shared, Math.min( MAX_SDL_CHARS, configService.getMcpMaxResultChars() / 2 ),
                             Math.min( MAX_NESTING, Math.max( 0, configService.getMcpMaxDepth() ) ) );
        structured.put( "sdl", sdl.text() );
        if ( !sdl.omitted().isEmpty() )
        {
            structured.put( "omittedTypes", sdl.omitted() );
        }
        if ( !sdl.notExpanded().isEmpty() )
        {
            structured.put( "sharedTypes", sdl.notExpanded() );
        }

        if ( resolved.contentType() != null )
        {
            final String exampleQuery = exampleQuery( (GraphQLObjectType) resolved.type(), dataType );
            structured.put( "exampleQuery", exampleQuery );
            structured.put( "exampleVariables", Map.of( "type", resolved.contentType(), "first", exampleFirst( exampleQuery, schema ) ) );
            structured.put( "usage", "Call query with {query: exampleQuery, variables: exampleVariables}; page with variables.after = " +
                "pageInfo.endCursor." );
        }

        return McpResult.success( summary( resolved, sdl ), structured,
                                  ResultBudget.of( configService.getMcpMaxResultChars(), "dataFields" )
                                      .withNarrowHint( "Call describe-type on the nested types separately." ) );
    }

    private static Map<String, GraphQLObjectType> contentTypes( final GraphQLSchema schema )
    {
        final Map<String, GraphQLObjectType> result = new TreeMap<>();
        if ( schema.getType( CONTENT_INTERFACE ) instanceof GraphQLInterfaceType contentInterface )
        {
            for ( final GraphQLObjectType type : schema.getImplementations( contentInterface ) )
            {
                final String name = contentTypeName( type );
                if ( name != null )
                {
                    result.putIfAbsent( name, type );
                }
            }
        }
        return result;
    }

    private static String contentTypeName( final GraphQLObjectType type )
    {
        final String description = type.getDescription();
        if ( description == null )
        {
            return null;
        }
        final int separator = description.lastIndexOf( " - " );
        final String name = separator < 0 ? null : description.substring( separator + 3 ).trim();
        return name != null && CONTENT_TYPE_NAME.matcher( name ).matches() ? name : null;
    }

    private static Resolved resolve( final GraphQLSchema schema, final Map<String, GraphQLObjectType> contentTypes,
                                     final String requested )
    {
        final GraphQLObjectType byContentType = contentTypes.get( requested );
        if ( byContentType != null )
        {
            return new Resolved( requested, byContentType );
        }

        if ( !requested.contains( ":" ) && !requested.startsWith( "__" ) && schema.getType( requested ) instanceof GraphQLNamedType type )
        {
            final String contentType = type instanceof GraphQLObjectType objectType ? contentTypeName( objectType ) : null;
            return new Resolved( contentType != null && contentTypes.get( contentType ) == type ? contentType : null, type );
        }

        if ( !requested.contains( ":" ) )
        {
            final List<String> matches = contentTypes.keySet()
                .stream()
                .filter( name -> name.substring( name.indexOf( ':' ) + 1 ).equalsIgnoreCase( requested ) )
                .toList();
            if ( matches.size() == 1 )
            {
                return new Resolved( matches.get( 0 ), contentTypes.get( matches.get( 0 ) ) );
            }
            if ( matches.size() > 1 )
            {
                throw new ToolError( "Type '" + requested + "' is ambiguous: " + String.join( ", ", matches ) + ".",
                                     "Pass the full content type name." );
            }
        }

        final String needle = requested.substring( requested.indexOf( ':' ) + 1 ).toLowerCase( Locale.ROOT );
        final List<String> suggestions = contentTypes.keySet()
            .stream()
            .filter( name -> name.toLowerCase( Locale.ROOT ).contains( needle ) )
            .limit( MAX_SUGGESTIONS )
            .toList();
        throw new ToolError( "Unknown type '" + McpJson.shorten( requested, McpJson.PREVIEW_CHARS ) + "'.",
                             ( suggestions.isEmpty() ? "" : "Similar content types: " + String.join( ", ", suggestions ) + ". " ) +
                                 "Call list-types to see the available content types, then pass one as \"app.key:name\"." );
    }

    private static GraphQLObjectType dataType( final GraphQLNamedType type )
    {
        if ( type instanceof GraphQLObjectType objectType && objectType.getFieldDefinition( "data" ) != null &&
            GraphQLTypeUtil.unwrapAll( objectType.getFieldDefinition( "data" ).getType() ) instanceof GraphQLObjectType data )
        {
            return data;
        }
        return null;
    }

    /**
     * Types every content type shares: the {@code Content} interface, its implementations and everything its fields reference.
     * They are listed by name instead of being printed for each content type.
     */
    private static Set<String> sharedTypes( final GraphQLSchema schema, final Map<String, GraphQLObjectType> contentTypes )
    {
        final Set<String> result = new HashSet<>();
        if ( schema.getType( CONTENT_INTERFACE ) instanceof GraphQLInterfaceType contentInterface )
        {
            result.add( contentInterface.getName() );
            result.addAll( referencedTypes( contentInterface ) );
            schema.getImplementations( contentInterface ).forEach( type -> result.add( type.getName() ) );
        }
        contentTypes.values().forEach( type -> result.add( type.getName() ) );
        return result;
    }

    /**
     * A content type is printed without the fields it shares with the {@code Content} interface, and a comment line in their place.
     * A type too large for {@code maxChars} is cut after its last field that fits, keeping at least its header.
     */
    static Sdl sdl( final GraphQLNamedType type, final Set<String> shared, final int maxChars, final int maxNesting )
    {
        final GraphQLNamedType root = withoutContentFields( type );
        final StringBuilder text = new StringBuilder( PRINTER.print( root ).trim() );
        if ( root != type )
        {
            final int body = text.indexOf( "{\n" );
            if ( body < 0 )
            {
                text.append( " {\n" ).append( CONTENT_FIELDS_LINE ).append( '}' );
            }
            else
            {
                text.insert( body + 2, CONTENT_FIELDS_LINE );
            }
        }
        if ( text.length() > maxChars )
        {
            final String cutLine = "\n  # more fields left out to stay within " + maxChars + " characters\n}";
            final int header = text.indexOf( "{\n" ) + 1;
            text.setLength( Math.max( header, text.lastIndexOf( "\n", Math.max( 0, maxChars - cutLine.length() ) ) ) );
            text.append( cutLine );
        }
        final Set<String> seen = new HashSet<>( Set.of( root.getName() ) );
        final Set<String> omitted = new LinkedHashSet<>();
        final Set<String> notExpanded = new LinkedHashSet<>();

        final Deque<Map.Entry<GraphQLNamedType, Integer>> queue = new ArrayDeque<>();
        queue.add( Map.entry( root, 0 ) );
        while ( !queue.isEmpty() )
        {
            final Map.Entry<GraphQLNamedType, Integer> current = queue.poll();
            for ( final GraphQLNamedType next : referencedNamedTypes( current.getKey() ) )
            {
                final String name = next.getName();
                if ( !seen.add( name ) || BUILT_IN_SCALARS.contains( name ) || name.startsWith( "__" ) )
                {
                    continue;
                }
                if ( shared.contains( name ) )
                {
                    notExpanded.add( name );
                    continue;
                }
                if ( current.getValue() >= maxNesting )
                {
                    omitted.add( name );
                    continue;
                }
                final String printed = PRINTER.print( next ).trim();
                if ( text.length() + printed.length() + 2 > maxChars )
                {
                    omitted.add( name );
                    continue;
                }
                text.append( "\n\n" ).append( printed );
                queue.add( Map.entry( next, current.getValue() + 1 ) );
            }
        }
        return new Sdl( text.toString(), List.copyOf( omitted ), List.copyOf( notExpanded ) );
    }

    private static GraphQLNamedType withoutContentFields( final GraphQLNamedType type )
    {
        if ( !( type instanceof GraphQLObjectType objectType ) )
        {
            return type;
        }
        final GraphQLInterfaceType contentInterface = objectType.getInterfaces()
            .stream()
            .filter( GraphQLInterfaceType.class::isInstance )
            .map( GraphQLInterfaceType.class::cast )
            .filter( candidate -> CONTENT_INTERFACE.equals( candidate.getName() ) )
            .findFirst()
            .orElse( null );
        if ( contentInterface == null )
        {
            return type;
        }
        final List<GraphQLFieldDefinition> own = objectType.getFieldDefinitions()
            .stream()
            .filter( field -> contentInterface.getFieldDefinition( field.getName() ) == null )
            .toList();
        return objectType.transform( builder -> builder.clearFields().fields( own ) );
    }

    private static Set<String> referencedTypes( final GraphQLNamedType type )
    {
        return referencedNamedTypes( type ).stream().map( GraphQLNamedType::getName ).collect( Collectors.toSet() );
    }

    private static List<GraphQLNamedType> referencedNamedTypes( final GraphQLNamedType type )
    {
        final List<GraphQLType> types = new ArrayList<>();
        if ( type instanceof GraphQLFieldsContainer container )
        {
            for ( final GraphQLFieldDefinition field : container.getFieldDefinitions() )
            {
                types.add( field.getType() );
                field.getArguments().stream().map( GraphQLArgument::getType ).forEach( types::add );
            }
        }
        else if ( type instanceof GraphQLInputObjectType inputType )
        {
            inputType.getFieldDefinitions().forEach( field -> types.add( field.getType() ) );
        }
        else if ( type instanceof GraphQLUnionType unionType )
        {
            types.addAll( unionType.getTypes() );
        }
        return types.stream()
            .map( GraphQLTypeUtil::unwrapAll )
            .filter( GraphQLNamedType.class::isInstance )
            .map( GraphQLNamedType.class::cast )
            .toList();
    }

    private static String exampleQuery( final GraphQLObjectType type, final GraphQLObjectType dataType )
    {
        final StringBuilder query = new StringBuilder();
        query.append( "query ($type: String!, $first: Int, $after: String) {\n" );
        query.append( "  guillotine {\n" );
        query.append( "    queryDslConnection(query: {term: {field: \"type\", value: {string: $type}}}, " )
            .append( "first: $first, after: $after) {\n" );
        query.append( "      totalCount\n" );
        query.append( "      edges {\n" );
        query.append( "        node {\n" );
        query.append( "          _id\n" );
        query.append( "          _path\n" );
        query.append( "          displayName\n" );
        if ( dataType != null )
        {
            final String data = selection( dataType, type.getName() + "_", 1, "              " );
            if ( !data.isEmpty() )
            {
                query.append( "          ... on " ).append( type.getName() ).append( " {\n" );
                query.append( "            data " ).append( data ).append( "\n" );
                query.append( "          }\n" );
            }
        }
        query.append( "        }\n" );
        query.append( "      }\n" );
        query.append( "      pageInfo {\n" );
        query.append( "        endCursor\n" );
        query.append( "        hasNext\n" );
        query.append( "      }\n" );
        query.append( "    }\n" );
        query.append( "  }\n" );
        query.append( "}" );
        return query.toString();
    }

    /**
     * The largest page size up to {@link #EXAMPLE_PAGE} for which {@code query} stays within {@code mcp.maxComplexity}, at least one.
     */
    private int exampleFirst( final String query, final GraphQLSchema schema )
    {
        final Document document = new Parser().parseDocument( query );
        final OperationDefinition operation = document.getDefinitionsOfType( OperationDefinition.class ).get( 0 );
        for ( int first = EXAMPLE_PAGE; first > 1; first-- )
        {
            try
            {
                QueryLimits.check( document, operation, Map.of( "first", first ), configService.getMcpMaxComplexity(), true,
                                   () -> schema );
                return first;
            }
            catch ( ToolError e )
            {
                // too complex at this page size
            }
        }
        return 1;
    }

    /**
     * A selection set for {@code type}: scalar fields and nested types of the content type itself (named with {@code ownPrefix}) up
     * to {@code nesting} levels, only the first scalar of other types such as {@code RichText}, and id and path of referenced content.
     * Repeated object fields get a small {@code first}.
     */
    private static String selection( final GraphQLType type, final String ownPrefix, final int nesting, final String indent )
    {
        final GraphQLType unwrapped = GraphQLTypeUtil.unwrapAll( type );
        if ( unwrapped instanceof GraphQLScalarType || unwrapped instanceof GraphQLEnumType )
        {
            return "";
        }
        if ( isContent( unwrapped ) )
        {
            return block( List.of( "_id", "_path" ), indent );
        }
        if ( !( unwrapped instanceof GraphQLFieldsContainer container ) )
        {
            return block( List.of( "__typename" ), indent );
        }

        final boolean sharedType = !container.getName().startsWith( ownPrefix );
        final List<String> lines = new ArrayList<>();
        int leaves = 0;
        for ( final GraphQLFieldDefinition field : container.getFieldDefinitions() )
        {
            if ( field.getArguments().stream().anyMatch( argument -> argument.getType() instanceof GraphQLNonNull ) )
            {
                continue;
            }
            final GraphQLType fieldType = GraphQLTypeUtil.unwrapAll( field.getType() );
            if ( fieldType instanceof GraphQLScalarType || fieldType instanceof GraphQLEnumType )
            {
                if ( leaves < ( sharedType ? 1 : MAX_EXAMPLE_LEAVES ) )
                {
                    lines.add( field.getName() );
                    leaves++;
                }
            }
            else if ( !sharedType && ( nesting > 0 || isContent( fieldType ) ) )
            {
                final String repeated = field.getArgument( QueryLimits.FIRST_ARG ) != null ? "(first: " + EXAMPLE_FIRST + ")" : "";
                lines.add( field.getName() + repeated + " " + selection( fieldType, ownPrefix, nesting - 1, indent + "  " ) );
            }
        }
        if ( lines.isEmpty() )
        {
            lines.add( "__typename" );
        }
        return block( lines, indent );
    }

    private static boolean isContent( final GraphQLType type )
    {
        if ( type instanceof GraphQLInterfaceType interfaceType )
        {
            return CONTENT_INTERFACE.equals( interfaceType.getName() );
        }
        return type instanceof GraphQLObjectType objectType &&
            objectType.getInterfaces().stream().anyMatch( contentInterface -> CONTENT_INTERFACE.equals( contentInterface.getName() ) );
    }

    private static String block( final List<String> lines, final String indent )
    {
        final String closing = indent.length() >= 2 ? indent.substring( 2 ) : "";
        return lines.stream().map( line -> indent + line ).collect( Collectors.joining( "\n", "{\n", "\n" + closing + "}" ) );
    }

    private static String summary( final Resolved resolved, final Sdl sdl )
    {
        final String what = resolved.contentType() != null
            ? "Content type " + resolved.contentType() + " is GraphQL type " + resolved.type().getName()
            : "GraphQL type " + resolved.type().getName();
        return what + "; SDL of " + sdl.text().length() + " chars" +
            ( sdl.omitted().isEmpty() ? "" : ", " + sdl.omitted().size() + " nested types omitted" ) + ".";
    }

    private record Resolved(String contentType, GraphQLNamedType type)
    {
    }

    record Sdl(String text, List<String> omitted, List<String> notExpanded)
    {
    }
}
