package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.GraphQlErrors;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;

/**
 * The content types that have content the caller can read in one project and branch, most used first, with the GraphQL type their
 * content has in {@code query} results.
 */
public final class ListTypesTool
    implements McpTool
{
    static final int MAX_TYPES = 1000;

    static final String QUERY = "query McpListTypes { guillotine { queryDslConnection(query: {matchAll: {}}, first: 0, " +
        "aggregations: [{name: \"types\", terms: {field: \"type\", size: " + MAX_TYPES + ", order: \"_count DESC\"}}]) " +
        "{ totalCount aggregationsAsJson } } }";

    private static final String TYPES = "types";

    private final SchemaProvider schemaProvider;

    private final ContentTypeService contentTypeService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    public ListTypesTool( final SchemaProvider schemaProvider, final ContentTypeService contentTypeService,
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
        return "list-types";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        return fit( typesInUse( args ) );
    }

    Map<String, Object> fit( final Map<String, Object> structured )
    {
        return ResultBudget.fromConfig( configService )
            .withNarrowHint( "Count one type with query and a term filter on the field type." )
            .fitList( structured, TYPES );
    }

    Map<String, Object> typesInUse( final Map<String, Object> args )
    {
        final ProjectBranch target = resolver.resolve( args );
        final Map<String, Object> response =
            target.callWith( () -> schemaProvider.executeToSpecification( QUERY, Map.of(), null, GraphQlErrors.hideInternalErrors() ) );
        final Object data = response.get( QueryTool.DATA );
        final List<Map<String, Object>> errors =
            GraphQlErrors.partialErrors( data, response.get( McpResult.ERRORS ) instanceof List<?> list ? list : List.of() );

        final Map<String, Object> connection = connection( data );
        final ContentTypeIndex index = new ContentTypeIndex( schemaProvider.getSchema(), ContentTypeIndex.registeredNames( contentTypeService ) );
        final List<Map<String, Object>> types = new ArrayList<>();
        for ( final Object item : buckets( connection ) )
        {
            if ( item instanceof Map<?, ?> bucket && bucket.get( "key" ) instanceof String key )
            {
                final String name = index.spelling( key );
                final Map<String, Object> type = new LinkedHashMap<>();
                type.put( "name", name );
                type.put( "title", title( name ) );
                type.put( "graphqlType", index.graphQlTypeName( name ) );
                type.put( "count", bucket.get( "docCount" ) instanceof Number number ? number.longValue() : 0L );
                types.add( type );
            }
        }
        types.sort( Comparator.comparingLong( ( Map<String, Object> type ) -> (Long) type.get( "count" ) )
                        .reversed()
                        .thenComparing( type -> (String) type.get( "name" ) ) );

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( ResultBudget.TOTAL, connection.get( "totalCount" ) instanceof Number number ? number.longValue() : 0L );
        structured.put( TYPES, types );
        if ( types.size() >= MAX_TYPES )
        {
            structured.put( ResultBudget.NOTE, "Only the " + MAX_TYPES + " most used content types are listed." );
        }
        if ( !errors.isEmpty() )
        {
            structured.put( McpResult.ERRORS, errors );
        }
        return structured;
    }

    private static Map<String, Object> connection( final Object data )
    {
        if ( data instanceof Map<?, ?> root && root.get( "guillotine" ) instanceof Map<?, ?> guillotine &&
            guillotine.get( "queryDslConnection" ) instanceof Map<?, ?> connection )
        {
            final Map<String, Object> result = new LinkedHashMap<>();
            connection.forEach( ( key, value ) -> result.put( String.valueOf( key ), value ) );
            return result;
        }
        return Map.of();
    }

    private static List<?> buckets( final Map<String, Object> connection )
    {
        if ( connection.get( "aggregationsAsJson" ) instanceof Map<?, ?> aggregations &&
            aggregations.get( TYPES ) instanceof Map<?, ?> byType && byType.get( "buckets" ) instanceof List<?> buckets )
        {
            return buckets;
        }
        return List.of();
    }

    private String title( final String name )
    {
        try
        {
            final ContentType type = contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.from( name ) ) );
            return type != null ? type.getTitle() : null;
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }
    }
}
