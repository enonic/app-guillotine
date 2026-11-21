package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.Aggregation;
import com.enonic.xp.aggregation.Buckets;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.Media;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.data.Property;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueTypes;
import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.aggregation.TermsAggregationQuery;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.util.Reference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * The content store as a small search index for the {@code references} tools: {@code find} evaluates the XP query DSL they send
 * ({@code boolean}, {@code in}, {@code like}, {@code pathMatch}, {@code matchAll}) with a terms aggregation on {@code _references},
 * {@code getOutboundDependencies} reads data references and processed references, and content in {@link #hidden} exists but is not
 * readable by the caller.
 */
public abstract class ReferenceIndexTestSupport
    extends ContentStoreTestSupport
{
    protected final Set<String> hidden = new LinkedHashSet<>();

    protected final List<ContentQuery> queries = new ArrayList<>();

    protected final List<String> outboundCalls = new ArrayList<>();

    /**
     * Calls of {@code find}, {@code getByIds} and {@code getOutboundDependencies}, for a test clock that advances with them.
     */
    protected int backendCalls;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();
        doAnswer( invocation -> {
            backendCalls++;
            return evaluate( invocation.getArgument( 0 ) );
        } ).when( contentService ).find( any( ContentQuery.class ) );
        doAnswer( invocation -> {
            backendCalls++;
            contexts.add( ContextAccessor.current() );
            final GetContentByIdsParams params = invocation.getArgument( 0 );
            return Contents.from( params.getIds().stream().map( id -> readable( id.toString() ) ).filter( c -> c != null ).toList() );
        } ).when( contentService ).getByIds( any( GetContentByIdsParams.class ) );
        doAnswer( invocation -> {
            backendCalls++;
            contexts.add( ContextAccessor.current() );
            final ContentId id = invocation.getArgument( 0 );
            outboundCalls.add( id.toString() );
            final Content content = readable( id.toString() );
            if ( content == null )
            {
                throw ContentNotFoundException.create().contentId( id ).build();
            }
            final Set<String> ids = references( content );
            ids.remove( id.toString() );
            return ContentIds.from( ids.toArray( String[]::new ) );
        } ).when( contentService ).getOutboundDependencies( any( ContentId.class ) );
    }

    /**
     * Stores an item of {@code type} whose data references {@code dataRefs} and whose rich text references {@code textRefs}.
     */
    protected Content item( final String id, final String parent, final String name, final String type, final List<String> dataRefs,
                            final List<String> textRefs )
    {
        final PropertyTree data = new PropertyTree();
        dataRefs.forEach( ref -> data.addReference( "related", Reference.from( ref ) ) );
        final ContentIds processed = ContentIds.from( textRefs.toArray( String[]::new ) );
        final Content withText;
        if ( type.startsWith( "media:" ) )
        {
            withText = Media.create()
                .id( ContentId.from( id ) )
                .name( name )
                .displayName( name.substring( 0, 1 ).toUpperCase() + name.substring( 1 ).replace( '-', ' ' ) )
                .parentPath( ContentPath.from( parent ) )
                .type( ContentTypeName.from( type ) )
                .data( data )
                .processedReferences( processed )
                .build();
        }
        else
        {
            withText = Content.create( put( id, parent, name, type, data, false ) ).processedReferences( processed ).build();
        }
        store.put( id, withText );
        return withText;
    }

    private Content readable( final String id )
    {
        return hidden.contains( id ) ? null : store.get( id );
    }

    private static Set<String> references( final Content content )
    {
        final Set<String> ids = new LinkedHashSet<>();
        for ( final Property property : content.getData().getProperties( ValueTypes.REFERENCE ) )
        {
            ids.add( property.getValue().toString() );
        }
        if ( content.getProcessedReferences() != null )
        {
            content.getProcessedReferences().forEach( id -> ids.add( id.toString() ) );
        }
        return ids;
    }

    private FindContentIdsByQueryResult evaluate( final ContentQuery query )
    {
        queries.add( query );
        contexts.add( ContextAccessor.current() );
        final Map<String, Object> dsl = dsl( query );
        final List<Content> matches = store.values()
            .stream()
            .filter( content -> !hidden.contains( content.getId().toString() ) && matches( content, dsl ) )
            .sorted( Comparator.comparing( content -> content.getPath().toString() ) )
            .toList();
        final List<String> page = matches.stream().skip( query.getFrom() ).limit( query.getSize() ).map( c -> c.getId().toString() ).toList();

        final Aggregations.Builder aggregations = Aggregations.create();
        for ( final AggregationQuery aggregation : query.getAggregationQueries() )
        {
            final TermsAggregationQuery terms = (TermsAggregationQuery) aggregation;
            final Map<String, Long> counts = new LinkedHashMap<>();
            matches.forEach( content -> values( content, terms.getFieldName() ).forEach( key -> counts.merge( key, 1L, Long::sum ) ) );
            final Buckets.Builder buckets = Buckets.create();
            counts.entrySet()
                .stream()
                .sorted( Map.Entry.<String, Long>comparingByValue().reversed() )
                .limit( terms.getSize() )
                .forEach( entry -> buckets.add( Bucket.create().key( entry.getKey() ).docCount( entry.getValue() ).build() ) );
            aggregations.add( Aggregation.bucketAggregation( terms.getName() ).buckets( buckets.build() ).build() );
        }
        return FindContentIdsByQueryResult.create()
            .contents( ContentIds.from( page.toArray( String[]::new ) ) )
            .totalHits( matches.size() )
            .aggregations( aggregations.build() )
            .build();
    }

    private static List<?> list( final Object value )
    {
        return value == null ? List.of() : value instanceof List<?> list ? list : List.of( value );
    }

    private static Set<String> values( final Content content, final String field )
    {
        return switch ( field )
        {
            case "_id" -> Set.of( content.getId().toString() );
            case "type" -> Set.of( content.getType().toString() );
            case "_path" -> Set.of( "/content" + content.getPath() );
            case "_references" -> references( content );
            default -> throw new IllegalArgumentException( "Field not indexed in this test: " + field );
        };
    }

    @SuppressWarnings("unchecked")
    private static boolean matches( final Content content, final Map<String, Object> clause )
    {
        if ( clause.containsKey( "matchAll" ) )
        {
            return true;
        }
        if ( clause.get( "boolean" ) instanceof Map<?, ?> bool )
        {
            return list( bool.get( "must" ) ).stream().allMatch( c -> matches( content, (Map<String, Object>) c ) ) &&
                list( bool.get( "mustNot" ) ).stream().noneMatch( c -> matches( content, (Map<String, Object>) c ) ) &&
                ( !bool.containsKey( "should" ) || list( bool.get( "should" ) ).stream().anyMatch( c -> matches( content, (Map<String, Object>) c ) ) );
        }
        if ( clause.get( "in" ) instanceof Map<?, ?> in )
        {
            final Set<String> values = values( content, (String) in.get( "field" ) );
            return list( in.get( "values" ) ).stream().anyMatch( value -> values.contains( String.valueOf( value ) ) );
        }
        if ( clause.get( "like" ) instanceof Map<?, ?> like )
        {
            final Pattern pattern = Pattern.compile( ( (String) like.get( "value" ) ).replace( ".", "\\." ).replace( "*", ".*" ) );
            return values( content, (String) like.get( "field" ) ).stream().anyMatch( value -> pattern.matcher( value ).matches() );
        }
        if ( clause.get( "pathMatch" ) instanceof Map<?, ?> pathMatch )
        {
            final String path = (String) pathMatch.get( "path" );
            final String own = "/content" + content.getPath();
            return own.equals( path ) || own.startsWith( path + "/" );
        }
        throw new IllegalArgumentException( "Clause not supported in this test: " + clause );
    }
}
