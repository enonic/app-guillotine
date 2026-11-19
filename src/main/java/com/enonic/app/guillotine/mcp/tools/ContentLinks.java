package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import com.enonic.app.guillotine.mcp.shape.ContentKeys;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.Mixin;
import com.enonic.xp.data.Property;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueTypes;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.index.IndexPath;
import com.enonic.xp.page.Page;
import com.enonic.xp.query.expr.CompareExpr;
import com.enonic.xp.query.expr.ConstraintExpr;
import com.enonic.xp.query.expr.FieldExpr;
import com.enonic.xp.query.expr.FieldOrderExpr;
import com.enonic.xp.query.expr.LogicalExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.query.expr.QueryExpr;
import com.enonic.xp.query.expr.ValueExpr;
import com.enonic.xp.region.Component;
import com.enonic.xp.region.DescriptorBasedComponent;
import com.enonic.xp.region.FragmentComponent;
import com.enonic.xp.region.ImageComponent;
import com.enonic.xp.region.LayoutComponent;
import com.enonic.xp.region.Region;
import com.enonic.xp.region.Regions;
import com.enonic.xp.schema.content.ContentTypeName;

/**
 * Reads content and its references in the caller's context for find-references and check-links. Every read goes through
 * {@link ContentService} with the caller's identity, so content the caller cannot read is indistinguishable from content that does
 * not exist.
 */
final class ContentLinks
{
    static final String REFERENCES = "_references";

    static final int MAX_TYPES = 20;

    static final String MISSING_REASON = "target not found on this branch, or not readable by the current user";

    static final String INVALID_REASON = "invalid reference: the value is not a content id";

    private static final int READ_BATCH = 500;

    private static final Pattern TYPE = Pattern.compile( "[A-Za-z0-9._\\-]+(:[A-Za-z0-9._\\-]+)?" );

    private static final OrderExpr BY_PATH = FieldOrderExpr.create( IndexPath.from( "_path" ), OrderExpr.Direction.ASC );

    private final ContentService contentService;

    ContentLinks( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    /**
     * One reference from a content: the target id, where it is held ({@code data.body}, {@code x.<mixin>.<field>}, {@code page},
     * {@code variantOf}) and its kind ({@code content}, {@code media}, {@code image}); {@code text} is the link text or image alt.
     */
    record Ref(String id, String field, String kind, String text)
    {
        boolean valid()
        {
            return isContentId( id );
        }
    }

    <T> T run( final ProjectBranch target, final Callable<T> callable )
    {
        try
        {
            return ProjectBranchResolver.runInProjectContext( target, callable );
        }
        catch ( ForbiddenAccessException e )
        {
            throw ContentGraphQl.accessDenied( target );
        }
    }

    FindContentIdsByQueryResult find( final ProjectBranch target, final ContentQuery query )
    {
        return run( target, () -> contentService.find( query ) );
    }

    /**
     * The content for an id or a path, or {@code null} when it does not exist or the caller cannot read it.
     */
    Content get( final ProjectBranch target, final String key )
    {
        return run( target, () -> {
            try
            {
                return key.startsWith( "/" ) ? contentService.getByPath( ContentPath.from( key ) ) : contentService.getById( contentId( key ) );
            }
            catch ( ContentNotFoundException e )
            {
                return null;
            }
        } );
    }

    /**
     * The readable content among {@code ids}, keyed by id; unknown and unreadable ids are absent.
     */
    Map<String, Content> readable( final ProjectBranch target, final Collection<String> ids )
    {
        final List<ContentId> unique =
            new LinkedHashSet<>( ids ).stream().filter( ContentLinks::isContentId ).map( ContentId::from ).toList();
        final Map<String, Content> result = new LinkedHashMap<>();
        for ( int from = 0; from < unique.size(); from += READ_BATCH )
        {
            final List<ContentId> batch = unique.subList( from, Math.min( unique.size(), from + READ_BATCH ) );
            final Iterable<Content> contents =
                run( target, () -> contentService.getByIds( new GetContentByIdsParams( ContentIds.from( batch ) ) ) );
            if ( contents != null )
            {
                contents.forEach( content -> result.put( content.getId().toString(), content ) );
            }
        }
        return result;
    }

    static Map<String, Object> summary( final Content content )
    {
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put( "_id", content.getId().toString() );
        summary.put( "_path", content.getPath().toString() );
        summary.put( "displayName", content.getDisplayName() );
        summary.put( "type", content.getType().toString() );
        return summary;
    }

    /**
     * Kind of a reference: links and images in rich text keep their own kind, selector values are named after the target type.
     */
    static String kind( final Ref ref, final Content target )
    {
        if ( ref.kind() != null )
        {
            return ref.kind();
        }
        if ( target != null && target.getType().isImageMedia() )
        {
            return "image";
        }
        return target != null && target.getType().isDescendantOfMedia() ? "media" : "content";
    }

    /**
     * Everything {@code content} references, in the order of its data: the values that XP indexes as {@code _references} (selector
     * values in data, mixins and page, the page template, rich text links collected as processed references, and the variant
     * source).
     */
    static List<Ref> outbound( final Content content )
    {
        final String self = content.getId().toString();
        final Set<String> processed = new HashSet<>();
        if ( content.getProcessedReferences() != null )
        {
            content.getProcessedReferences().forEach( id -> processed.add( id.toString() ) );
        }

        final Map<String, Ref> refs = new LinkedHashMap<>();
        collect( content.getData(), "data.", processed, refs );
        if ( content.getMixins() != null )
        {
            for ( final Mixin mixin : content.getMixins() )
            {
                collect( mixin.getData(), "x." + mixin.getName() + ".", processed, refs );
            }
        }
        final Page page = content.getPage();
        if ( page != null )
        {
            if ( page.getTemplate() != null )
            {
                add( refs, new Ref( page.getTemplate().toString(), "page.template", null, null ) );
            }
            references( page.getConfig(), "page", refs );
            components( page.getRegions(), refs );
            if ( page.getFragment() != null )
            {
                component( page.getFragment(), refs );
            }
        }
        final Set<String> found = new HashSet<>();
        refs.values().forEach( ref -> found.add( ref.id() ) );
        processed.stream().filter( id -> !found.contains( id ) ).sorted().forEach(
            id -> add( refs, new Ref( id, page != null ? "page" : "processedReferences", null, null ) ) );
        if ( content.getVariantOf() != null )
        {
            add( refs, new Ref( content.getVariantOf().toString(), "variantOf", "content", null ) );
        }

        return refs.values().stream().filter( ref -> !self.equals( ref.id() ) ).toList();
    }

    private static void collect( final PropertyTree data, final String prefix, final Set<String> processed, final Map<String, Ref> refs )
    {
        if ( data == null )
        {
            return;
        }
        for ( final Property property : data.getProperties( ValueTypes.STRING ) )
        {
            final String value = property.getString();
            if ( value == null || !value.contains( "://" ) )
            {
                continue;
            }
            for ( final Map<String, Object> link : RichText.links( value ) )
            {
                if ( link.get( "id" ) instanceof String id && processed.contains( id ) )
                {
                    add( refs, new Ref( id, prefix + property.getPath(), String.valueOf( link.get( "type" ) ),
                                        (String) link.get( "text" ) ) );
                }
            }
        }
        references( data, prefix.substring( 0, prefix.length() - 1 ), refs );
    }

    private static void references( final PropertyTree data, final String field, final Map<String, Ref> refs )
    {
        if ( data == null )
        {
            return;
        }
        for ( final Property property : data.getProperties( ValueTypes.REFERENCE ) )
        {
            if ( property.getReference() != null && !property.getReference().toString().isBlank() )
            {
                add( refs, new Ref( property.getReference().toString(), field + "." + property.getPath(), null, null ) );
            }
        }
    }

    private static void components( final Regions regions, final Map<String, Ref> refs )
    {
        if ( regions == null )
        {
            return;
        }
        for ( final Region region : regions )
        {
            region.getComponents().forEach( component -> component( component, refs ) );
        }
    }

    private static void component( final Component component, final Map<String, Ref> refs )
    {
        if ( component instanceof ImageComponent image && image.getImage() != null )
        {
            add( refs, new Ref( image.getImage().toString(), "page", "image", null ) );
        }
        else if ( component instanceof FragmentComponent fragment && fragment.getFragment() != null )
        {
            add( refs, new Ref( fragment.getFragment().toString(), "page", "content", null ) );
        }
        else if ( component instanceof DescriptorBasedComponent described )
        {
            references( described.getConfig(), "page", refs );
            if ( component instanceof LayoutComponent layout )
            {
                components( layout.getRegions(), refs );
            }
        }
    }

    private static void add( final Map<String, Ref> refs, final Ref ref )
    {
        refs.putIfAbsent( ref.id() + "\n" + ref.field() + "\n" + ref.kind(), ref );
    }

    static boolean isContentId( final String value )
    {
        try
        {
            ContentId.from( value );
            return true;
        }
        catch ( IllegalArgumentException e )
        {
            return false;
        }
    }

    private static ContentId contentId( final String key )
    {
        try
        {
            return ContentId.from( key );
        }
        catch ( IllegalArgumentException e )
        {
            throw invalidKey();
        }
    }

    private static ToolError invalidKey()
    {
        return new ToolError( "Invalid key: expected a content id or a path such as /articles/my-article.",
                              "Call search to find content and pass its _id or _path." );
    }

    /**
     * Type names as given; short names without an application ({@code article}) match that name in any application. With
     * {@code media} set, only media types are accepted and short names mean {@code media:<name>}.
     */
    static List<String> types( final Object value, final boolean media )
    {
        if ( value == null )
        {
            return List.of();
        }
        if ( !( value instanceof List<?> list ) || list.size() > MAX_TYPES )
        {
            throw new ToolError( "Invalid types: expected a list of up to " + MAX_TYPES + " content type names.",
                                 "Call list-types to see the types in the project." );
        }
        final List<String> result = new ArrayList<>();
        for ( final Object item : list )
        {
            if ( !( item instanceof String type ) || !TYPE.matcher( type ).matches() )
            {
                throw new ToolError( "Invalid type: " + McpJson.write( item ) + ".",
                                     "Use a name from list-types, e.g. \"com.example.app:article\" or the short form \"article\"." );
            }
            final String name = media && !type.contains( ":" ) ? "media:" + type : type;
            if ( media && !ContentTypeName.from( name ).isDescendantOfMedia() )
            {
                throw new ToolError( "Invalid type for unusedMedia: " + McpJson.write( type ) + " is not a media type.",
                                     "types for unusedMedia must be media types like media:image (or the short form image)." );
            }
            result.add( name );
        }
        return result;
    }

    static ConstraintExpr typeConstraint( final List<String> types )
    {
        final List<ValueExpr> fullNames = new ArrayList<>();
        ConstraintExpr result = null;
        for ( final String type : types )
        {
            if ( type.contains( ":" ) )
            {
                fullNames.add( ValueExpr.string( type ) );
            }
            else
            {
                result = or( result, CompareExpr.like( FieldExpr.from( "type" ), ValueExpr.string( "*:" + type ) ) );
            }
        }
        if ( !fullNames.isEmpty() )
        {
            result = or( CompareExpr.in( FieldExpr.from( "type" ), fullNames ), result );
        }
        return result;
    }

    /**
     * The content at {@code path} and everything below it.
     */
    static ConstraintExpr subtree( final String path )
    {
        final String normalized = ContentKeys.scopePath( path );
        if ( normalized == null )
        {
            return null;
        }
        final String nodePath = "/content" + normalized;
        return or( CompareExpr.eq( FieldExpr.from( "_path" ), ValueExpr.string( nodePath ) ),
                   CompareExpr.like( FieldExpr.from( "_path" ), ValueExpr.string( nodePath + "/*" ) ) );
    }

    /**
     * Query of all given constraints (null ones are skipped), ordered by path so that pages are stable.
     */
    static QueryExpr byPath( final ConstraintExpr... constraints )
    {
        ConstraintExpr result = null;
        for ( final ConstraintExpr constraint : constraints )
        {
            if ( constraint != null )
            {
                result = result == null ? constraint : LogicalExpr.and( result, constraint );
            }
        }
        return result == null ? new QueryExpr( List.of( BY_PATH ) ) : QueryExpr.from( result, BY_PATH );
    }

    static String normalizeId( final Object id )
    {
        return String.valueOf( id ).toLowerCase( Locale.ROOT );
    }

    private static ConstraintExpr or( final ConstraintExpr left, final ConstraintExpr right )
    {
        if ( left == null )
        {
            return right;
        }
        return right == null ? left : LogicalExpr.or( left, right );
    }
}
