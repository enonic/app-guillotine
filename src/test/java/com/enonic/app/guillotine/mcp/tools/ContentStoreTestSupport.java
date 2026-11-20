package com.enonic.app.guillotine.mcp.tools;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.WorkflowInfo;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.expr.DslExpr;
import com.enonic.xp.query.expr.DslOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A content store behind the mocked content service, so that the tools run real Guillotine GraphQL: {@code find} records each search
 * and answers the {@link #searchable} ids page by page, id lookups ({@code in _id}) answer the stored ids, and gets by id or path read
 * the store.
 */
public abstract class ContentStoreTestSupport
    extends ArticleSchemaTestSupport
{
    static final PrincipalKey ALICE_KEY = PrincipalKey.ofUser( IdProviderKey.system(), "alice" );

    static final AuthenticationInfo ALICE =
        AuthenticationInfo.create().user( User.create().key( ALICE_KEY ).login( "alice" ).build() ).principals( RoleKeys.AUTHENTICATED )
            .build();

    static final String PERSON = DATASET_APP + ":person";

    protected final Map<String, Content> store = new LinkedHashMap<>();

    protected final List<String> searchable = new ArrayList<>();

    protected final List<ContentQuery> searches = new ArrayList<>();

    protected final List<Context> contexts = new ArrayList<>();

    protected final AtomicReference<Context> searchContext = new AtomicReference<>();

    protected Aggregations aggregations = Aggregations.empty();

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();
        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> find( invocation.getArgument( 0 ) ) );
        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenAnswer( invocation -> {
            contexts.add( ContextAccessor.current() );
            final GetContentByIdsParams params = invocation.getArgument( 0 );
            return Contents.from( params.getIds().stream().map( id -> store.get( id.toString() ) ).filter( c -> c != null ).toList() );
        } );
        when( contentService.getById( any( ContentId.class ) ) ).thenAnswer( invocation -> {
            final Content content = store.get( invocation.getArgument( 0 ).toString() );
            if ( content == null )
            {
                throw ContentNotFoundException.create().contentId( invocation.getArgument( 0 ) ).build();
            }
            return content;
        } );
        when( contentService.getByPath( any( ContentPath.class ) ) ).thenAnswer( invocation -> store.values()
            .stream()
            .filter( content -> content.getPath().equals( invocation.getArgument( 0 ) ) )
            .findFirst()
            .orElseThrow( () -> ContentNotFoundException.create().contentPath( invocation.getArgument( 0 ) ).build() ) );

        final ContentTypeService contentTypeService = serviceFacade.getContentTypeService();
        when( contentTypeService.getByName( any( GetContentTypeParams.class ) ) ).thenAnswer( invocation -> {
            final ContentTypeName name = ( (GetContentTypeParams) invocation.getArgument( 0 ) ).getContentTypeName();
            return contentTypeService.getAll().stream().filter( type -> type.getName().equals( name ) ).findFirst().orElse( null );
        } );
    }

    static String uuid( final int n )
    {
        return "%08d-0000-4000-8000-000000000000".formatted( n );
    }

    protected Content put( final String id, final String parent, final String name, final String type, final PropertyTree data,
                           final boolean inProgress )
    {
        final Content content = Content.create()
            .id( ContentId.from( id ) )
            .name( name )
            .displayName( name.substring( 0, 1 ).toUpperCase() + name.substring( 1 ).replace( '-', ' ' ) )
            .parentPath( ContentPath.from( parent ) )
            .type( ContentTypeName.from( type ) )
            .data( data != null ? data : new PropertyTree() )
            .modifier( ALICE_KEY )
            .modifiedTime( Instant.parse( "2026-01-01T00:00:00Z" ).plusSeconds( store.size() ) )
            .workflowInfo( inProgress ? WorkflowInfo.inProgress() : null )
            .build();
        store.put( id, content );
        return content;
    }

    /**
     * Stores {@code count} folders under /folders and makes them the search result.
     */
    protected void folders( final int count )
    {
        for ( int i = 0; i < count; i++ )
        {
            put( uuid( 500 + i ), "/folders", "folder-" + i, "base:folder", null, i == 0 );
            searchable.add( uuid( 500 + i ) );
        }
    }

    private FindContentIdsByQueryResult find( final ContentQuery query )
    {
        final Map<String, Object> dsl = dsl( query );
        if ( dsl.get( "in" ) instanceof Map<?, ?> in && "_id".equals( in.get( "field" ) ) )
        {
            contexts.add( ContextAccessor.current() );
            final List<?> values = in.get( "values" ) instanceof List<?> list ? list : List.of( in.get( "values" ) );
            final String[] ids = values.stream().map( String::valueOf ).filter( store::containsKey ).toArray( String[]::new );
            return FindContentIdsByQueryResult.create().contents( ContentIds.from( ids ) ).totalHits( ids.length ).build();
        }
        searches.add( query );
        searchContext.set( ContextAccessor.current() );
        final List<String> page = searchable.stream().skip( query.getFrom() ).limit( query.getSize() ).toList();
        return FindContentIdsByQueryResult.create()
            .contents( ContentIds.from( page.toArray( String[]::new ) ) )
            .totalHits( searchable.size() )
            .aggregations( aggregations )
            .build();
    }

    /**
     * The query DSL as a plain map. {@link PropertyTree#toMap()} wraps every value in a list, so single-item lists are unwrapped; use
     * {@link #unwrap(Object)} on the expected value too.
     */
    static Map<String, Object> dsl( final ContentQuery query )
    {
        return unwrap( ( (DslExpr) query.getQueryExpr().getConstraint() ).getExpression().toMap() );
    }

    @SuppressWarnings("unchecked")
    static <T> T unwrap( final Object value )
    {
        final Object normalized = normalize( value );
        if ( normalized instanceof Map<?, ?> map )
        {
            final Map<String, Object> result = new LinkedHashMap<>();
            map.forEach( ( key, item ) -> result.put( (String) key, unwrap( item ) ) );
            return (T) result;
        }
        if ( normalized instanceof List<?> list )
        {
            return list.size() == 1 ? unwrap( list.get( 0 ) ) : (T) list.stream().map( ContentStoreTestSupport::unwrap ).toList();
        }
        return (T) normalized;
    }

    @SuppressWarnings("unchecked")
    static <T> T normalize( final Object value )
    {
        return (T) McpJson.readObject( McpJson.write( Map.of( "v", value ) ) ).get( "v" );
    }

    static List<String> sort( final ContentQuery query )
    {
        return query.getQueryExpr().getOrderList().stream().map( ContentStoreTestSupport::order ).toList();
    }

    private static String order( final OrderExpr expr )
    {
        return ( (DslOrderExpr) expr ).getField() + " " + expr.getDirection();
    }

    static Set<String> aggregationNames( final ContentQuery query )
    {
        final Set<String> names = new HashSet<>();
        for ( final AggregationQuery aggregation : query.getAggregationQueries() )
        {
            names.add( aggregation.getName() );
        }
        return names;
    }

    static <T> T as( final AuthenticationInfo authInfo, final Callable<T> callable )
    {
        return ContextBuilder.copyOf( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( callable );
    }

    static Map<String, Object> args( final Object... keyValues )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        for ( int i = 0; i < keyValues.length; i += 2 )
        {
            result.put( (String) keyValues[i], keyValues[i + 1] );
        }
        return result;
    }
}
