package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.graphql.BaseGraphQLIntegrationTest;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.aggregation.Buckets;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.ProjectService;
import com.enonic.xp.project.Projects;
import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.expr.DslExpr;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.content.GetContentTypeParams;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static com.enonic.app.guillotine.mcp.McpResults.args;
import static com.enonic.app.guillotine.mcp.McpResults.successStructured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentToolsIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    private static final PrincipalKey ALICE_KEY = PrincipalKey.ofUser( IdProviderKey.system(), "alice" );

    private static final AuthenticationInfo ALICE =
        AuthenticationInfo.create().user( User.create().key( ALICE_KEY ).login( "alice" ).build() ).principals( RoleKeys.AUTHENTICATED )
            .build();

    private static final AuthenticationInfo ADMIN = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "admin" ) ).login( "admin" ).build() )
        .principals( RoleKeys.AUTHENTICATED, RoleKeys.ADMIN )
        .build();

    private static final ContentTypeName LANDING = ContentTypeName.from( "myapplication:landing" );

    private final Map<String, Content> store = new LinkedHashMap<>();

    private final List<String> searchable = new ArrayList<>();

    private final List<ContentQuery> searches = new ArrayList<>();

    private final AtomicReference<Context> searchContext = new AtomicReference<>();

    private final AtomicReference<Context> labelContext = new AtomicReference<>();

    private final AtomicReference<Context> workflowContext = new AtomicReference<>();

    private Aggregations aggregations = Aggregations.empty();

    private ListTypesTool listTypes;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        doReturn( "myproject" ).when( guillotineConfigService ).getMcpDefaultProject();

        final NodeService nodeService = mock( NodeService.class );
        when( nodeService.getByPath( ContentConstants.CONTENT_ROOT_PATH ) ).thenReturn(
            Node.create().name( "content" ).parentPath( NodePath.ROOT ).data( new PropertyTree() ).build() );
        final ProjectService projectService = mock( ProjectService.class );
        when( projectService.list() ).thenReturn( Projects.empty() );
        final ProjectBranchResolver resolver = new ProjectBranchResolver( projectService, nodeService, guillotineConfigService );

        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> find( invocation.getArgument( 0 ) ) );
        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenAnswer( invocation -> {
            final GetContentByIdsParams params = invocation.getArgument( 0 );
            workflowContext.set( ContextAccessor.current() );
            return Contents.from(
                params.getIds().stream().map( id -> store.get( id.toString() ) ).filter( content -> content != null ).toList() );
        } );
        when( contentService.getByPath( any( ContentPath.class ) ) ).thenAnswer( invocation -> store.values()
            .stream()
            .filter( content -> content.getPath().equals( invocation.getArgument( 0 ) ) )
            .findFirst()
            .orElse( null ) );

        final ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( contentTypeService.getByName( any( GetContentTypeParams.class ) ) ).thenAnswer( invocation -> {
            final ContentTypeName name = ( (GetContentTypeParams) invocation.getArgument( 0 ) ).getContentTypeName();
            return getCustomContentTypes().stream().filter( type -> type.getName().equals( name ) ).findFirst().orElse(
                ContentType.create().superType( ContentTypeName.structured() ).name( name ).title( "Folder" ).build() );
        } );

        when( contentTypeService.getAll() ).thenReturn(
            ContentTypes.from( List.of( getCustomContentTypes().get( 0 ), ContentType.create()
                .superType( ContentTypeName.structured() )
                .name( ContentTypeName.folder() )
                .title( "Folder" )
                .build() ) ) );

        final ContentGraphQl graphQl = new ContentGraphQl( schemaProvider, contentService );
        listTypes = new ListTypesTool( graphQl, resolver, guillotineConfigService, contentTypeService );
    }

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        return List.of( ContentType.create()
                            .superType( ContentTypeName.structured() )
                            .name( LANDING )
                            .title( "Landing" )
                            .addFormItem( Input.create().name( "title" ).label( "Title" ).inputType( InputTypeName.TEXT_LINE ).build() )
                            .addFormItem( Input.create().name( "body" ).label( "Body" ).inputType( InputTypeName.HTML_AREA ).build() )
                            .build() );
    }

    @Test
    public void testListTypesSummaryCountsWhatIsReturned()
        throws Exception
    {
        final Buckets.Builder buckets = Buckets.create();
        for ( int i = 0; i < ListTypesTool.MAX_TYPES; i++ )
        {
            buckets.add( Bucket.create().key( "com.example.app:type-" + i ).docCount( ListTypesTool.MAX_TYPES - i ).build() );
        }
        aggregations = Aggregations.create().add( BucketAggregation.bucketAggregation( "types" ).buckets( buckets.build() ).build() ).build();
        searchable.add( "x" );
        doReturn( 3000 ).when( guillotineConfigService ).getMcpMaxResultChars();

        final Map<String, Object> result = as( ALICE, () -> listTypes.execute( args() ) );

        final Map<String, Object> structured = successStructured( result );
        final int listed = ( (List<?>) structured.get( "types" ) ).size();
        assertTrue( listed > 0 && listed < ListTypesTool.MAX_TYPES, () -> McpJson.write( result ) );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( "Only the 1000 most used content types are listed.", structured.get( "note" ) );
        assertTrue( text( result ).startsWith( "At least 1000 content types in use in myproject (draft); " + listed + " listed here.\n" ),
                    text( result ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-types", structured ) );
    }

    @Test
    public void testListTypesThatCannotFitKeepTheRequiredFields()
        throws Exception
    {
        final ContentGraphQl graphQl = mock( ContentGraphQl.class );
        final List<Object> errors = IntStream.range( 0, 10 )
            .mapToObj( i -> (Object) Map.of( "message", "e".repeat( 290 ), "path", List.of( "guillotine", "queryDslConnection" ) ) )
            .toList();
        final Map<String, Object> connection = Map.of( "totalCount", 4, "aggregationsAsJson", Map.of( "types", Map.of( "buckets", List.of(
            Map.of( "key", "base:folder", "docCount", 4 ) ) ) ) );
        when( graphQl.execute( any(), any(), any() ) ).thenReturn(
            Map.of( "data", Map.of( "guillotine", Map.of( "queryDslConnection", connection ) ), "errors", errors ) );
        final ProjectBranchResolver resolver = mock( ProjectBranchResolver.class );
        when( resolver.resolve( any() ) ).thenReturn( new ProjectBranch( "myproject", "draft" ) );
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        final ListTypesTool tool = new ListTypesTool( graphQl, resolver, guillotineConfigService, mock( ContentTypeService.class ) );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> tool.execute( args() ) ) );

        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-types", structured ) );
        assertTrue( McpJson.length( structured ) <= 1000, String.valueOf( McpJson.length( structured ) ) );
        assertEquals( "myproject", structured.get( "project" ) );
        assertEquals( "draft", structured.get( "branch" ) );
        assertEquals( List.of(), structured.get( "types" ) );
        assertEquals( true, structured.get( "truncated" ) );
    }

    @Test
    public void testListTypesCountsTypesInUse()
        throws Exception
    {
        aggregations = Aggregations.create()
            .add( BucketAggregation.bucketAggregation( "types" )
                      .buckets( Buckets.create()
                                    .add( Bucket.create().key( "base:folder" ).docCount( 4 ).build() )
                                    .add( Bucket.create().key( LANDING.toString() ).docCount( 2 ).build() )
                                    .build() )
                      .build() )
            .build();
        searchable.add( "x" );

        final Map<String, Object> structured = successStructured( as( ALICE, () -> listTypes.execute( args() ) ) );

        assertEquals( List.of( Map.of( "name", "base:folder", "displayName", "Folder", "count", 4 ),
                               Map.of( "name", LANDING.toString(), "displayName", "Landing", "count", 2 ) ),
                      normalize( structured.get( "types" ) ) );
        assertEquals( 0, searches.get( 0 ).getSize() );
        assertTrue( aggregationNames( searches.get( 0 ) ).contains( "types" ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( "list-types", structured ) );
    }

    private FindContentIdsByQueryResult find( final ContentQuery query )
    {
        final Map<String, Object> dsl = dsl( query );
        if ( dsl.get( "in" ) instanceof Map<?, ?> in && "_id".equals( in.get( "field" ) ) )
        {
            labelContext.set( ContextAccessor.current() );
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
    private static Map<String, Object> dsl( final ContentQuery query )
    {
        return unwrap( ( (DslExpr) query.getQueryExpr().getConstraint() ).getExpression().toMap() );
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrap( final Object value )
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
            return list.size() == 1 ? unwrap( list.get( 0 ) ) : (T) list.stream().map( ContentToolsIntegrationTest::unwrap ).toList();
        }
        return (T) normalized;
    }

    private static Set<String> aggregationNames( final ContentQuery query )
    {
        final Set<String> names = new HashSet<>();
        for ( final AggregationQuery aggregation : query.getAggregationQueries() )
        {
            names.add( aggregation.getName() );
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private static <T> T normalize( final Object value )
    {
        return (T) McpJson.readObject( McpJson.write( Map.of( "v", value ) ) ).get( "v" );
    }

    private static <T> T as( final AuthenticationInfo authInfo, final Callable<T> callable )
    {
        return ContextBuilder.copyOf( ContextAccessor.current() ).authInfo( authInfo ).build().callWith( callable );
    }
}
