package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeUtil;

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
import com.enonic.xp.form.FormItem;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;

/**
 * What a model needs to query one content type: its GraphQL type as compact SDL, each data field with its index path, form label and
 * help text, input type and selector targets, the index fields the schema does not show, fields other applications add, and three
 * ready {@code query} documents. The type is named by content type name, GraphQL type name, or a local name that only one content type
 * has.
 */
public final class DescribeTypeTool
    implements McpTool
{
    static final String TYPE_ARG = "type";

    static final String FIELDS = "fields";

    static final int MAX_SDL_CHARS = 4000;

    static final String COUNT_QUERY = "query McpCountType($type: String!) { guillotine { queryDslConnection(query: " +
        "{term: {field: \"type\", value: {string: $type}}}, first: 0) { totalCount } } }";

    /**
     * Index fields every content item has but the GraphQL schema does not show, as their paths read in filters, sorts and
     * aggregations.
     */
    static final Map<String, String> INDEX_FIELDS = indexFields( false );

    static final Map<String, String> ANONYMOUS_INDEX_FIELDS = indexFields( true );

    static final String APP_DEFINED_NOTE = "Added by another application: runs its code, which may apply its own access rules and " +
        "limits; check counts with queryDslConnection.";

    /**
     * Room kept for the {@code truncated} flag and the {@code hint} that a cut list adds.
     */
    private static final int HINT_RESERVE = 300;

    private static final int MAX_SUGGESTIONS = 5;

    private static final int MAX_APP_FIELDS = 20;

    private final SchemaProvider schemaProvider;

    private final ContentTypeService contentTypeService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    public DescribeTypeTool( final SchemaProvider schemaProvider, final ContentTypeService contentTypeService,
                             final GuillotineConfigService configService, final ProjectBranchResolver resolver )
    {
        this.schemaProvider = schemaProvider;
        this.contentTypeService = contentTypeService;
        this.configService = configService;
        this.resolver = resolver;
    }

    @Override
    public McpGroup group()
    {
        return McpGroup.CONTENT;
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
            throw new ToolError( "type is required.", "Pass a content type name from list-types, such as \"com.example.app:article\"." );
        }
        final ContentTypeIndex index = new ContentTypeIndex( schemaProvider.getSchema(), ContentTypeIndex.registeredNames( contentTypeService ) );
        final String contentType = resolve( index, requested.trim() );
        final GraphQLObjectType type = index.byContentType( contentType );
        final ProjectBranch target = resolver.resolve( args );
        final long count = count( target, contentType );

        final ContentType schema = contentTypeSchema( contentType );
        final GraphQLObjectType dataType = TypeFields.dataType( type );
        final List<TypeFields.Field> fields = TypeFields.of( schema, type );

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( "contentType", contentType );
        structured.put( "title", schema != null ? schema.getTitle() : null );
        structured.put( "graphqlType", type.getName() );
        structured.put( "count", count );
        structured.put( "sdl", TypeSdl.print( index, type, Math.min( MAX_SDL_CHARS, configService.getMcpMaxResultChars() / 4 ) ) );
        structured.put( FIELDS, fields.stream().map( TypeFields.Field::toMap ).toList() );
        final List<String> appFields = appFields( index, type, dataType );
        if ( !appFields.isEmpty() )
        {
            structured.put( "appFields", appFields );
            structured.put( "appFieldsNote", APP_DEFINED_NOTE );
        }
        structured.put( "indexFields", ProjectBranchResolver.isAuthenticated() ? INDEX_FIELDS : ANONYMOUS_INDEX_FIELDS );
        structured.put( "examples", TypeExamples.of( contentType, type.getName(), fields ) );
        final ResultBudget budget = ResultBudget.fromConfig( configService );
        final Map<String, Object> withoutFields = new LinkedHashMap<>( structured );
        withoutFields.put( FIELDS, List.of() );
        if ( McpJson.length( withoutFields ) + HINT_RESERVE > budget.maxResultChars() )
        {
            throw new ToolError( "The description of " + contentType + " does not fit mcp.maxResultChars (" + budget.maxResultChars() +
                                     ").", "Do not retry; ask the administrator to raise mcp.maxResultChars in " +
                "com.enonic.app.guillotine.cfg." );
        }
        final String fieldNames = dataType != null
            ? " For every data field name, run query with { __type(name: \"" + dataType.getName() + "\") { fields { name } } }."
            : "";
        return budget.withNarrowHint( "Fields left out may be missing from the SDL too." + fieldNames ).fitList( structured, FIELDS );
    }

    private static String resolve( final ContentTypeIndex index, final String requested )
    {
        if ( index.byContentType( index.spelling( requested ) ) != null )
        {
            return index.spelling( requested );
        }
        final String byGraphQlName = index.contentTypeOf( requested );
        if ( byGraphQlName != null )
        {
            return byGraphQlName;
        }
        if ( !requested.contains( ":" ) )
        {
            final List<String> matches = index.contentTypes()
                .stream()
                .filter( name -> name.substring( name.indexOf( ':' ) + 1 ).equalsIgnoreCase( requested ) )
                .toList();
            if ( matches.size() == 1 )
            {
                return matches.get( 0 );
            }
            if ( matches.size() > 1 )
            {
                throw new ToolError( "Type '" + McpJson.shorten( requested, McpJson.PREVIEW_CHARS ) + "' is ambiguous: " +
                                         String.join( ", ", matches.stream().limit( MAX_SUGGESTIONS ).toList() ) + ".",
                                     "Pass the full content type name." );
            }
        }
        final String needle = requested.substring( requested.indexOf( ':' ) + 1 ).toLowerCase( Locale.ROOT );
        final List<String> similar = index.contentTypes()
            .stream()
            .filter( name -> !needle.isEmpty() && name.toLowerCase( Locale.ROOT ).contains( needle ) )
            .limit( MAX_SUGGESTIONS )
            .toList();
        throw new ToolError( "Unknown content type '" + McpJson.shorten( requested, McpJson.PREVIEW_CHARS ) + "'.",
                             ( similar.isEmpty() ? "" : "Similar: " + String.join( ", ", similar ) + ". " ) +
                                 "Call list-types for the content types in use and pass one of their names." );
    }

    private long count( final ProjectBranch target, final String contentType )
    {
        final Map<String, Object> response = target.callWith(
            () -> schemaProvider.executeToSpecification( COUNT_QUERY, Map.of( "type", contentType ), null,
                                                         GraphQlErrors.hideInternalErrors() ) );
        final Object data = response.get( QueryTool.DATA );
        GraphQlErrors.partialErrors( data, response.get( McpResult.ERRORS ) instanceof List<?> list ? list : List.of() );
        if ( data instanceof Map<?, ?> root && root.get( "guillotine" ) instanceof Map<?, ?> guillotine &&
            guillotine.get( "queryDslConnection" ) instanceof Map<?, ?> connection &&
            connection.get( "totalCount" ) instanceof Number total )
        {
            return total.longValue();
        }
        return 0;
    }

    private ContentType contentTypeSchema( final String contentType )
    {
        return contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.from( contentType ) ) );
    }

    /**
     * Fields of the type and its data that run another application's code, and fields of {@code guillotine} whose resolver does and
     * that return this type.
     */
    private static List<String> appFields( final ContentTypeIndex index, final GraphQLObjectType type, final GraphQLObjectType dataType )
    {
        final List<String> result = new ArrayList<>();
        for ( final GraphQLFieldDefinition field : type.getFieldDefinitions() )
        {
            if ( index.runsAppCode( type, field ) ||
                ( TypeSdl.isAppAdded( field ) && !isContentField( index, field ) ) )
            {
                result.add( field.getName() );
            }
        }
        if ( dataType != null )
        {
            dataType.getFieldDefinitions()
                .stream()
                .filter( field -> index.runsAppCode( dataType, field ) )
                .forEach( field -> result.add( "data." + field.getName() ) );
        }
        if ( index.schema().getType( "HeadlessCms" ) instanceof GraphQLFieldsContainer guillotine )
        {
            guillotine.getFieldDefinitions()
                .stream()
                .filter( field -> GraphQLTypeUtil.unwrapAll( field.getType() ) == type && index.runsAppCode( guillotine, field ) )
                .forEach( field -> result.add( "guillotine." + field.getName() ) );
        }
        return result.size() > MAX_APP_FIELDS ? result.subList( 0, MAX_APP_FIELDS ) : result;
    }

    private static boolean isContentField( final ContentTypeIndex index, final GraphQLFieldDefinition field )
    {
        return index.schema().getType( ContentTypeIndex.CONTENT ) instanceof GraphQLFieldsContainer content &&
            content.getFieldDefinition( field.getName() ) != null;
    }

    private static Map<String, String> indexFields( final boolean anonymous )
    {
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put( "type", "content type name" );
        fields.put( "_path, _parentPath", "path with /content in front" );
        fields.put( "_references", "_id of each item this one references" );
        fields.put( "workflow.state", "IN_PROGRESS, PENDING_APPROVAL, REJECTED or READY; may be unset, so match ready items by " +
            "excluding the other three; lower case in buckets" );
        fields.put( "creator, modifier, owner", "principal key, e.g. user:system:alice" +
            ( anonymous ? "; filters, sorts and aggregations on them need a signed-in caller" : "" ) );
        fields.put( "createdTime, modifiedTime, publish.from, publish.to, publish.first", "instant" );
        return fields;
    }
}
