package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;

/**
 * One content item by id or path: its metadata, workflow state and data fields. Rich text becomes {@code {text, links}} with each
 * internal link target's display name and path; reference fields carry their target's display name and path. Page and x-data are left
 * to {@code query}.
 */
public final class GetTool
    implements McpTool
{
    static final String KEY_ARG = "key";

    static final String DATA = "data";

    static final String QUERY = "query McpGet($key: ID) { guillotine { get(key: $key) { _id _path _name displayName type language " +
        "createdTime modifiedTime publish { from to first } dataAsJson } } }";

    private final SchemaProvider schemaProvider;

    private final ContentTypeService contentTypeService;

    private final GuillotineConfigService configService;

    private final ProjectBranchResolver resolver;

    private final ContentLookup lookup;

    public GetTool( final SchemaProvider schemaProvider, final ContentService contentService, final ContentTypeService contentTypeService,
                    final GuillotineConfigService configService, final ProjectBranchResolver resolver )
    {
        this.schemaProvider = schemaProvider;
        this.contentTypeService = contentTypeService;
        this.configService = configService;
        this.resolver = resolver;
        this.lookup = new ContentLookup( schemaProvider, contentService );
    }

    @Override
    public McpGroup group()
    {
        return McpGroup.CONTENT;
    }

    @Override
    public String name()
    {
        return "get";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final String key = ContentKeys.key( args.get( KEY_ARG ), KEY_ARG );
        final ProjectBranch target = resolver.resolve( args );

        final Map<String, Object> variables = new LinkedHashMap<>();
        variables.put( KEY_ARG, key );
        final ContentLookup.Response response = lookup.execute( target, QUERY, variables );
        final Map<?, ?> node = response.object( "get" );
        if ( node == null )
        {
            throw new ToolError( "No content at " + McpJson.preview( key ) + " in project " + target.project() + " on branch " +
                                     target.branch() + " that the caller can read.",
                                 "Pass an _id or _path from search, such as /articles/article-01; master holds published content only." );
        }

        final String type = String.valueOf( node.get( "type" ) );
        final DataFieldScope scope = scope( type );
        final List<String> fields = new ArrayList<>( new LinkedHashSet<>(
            ( args.get( "fields" ) instanceof List<?> list ? list : List.of() ).stream()
                .map( field -> scope.resolveDataField( String.valueOf( field ), "fields" ) )
                .toList() ) );

        final Map<String, Object> data = node.get( "dataAsJson" ) instanceof Map<?, ?> map ? McpJson.readObject( McpJson.write( map ) )
            : new LinkedHashMap<>();
        final DataValues values = new DataValues( scope );
        final Map<String, Object> shown;
        if ( fields.isEmpty() )
        {
            values.collectTree( data, type );
            shown = data;
        }
        else
        {
            shown = values.project( data, fields, type );
        }
        final List<Map<String, Object>> links = new ArrayList<>();
        if ( fields.isEmpty() )
        {
            scope.richTextPaths( type )
                .forEach( path -> DataValues.replaceAt( shown, DataValues.parts( path ), 0, html -> richText( html, links ) ) );
        }
        else
        {
            List.copyOf( shown.keySet() )
                .forEach( name -> DataValues.below( shown, name, scope.richTextPaths( type ), html -> richText( html, links ) ) );
        }

        final Set<String> ids = new LinkedHashSet<>( values.referencedIds() );
        links.forEach( link -> {
            if ( link.get( "id" ) instanceof String id )
            {
                ids.add( id );
            }
        } );
        final Map<String, Map<String, Object>> labels = lookup.labels( target, ids );
        final Set<String> looked = new LinkedHashSet<>( ids.stream().limit( ContentLookup.MAX_LABELS ).toList() );
        if ( fields.isEmpty() )
        {
            values.labelTree( shown, type, labels, looked );
        }
        else
        {
            values.labelProjected( shown, type, labels, looked );
        }
        for ( final Map<String, Object> link : links )
        {
            if ( link.get( "id" ) instanceof String id && looked.contains( id ) )
            {
                if ( labels.containsKey( id ) )
                {
                    link.putAll( labels.get( id ) );
                }
                else
                {
                    link.put( ContentLookup.MISSING, true );
                }
            }
        }

        final String id = String.valueOf( node.get( ContentLookup.ID ) );
        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        for ( final String field : List.of( ContentLookup.ID, ContentLookup.PATH, "_name", ContentLookup.DISPLAY_NAME, "type", "language",
                                            "createdTime", "modifiedTime" ) )
        {
            structured.put( field, node.get( field ) );
        }
        if ( node.get( "publish" ) instanceof Map<?, ?> publish && publish.values().stream().anyMatch( value -> value != null ) )
        {
            structured.put( "publish", publish );
        }
        structured.put( "workflow", lookup.workflowStates( target, List.of( id ) ).get( id ) );
        if ( ids.size() > ContentLookup.MAX_LABELS )
        {
            structured.put( ResultBudget.NOTE, "Display names were looked up for the first " + ContentLookup.MAX_LABELS + " content ids " +
                "only." );
        }
        if ( !response.errors().isEmpty() )
        {
            structured.put( McpResult.ERRORS, response.errors() );
        }
        structured.put( DATA, shown );
        return ResultBudget.fromConfig( configService )
            .withNarrowHint( "Pass fields with the data fields to read; one field alone gets the whole budget." )
            .fitSubtrees( structured, DATA );
    }

    private DataFieldScope scope( final String type )
    {
        ContentType contentType;
        try
        {
            contentType = contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.from( type ) ) );
        }
        catch ( IllegalArgumentException e )
        {
            contentType = null;
        }
        final ContentTypeIndex index =
            new ContentTypeIndex( schemaProvider.getSchema(), ContentTypeIndex.registeredNames( contentTypeService ) );
        return DataFieldScope.of( index, contentType != null ? List.of( contentType ) : List.of(),
                                  contentType != null ? List.of( contentType.getName().toString() ) : List.of() );
    }

    private static Object richText( final Object value, final List<Map<String, Object>> links )
    {
        if ( value instanceof List<?> list )
        {
            return list.stream().map( item -> richText( item, links ) ).toList();
        }
        if ( !( value instanceof String html ) )
        {
            return value;
        }
        final Map<String, Object> converted = RichText.convert( html );
        if ( converted.get( "links" ) instanceof List<?> found )
        {
            for ( final Object link : found )
            {
                @SuppressWarnings("unchecked") final Map<String, Object> entry = (Map<String, Object>) link;
                links.add( entry );
            }
        }
        return converted;
    }
}
