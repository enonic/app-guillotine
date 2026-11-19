package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.GuillotineConfigService;
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

public final class ListTypesTool
    implements McpTool
{
    static final int MAX_TYPES = 1000;

    static final String QUERY = "query McpListTypes($project: String, $branch: String) {" +
        " guillotine(project: $project, branch: $branch) {" +
        " queryDslConnection(query: {matchAll: {}}, first: 0," +
        " aggregations: [{name: \"types\", terms: {field: \"type\", size: " + MAX_TYPES + ", order: \"_count DESC\"}}]) {" +
        " totalCount aggregationsAsJson } } }";

    private final ContentGraphQl graphQl;

    private final ProjectBranchResolver resolver;

    private final GuillotineConfigService configService;

    private final ContentTypeService contentTypeService;

    public ListTypesTool( final ContentGraphQl graphQl, final ProjectBranchResolver resolver, final GuillotineConfigService configService,
                          final ContentTypeService contentTypeService )
    {
        this.graphQl = graphQl;
        this.resolver = resolver;
        this.configService = configService;
        this.contentTypeService = contentTypeService;
    }

    @Override
    public String name()
    {
        return "list-types";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final ProjectBranch target = resolver.resolve( args );
        final Map<String, Object> response = graphQl.execute( target, QUERY, Map.of() );
        final Map<String, Object> connection = ContentGraphQl.field( response, "queryDslConnection" );
        if ( connection == null )
        {
            return ContentGraphQl.failure( response, target );
        }

        final List<Map<String, Object>> types = new ArrayList<>();
        if ( connection.get( "aggregationsAsJson" ) instanceof Map<?, ?> aggregations &&
            aggregations.get( "types" ) instanceof Map<?, ?> byType && byType.get( "buckets" ) instanceof List<?> buckets )
        {
            for ( final Object item : buckets )
            {
                if ( item instanceof Map<?, ?> bucket && bucket.get( "key" ) instanceof String name )
                {
                    final Map<String, Object> type = new LinkedHashMap<>();
                    type.put( "name", name );
                    type.put( "displayName", displayName( name ) );
                    type.put( "count", bucket.get( "docCount" ) instanceof Number number ? number.longValue() : 0L );
                    types.add( type );
                }
            }
        }
        types.sort( Comparator.comparingLong( ( Map<String, Object> type ) -> (Long) type.get( "count" ) ).reversed()
                        .thenComparing( type -> (String) type.get( "name" ) ) );

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( "total", connection.get( "totalCount" ) );
        structured.put( "types", types );
        final List<Map<String, Object>> errors = GraphQlErrors.sanitize( ContentGraphQl.errors( response ) );
        if ( !errors.isEmpty() )
        {
            structured.put( McpResult.ERRORS, errors );
        }

        final boolean capped = types.size() >= MAX_TYPES;
        if ( capped )
        {
            structured.put( ResultBudget.NOTE, "Only the " + MAX_TYPES + " most used content types are listed." );
        }

        final int found = types.size();
        return McpResult.success( result -> summary( target, found, capped, result.get( "types" ) ), structured,
                                  ResultBudget.listOnly( configService.getMcpMaxResultChars(), "types",
                                                         "Use search with types and count 0 to count one type.", "project", "branch",
                                                         "types" ), null, null, 0 );
    }

    private static String summary( final ProjectBranch target, final int found, final boolean capped, final Object returned )
    {
        final String where = target.project() + " (" + target.branch() + ")";
        if ( found == 0 )
        {
            return "No content in " + where + ".";
        }
        final int listed = returned instanceof List<?> list ? list.size() : 0;
        final String count = ( capped ? "At least " : "" ) + found + " content types in use in " + where;
        return listed < found ? count + "; " + listed + " listed here." : count + ".";
    }

    private String displayName( final String name )
    {
        try
        {
            final ContentType type = contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.from( name ) ) );
            return type != null && type.getTitle() != null ? type.getTitle() : null;
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }
    }
}
