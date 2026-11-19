package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.mcp.McpTool;
import com.enonic.app.guillotine.mcp.shape.ContentKeys;
import com.enonic.app.guillotine.mcp.shape.GraphQlErrors;
import com.enonic.app.guillotine.mcp.shape.McpArgs;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.McpResult;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ResultBudget;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.content.CmsFormFragmentService;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;

public final class GetTool
    implements McpTool
{
    static final String QUERY_TEMPLATE = "query McpGet($project: String, $branch: String, $key: ID) {" +
        " guillotine(project: $project, branch: $branch) { get(key: $key) {" +
        " %s _name language createdTime valid dataAsJson } } }";

    static final String QUERY = QUERY_TEMPLATE.formatted( ContentSummary.fields( false ) );

    static final String QUERY_WITH_PRINCIPALS = QUERY_TEMPLATE.formatted( ContentSummary.fields( true ) + " creator { value }" );

    static final int MAX_FIELDS = 50;

    private static final Pattern FIELD = Pattern.compile( "[A-Za-z0-9_\\-]+(\\.[A-Za-z0-9_\\-]+)*" );

    private static final String FIELDS_HINT = "Pass fields with the names of the replaced data fields to read them in full.";

    private final ContentGraphQl graphQl;

    private final ProjectBranchResolver resolver;

    private final GuillotineConfigService configService;

    private final ContentTypeService contentTypeService;

    private final CmsFormFragmentService formFragmentService;

    public GetTool( final ContentGraphQl graphQl, final ProjectBranchResolver resolver, final GuillotineConfigService configService,
                    final ContentTypeService contentTypeService, final CmsFormFragmentService formFragmentService )
    {
        this.graphQl = graphQl;
        this.resolver = resolver;
        this.configService = configService;
        this.contentTypeService = contentTypeService;
        this.formFragmentService = formFragmentService;
    }

    @Override
    public String name()
    {
        return "get";
    }

    @Override
    public Map<String, Object> execute( final Map<String, Object> args )
    {
        final ProjectBranch target = resolver.resolve( args );
        final String key = ContentKeys.key( args.get( "key" ) );
        final List<String> fields = fields( args.get( "fields" ) );

        final Map<String, Object> response = graphQl.execute( target, ContentGraphQl.canReadPrincipals() ? QUERY_WITH_PRINCIPALS : QUERY,
                                                              Map.of( "key", key ) );
        final Map<String, Object> node = ContentGraphQl.field( response, "get" );
        if ( node == null )
        {
            if ( ContentGraphQl.errors( response ).isEmpty() )
            {
                throw new ToolError( "Content " + McpJson.write( key ) + " not found in project '" + target.project() + "' on branch '" +
                                         target.branch() + "'.",
                                     "Pass an _id or _path returned by search, e.g. /articles/my-article; " +
                                         "master holds only published content." );
            }
            return ContentGraphQl.failure( response, target );
        }

        final String id = String.valueOf( node.get( "_id" ) );
        final Map<String, Object> content = ContentSummary.of( node, graphQl.workflowStates( target, List.of( id ) ).get( id ) );
        content.put( "_name", node.get( "_name" ) );
        content.put( "language", node.get( "language" ) );
        content.put( "createdTime", node.get( "createdTime" ) );
        content.put( "creator", node.get( "creator" ) instanceof Map<?, ?> creator ? creator.get( "value" ) : null );
        content.put( "valid", node.get( "valid" ) );

        final Map<String, Object> data =
            node.get( "dataAsJson" ) instanceof Map<?, ?> map ? McpJson.readObject( McpJson.write( map ) ) : new LinkedHashMap<>();
        final List<Map<String, Object>> links = new ArrayList<>();
        for ( final String htmlPath : htmlAreaPaths( String.valueOf( node.get( "type" ) ) ) )
        {
            convertRichText( data, htmlPath.split( "\\." ), 0, links );
        }
        resolveLinks( target, links );

        final Map<String, Object> structured = target.putInto( new LinkedHashMap<>() );
        structured.put( "content", content );
        final String summary;
        final ResultBudget budget;
        if ( fields != null )
        {
            final Map<String, Object> projected = new LinkedHashMap<>();
            final List<String> missing = new ArrayList<>();
            for ( final String field : fields )
            {
                final Object value = valueAt( data, field.split( "\\." ), 0 );
                if ( value != null )
                {
                    projected.put( field, value );
                }
                else
                {
                    missing.add( field );
                }
            }
            content.put( "data", projected );
            structured.put( "missingFields", missing.isEmpty() ? null : missing );
            summary = describe( content ) + ( missing.isEmpty() ? "" : " Fields not set: " + String.join( ", ", missing ) + "." );
            budget = ResultBudget.fromConfig( configService ).withNarrowHint( "Request fewer fields." );
        }
        else
        {
            final List<String> omitted = fitData( data, configService.getMcpMaxResultChars() * 4 / 5 );
            content.put( "data", data );
            structured.put( "omittedFields", omitted.isEmpty() ? null : omitted );
            final String note = omitted.isEmpty() ? null : "Large fields omitted: " + String.join( ", ", omitted ) +
                "; pass fields to read them.";
            structured.put( ResultBudget.NOTE, note );
            summary = describe( content ) + ( note == null ? "" : " " + note );
            // without fields, one large data field must not crowd out the rest; fields asked for by name get the whole budget
            budget = ResultBudget.fromConfig( configService, "data.*" ).withNarrowHint( FIELDS_HINT );
        }

        final List<Map<String, Object>> errors = GraphQlErrors.sanitize( ContentGraphQl.errors( response ) );
        if ( !errors.isEmpty() )
        {
            structured.put( McpResult.ERRORS, errors );
        }
        return McpResult.success( summary, structured, budget );
    }

    /**
     * Drops top-level data fields, largest first, until the rest fits {@code maxChars}; returns the dropped names in data order.
     */
    private static List<String> fitData( final Map<String, Object> data, final int maxChars )
    {
        final Set<String> omitted = new LinkedHashSet<>();
        while ( !data.isEmpty() && McpJson.length( data ) > maxChars )
        {
            String largest = null;
            int largestLength = -1;
            for ( final Map.Entry<String, Object> entry : data.entrySet() )
            {
                final int length = McpJson.length( entry.getValue() );
                if ( length > largestLength )
                {
                    largest = entry.getKey();
                    largestLength = length;
                }
            }
            data.remove( largest );
            omitted.add( largest );
        }
        return new ArrayList<>( omitted );
    }

    private void resolveLinks( final ProjectBranch target, final List<Map<String, Object>> links )
    {
        final Set<String> ids = new LinkedHashSet<>();
        links.forEach( link -> {
            if ( link.get( "id" ) instanceof String id && ids.size() < ContentGraphQl.MAX_LABELS )
            {
                ids.add( id );
            }
        } );
        final Map<String, Map<String, Object>> labels = graphQl.labels( target, ids );
        for ( final Map<String, Object> link : links )
        {
            if ( link.get( "id" ) instanceof String id && ids.contains( id ) )
            {
                final Map<String, Object> label = labels.get( id );
                if ( label != null )
                {
                    link.putAll( label );
                }
                else
                {
                    link.put( "missing", true );
                }
            }
        }
    }

    private static void convertRichText( final Object node, final String[] path, final int index, final List<Map<String, Object>> links )
    {
        if ( node instanceof List<?> list )
        {
            list.forEach( item -> convertRichText( item, path, index, links ) );
            return;
        }
        if ( !( node instanceof Map<?, ?> map ) )
        {
            return;
        }
        final Map<String, Object> object = McpArgs.castMap( map );
        final Object value = object.get( path[index] );
        if ( index < path.length - 1 )
        {
            convertRichText( value, path, index + 1, links );
            return;
        }
        if ( value instanceof String html )
        {
            object.put( path[index], richText( html, links ) );
        }
        else if ( value instanceof List<?> values )
        {
            object.put( path[index],
                        values.stream().map( item -> item instanceof String html ? richText( html, links ) : item ).toList() );
        }
    }

    private static Map<String, Object> richText( final String html, final List<Map<String, Object>> links )
    {
        final Map<String, Object> converted = RichText.convert( html );
        if ( converted.get( "links" ) instanceof List<?> list )
        {
            list.forEach( link -> links.add( McpArgs.castMap( (Map<?, ?>) link ) ) );
        }
        return converted;
    }

    private List<String> htmlAreaPaths( final String type )
    {
        final ContentType contentType;
        try
        {
            contentType = contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.from( type ) ) );
        }
        catch ( IllegalArgumentException e )
        {
            return List.of();
        }
        if ( contentType == null || contentType.getForm() == null )
        {
            return List.of();
        }
        final Form inlined = formFragmentService != null ? formFragmentService.inlineFormItems( contentType.getForm() ) : null;
        final List<String> paths = new ArrayList<>();
        collectHtmlAreas( inlined != null ? inlined : contentType.getForm(), "", paths );
        return paths;
    }

    private static void collectHtmlAreas( final Iterable<? extends FormItem> items, final String prefix, final List<String> paths )
    {
        for ( final FormItem item : items )
        {
            if ( item instanceof Input input && InputTypeName.HTML_AREA.equals( input.getInputType() ) )
            {
                paths.add( prefix + input.getName() );
            }
            else if ( item instanceof FormItemSet set )
            {
                collectHtmlAreas( set, prefix + set.getName() + ".", paths );
            }
            else if ( item instanceof FieldSet fieldSet )
            {
                collectHtmlAreas( fieldSet, prefix, paths );
            }
            else if ( item instanceof FormOptionSet optionSet )
            {
                for ( final FormOptionSetOption option : optionSet )
                {
                    collectHtmlAreas( option, prefix + optionSet.getName() + "." + option.getName() + ".", paths );
                }
            }
        }
    }

    /**
     * The value at {@code path}; inside a multi-occurrence item set it is the list of the values found in its occurrences.
     */
    private static Object valueAt( final Object node, final String[] path, final int index )
    {
        if ( index == path.length )
        {
            return node;
        }
        if ( node instanceof List<?> list )
        {
            final List<Object> values = list.stream().map( item -> valueAt( item, path, index ) ).filter( Objects::nonNull ).toList();
            return values.isEmpty() ? null : values;
        }
        return node instanceof Map<?, ?> map ? valueAt( map.get( path[index] ), path, index + 1 ) : null;
    }

    private static List<String> fields( final Object value )
    {
        if ( value == null || value instanceof List<?> empty && empty.isEmpty() )
        {
            return null;
        }
        if ( !( value instanceof List<?> list ) || list.size() > MAX_FIELDS )
        {
            throw new ToolError( "Invalid fields: expected a list of up to " + MAX_FIELDS + " data field names.",
                                 "Call describe-type to see the data fields of a type." );
        }
        final List<String> result = new ArrayList<>();
        for ( final Object item : list )
        {
            String field = item instanceof String text ? text : null;
            if ( field != null && field.startsWith( "data." ) )
            {
                field = field.substring( "data.".length() );
            }
            if ( field == null || !FIELD.matcher( field ).matches() )
            {
                throw new ToolError( "Invalid field: " + McpJson.write( item ) + ".",
                                     "Use data field names such as \"title\" or \"set.body\" (call describe-type to see them)." );
            }
            result.add( field );
        }
        return result;
    }

    private static String describe( final Map<String, Object> content )
    {
        return content.get( "displayName" ) + " (" + content.get( "type" ) + ") at " + content.get( "_path" ) + ".";
    }
}
