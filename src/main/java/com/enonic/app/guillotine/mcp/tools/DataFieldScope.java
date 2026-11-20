package com.enonic.app.guillotine.mcp.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.content.ContentType;

/**
 * The data fields of a set of content types, for reading field arguments: a field is named by its index path ({@code data.title}),
 * by that path without {@code data.}, or by its GraphQL name, and resolves to the index path. Names that are not data fields pass
 * through unchanged, since index fields such as {@code modifiedTime} or {@code x.<app>.<mixin>.<field>} are not in any form.
 */
final class DataFieldScope
{
    static final String DATA_PREFIX = "data.";

    static final int MAX_LISTED = 40;

    /**
     * First elements of index paths that are not data fields, so a bare name like {@code language} keeps meaning the index field.
     */
    private static final Set<String> INDEX_ROOTS =
        Set.of( "_id", "_name", "_path", "_parentpath", "_references", "_alltext", "_score", "_ts", "_state", "_nodetype", "type",
                "displayname", "language", "valid", "creator", "modifier", "owner", "createdtime", "modifiedtime", "archivedtime",
                "archivedby", "publish", "workflow", "x", "page", "components", "attachment", "site", "data", "originproject", "inherit",
                "childorder", "originalname", "originalparentpath" );

    private final Map<String, TypeFields.Field> byPath = new LinkedHashMap<>();

    private final Map<String, String> byName = new LinkedHashMap<>();

    private final Set<String> types = new LinkedHashSet<>();

    private final Map<String, Set<String>> declaring = new HashMap<>();

    private final Map<String, Set<String>> references = new HashMap<>();

    private final Map<String, Set<String>> richText = new HashMap<>();

    private final String description;

    private DataFieldScope( final String description )
    {
        this.description = description;
    }

    /**
     * The data fields of {@code contentTypes}, or of every registered content type when it is {@code null}.
     */
    static DataFieldScope of( final ContentTypeIndex index, final Collection<ContentType> registered, final Collection<String> contentTypes )
    {
        final DataFieldScope scope =
            new DataFieldScope( contentTypes == null ? "any content type" : contentTypes.isEmpty() ? "the content type"
                : String.join( ", ", contentTypes ) );
        for ( final ContentType contentType : registered )
        {
            final String name = contentType.getName().toString();
            if ( contentTypes == null || contentTypes.contains( name ) )
            {
                scope.types.add( name );
                TypeFields.of( contentType, index.byContentType( name ) ).forEach( field -> scope.add( name, field ) );
            }
        }
        return scope;
    }

    private void add( final String contentType, final TypeFields.Field field )
    {
        byPath.putIfAbsent( field.path(), field );
        declaring.computeIfAbsent( field.path(), path -> new LinkedHashSet<>() ).add( contentType );
        if ( field.isReference() )
        {
            references.computeIfAbsent( contentType, type -> new HashSet<>() ).add( field.path() );
        }
        if ( InputTypeName.HTML_AREA.toString().equals( field.input() ) )
        {
            richText.computeIfAbsent( contentType, type -> new HashSet<>() ).add( field.path() );
        }
        byName.putIfAbsent( field.path().substring( DATA_PREFIX.length() ), field.path() );
        byName.putIfAbsent( field.name(), field.path() );
    }

    /**
     * Whether none of the types searched is installed, so their fields are unknown and {@code data.*} names cannot be checked.
     */
    boolean isUnknown()
    {
        return types.isEmpty();
    }

    /**
     * The index path {@code field} names. A path under {@code data.} that is not a data field of the scope is a {@link ToolError} that
     * lists the valid ones; {@code where} names the argument.
     */
    String resolve( final String field, final String where )
    {
        if ( field.startsWith( DATA_PREFIX ) )
        {
            final String path = dataPath( field.substring( DATA_PREFIX.length() ) );
            if ( path != null || isUnknown() )
            {
                return path != null ? path : field;
            }
            throw unknown( field, where );
        }
        final String root = field.split( "[.^]", 2 )[0].toLowerCase( Locale.ROOT );
        final String path = INDEX_ROOTS.contains( root ) ? null : dataPath( field );
        return path != null ? path : field;
    }

    /**
     * The index path of a data field argument, for arguments that take only data fields.
     */
    String resolveDataField( final String field, final String where )
    {
        final String name = field.startsWith( DATA_PREFIX ) ? field.substring( DATA_PREFIX.length() ) : field;
        final String path = dataPath( name );
        if ( path == null )
        {
            throw unknown( field, where );
        }
        return path;
    }

    boolean isDataField( final String path )
    {
        return byPath.containsKey( path );
    }

    /**
     * Whether any content type in scope declares {@code path} as a content, image or media selector.
     */
    boolean isReference( final String path )
    {
        return references.values().stream().anyMatch( paths -> paths.contains( path ) );
    }

    /**
     * The types in scope that declare {@code path} as a selector, in registration order.
     */
    List<String> referenceTypes( final String path )
    {
        return declaring.getOrDefault( path, Set.of() ).stream().filter( type -> referencePaths( type ).contains( path ) ).toList();
    }

    /**
     * Whether every type in scope that has {@code path} declares it as a selector, so that any value of it is a content id.
     */
    boolean isReferenceEverywhere( final String path )
    {
        final Set<String> types = declaring.getOrDefault( path, Set.of() );
        return !types.isEmpty() && referenceTypes( path ).size() == types.size();
    }

    /**
     * The selector paths that {@code contentType} declares; types sharing a field name may give it different input types.
     */
    Set<String> referencePaths( final String contentType )
    {
        return references.getOrDefault( contentType, Set.of() );
    }

    Set<String> richTextPaths( final String contentType )
    {
        return richText.getOrDefault( contentType, Set.of() );
    }

    private String dataPath( final String name )
    {
        if ( byPath.containsKey( DATA_PREFIX + name ) )
        {
            return DATA_PREFIX + name;
        }
        return byName.get( name );
    }

    private ToolError unknown( final String field, final String where )
    {
        final String message = "Invalid " + where + ": " + McpJson.preview( field ) + " is not a data field of " + description + ".";
        if ( isUnknown() )
        {
            return new ToolError( message, "No installed content type matches types, so its fields are unknown; call list-types." );
        }
        if ( byPath.isEmpty() )
        {
            return new ToolError( message, "These types have no data fields; filter on index fields such as displayName or modifiedTime." );
        }
        final List<String> paths = List.copyOf( byPath.keySet() );
        final String listed = String.join( ", ", paths.subList( 0, Math.min( paths.size(), MAX_LISTED ) ) ) +
            ( paths.size() > MAX_LISTED ? " and " + ( paths.size() - MAX_LISTED ) + " more" : "" );
        return new ToolError( message, "Data fields: " + listed + ". Call describe-type for the fields of one type." );
    }
}
