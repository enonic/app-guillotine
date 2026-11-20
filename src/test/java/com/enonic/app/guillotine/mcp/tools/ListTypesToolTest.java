package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.aggregation.Buckets;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;
import com.enonic.xp.security.User;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class ListTypesToolTest
    extends ArticleSchemaTestSupport
{
    private static final String BLOG_POST = DATASET_APP + ":BlogPost";

    private final AtomicReference<String> queriedIn = new AtomicReference<>();

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();
        final ContentTypeService contentTypeService = serviceFacade.getContentTypeService();
        when( contentTypeService.getByName( any( GetContentTypeParams.class ) ) ).thenAnswer( invocation -> {
            final ContentTypeName name = ( (GetContentTypeParams) invocation.getArgument( 0 ) ).getContentTypeName();
            return contentTypeService.getAll().stream().filter( type -> type.getName().equals( name ) ).findFirst().orElse( null );
        } );
    }

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        final List<ContentType> result = new ArrayList<>( super.getCustomContentTypes() );
        result.add( ContentType.create().superType( ContentTypeName.structured() ).name( BLOG_POST ).title( "Blog post" ).build() );
        return result;
    }

    private void typesInUse( final long total, final Map<String, Long> counts )
    {
        final Buckets.Builder buckets = Buckets.create();
        counts.forEach( ( key, count ) -> buckets.add( Bucket.create().key( key ).docCount( count ).build() ) );
        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> {
            queriedIn.set( ContextAccessor.current().getRepositoryId() + "@" + ContextAccessor.current().getBranch() );
            return FindContentIdsByQueryResult.create()
                .contents( ContentIds.empty() )
                .totalHits( total )
                .aggregations(
                    Aggregations.create().add( BucketAggregation.bucketAggregation( "types" ).buckets( buckets.build() ).build() ).build() )
                .build();
        } );
    }

    private Map<String, Object> run()
    {
        return createAdminContext().callWith(
            () -> new ListTypesTool( schemaProvider, serviceFacade.getContentTypeService(), guillotineConfigService, resolver ).execute(
                Map.of() ) );
    }

    @Test
    public void testTypesInUseComeMostUsedFirstWithTitleAndGraphQlType()
    {
        final Map<String, Long> counts = new LinkedHashMap<>();
        counts.put( "base:folder", 4L );
        counts.put( ARTICLE, 30L );
        counts.put( "other.app:thing", 4L );
        counts.put( DATASET_APP.toLowerCase() + ":blogpost", 2L );
        typesInUse( 40, counts );

        final Map<String, Object> structured = structured( run() );

        assertEquals( Map.of( "project", "myproject", "branch", "draft", "total", 40, "types",
                              List.of( Map.of( "name", ARTICLE, "title", "Article", "graphqlType", ARTICLE_TYPE, "count", 30 ),
                                       Map.of( "name", "base:folder", "title", "Folder", "graphqlType", "base_Folder", "count", 4 ),
                                       Map.of( "name", "other.app:thing", "graphqlType", "UntypedContent", "count", 4 ),
                                       Map.of( "name", BLOG_POST, "title", "Blog post", "graphqlType",
                                               "com_example_mcpdataset_BlogPost", "count", 2 ) ) ),
                      structured );
        assertEquals( "com.enonic.cms.myproject@draft", queriedIn.get() );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-types", structured ) );
    }

    @Test
    public void testDeniedReadSaysWhichBranchToUse()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenThrow( new ForbiddenAccessException( User.anonymous() ) );

        final ToolError error = assertThrows( ToolError.class, this::run );

        assertTrue( error.userMessage().startsWith( "Access denied: the caller cannot read branch draft of project myproject." ),
                    error.userMessage() );
    }

    @Test
    public void testManyTypesAreCutWithinTheBudgetAndCappedWithANote()
    {
        final Map<String, Long> counts = new LinkedHashMap<>();
        IntStream.range( 0, ListTypesTool.MAX_TYPES ).forEach( i -> counts.put( "com.example.app:type-" + i, (long) ListTypesTool.MAX_TYPES - i ) );
        typesInUse( 100_000, counts );
        doReturn( 3000 ).when( guillotineConfigService ).getMcpMaxResultChars();

        final Map<String, Object> structured = structured( run() );

        final int listed = ( (List<?>) structured.get( "types" ) ).size();
        assertTrue( listed > 0 && listed < ListTypesTool.MAX_TYPES, () -> McpJson.write( structured ) );
        assertTrue( McpJson.length( structured ) <= 3000 );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( "Only the 1000 most used content types are listed.", structured.get( "note" ) );
        assertEquals( "com.example.app:type-0", ( (Map<?, ?>) ( (List<?>) structured.get( "types" ) ).get( 0 ) ).get( "name" ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-types", structured ) );
    }
}
