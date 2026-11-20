package com.enonic.app.guillotine.mcp.tools;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.xp.context.Context;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FindUnreferencedToolTest
    extends ReferenceIndexTestSupport
{
    private static final String IMAGE = "media:image";

    private Map<String, Object> run( final Map<String, Object> args )
    {
        return run( args, FindUnreferencedTool.MAX_BUCKETS );
    }

    private Map<String, Object> run( final Map<String, Object> args, final int maxBuckets )
    {
        final FindUnreferencedTool tool =
            new FindUnreferencedTool( contentService, serviceFacade.getContentTypeService(), guillotineConfigService, resolver, maxBuckets );
        final Map<String, Object> structured = structured( as( ALICE, () -> tool.execute( args ) ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.REFERENCES, "find-unreferenced", structured ) );
        return structured;
    }

    private static Map<String, Object> image( final int n )
    {
        return Map.of( "_id", uuid( n ), "_path", "/images/img-" + n, "displayName", "Img " + n, "type", IMAGE );
    }

    /**
     * Images 1 to 4: 1 is used in an article's data, 2 only in its rich text, 3 only by content the caller cannot read, 4 by nothing.
     */
    private void images()
    {
        for ( int n = 1; n <= 4; n++ )
        {
            item( uuid( n ), "/images", "img-" + n, IMAGE, List.of(), List.of() );
        }
        item( uuid( 10 ), "/articles", "article-a", ARTICLE, List.of( uuid( 1 ) ), List.of( uuid( 2 ) ) );
        item( uuid( 11 ), "/private", "secret", ARTICLE, List.of( uuid( 3 ) ), List.of() );
        hidden.add( uuid( 11 ) );
    }

    @Test
    public void testMediaByDefaultListsWhatNoReadableContentReferences()
    {
        images();

        final Map<String, Object> structured = run( args() );

        assertEquals( List.of( image( 3 ), image( 4 ) ), structured.get( "items" ) );
        assertEquals( 2, structured.get( "total" ) );
        assertEquals( 4, structured.get( "checked" ) );
        assertEquals( 4, structured.get( "candidates" ) );
        assertNull( structured.get( "incomplete" ) );
        assertTrue( ( (String) structured.get( "note" ) ).startsWith( "Covers only content the caller can read" ), structured::toString );
        assertEquals( Map.of( "like", Map.of( "field", "type", "value", "media:*" ) ),
                      unwrap( ( (Map<?, ?>) dsl( queries.get( 0 ) ).get( "boolean" ) ).get( "must" ) ) );
        assertEquals( List.of( "_path ASC" ), sort( queries.get( 0 ) ) );
        for ( final Context context : contexts )
        {
            assertEquals( "com.enonic.cms.myproject", context.getRepositoryId().toString() );
            assertEquals( "draft", context.getBranch().toString() );
            assertEquals( ALICE_KEY, context.getAuthInfo().getUser().getKey() );
        }
    }

    @Test
    public void testTypesAndPathNarrowTheCandidates()
    {
        images();
        item( uuid( 20 ), "/images/archive", "old", IMAGE, List.of(), List.of() );

        final Map<String, Object> structured = run( args( "types", List.of( "image" ), "path", "/images/archive/" ) );

        assertEquals( List.of( Map.of( "_id", uuid( 20 ), "_path", "/images/archive/old", "displayName", "Old", "type", IMAGE ) ),
                      structured.get( "items" ) );
        assertEquals( List.of( Map.of( "like", Map.of( "field", "type", "value", "*:image" ) ),
                               Map.of( "pathMatch", Map.of( "field", "_path", "path", "/content/images/archive", "minimumMatch", 3 ) ) ),
                      ( (Map<?, ?>) dsl( queries.get( 0 ) ).get( "boolean" ) ).get( "must" ) );
    }

    @Test
    public void testAReferenceOfAnItemToItselfDoesNotCount()
    {
        item( uuid( 1 ), "/articles", "a1", ARTICLE, List.of( uuid( 1 ), uuid( 2 ) ), List.of() );
        item( uuid( 2 ), "/articles", "a2", ARTICLE, List.of(), List.of() );
        item( uuid( 3 ), "/articles", "a3", ARTICLE, List.of(), List.of( uuid( 3 ) ) );

        final Map<String, Object> structured = run( args( "types", List.of( ARTICLE ) ) );

        assertEquals( List.of( "/articles/a1", "/articles/a3" ), paths( structured ) );
    }

    @Test
    public void testCutOffBucketsAreDecidedOneByOne()
    {
        images();

        final Map<String, Object> structured = run( args(), 1 );

        assertEquals( List.of( "/images/img-3", "/images/img-4" ), paths( structured ) );
        assertNull( structured.get( "incomplete" ) );
    }

    @Test
    public void testCallCapLeavesTheRestUncheckedAndSaysSo()
    {
        IntStream.rangeClosed( 1, 60 ).forEach( n -> item( uuid( n ), "/images", "img-%02d".formatted( n ), IMAGE, List.of(), List.of() ) );
        item( uuid( 100 ), "/articles", "gallery", ARTICLE, IntStream.rangeClosed( 1, 59 ).mapToObj( ContentStoreTestSupport::uuid ).toList(),
              List.of() );

        final Map<String, Object> structured = run( args(), 1 );

        assertEquals( true, structured.get( "incomplete" ) );
        assertEquals( 60, structured.get( "candidates" ) );
        final int checked = (Integer) structured.get( "checked" );
        assertTrue( checked < 60, structured::toString );
        assertEquals( FindUnreferencedTool.MAX_CALLS, queries.size() + 1, "every call but the label lookup is a query" );
        assertTrue( ( (String) structured.get( "note" ) ).contains( "Incomplete: checked the first " + checked + " of 60 items" ),
                    structured::toString );
        assertEquals( List.of(), structured.get( "items" ), "the one unreferenced image is not reached" );
    }

    @Test
    public void testCountLimitsTheListButNotTheTotal()
    {
        images();

        final Map<String, Object> one = run( args( "count", 1 ) );
        assertEquals( List.of( image( 3 ) ), one.get( "items" ) );
        assertEquals( 2, one.get( "total" ) );
        assertTrue( ( (String) one.get( "note" ) ).contains( "Listed the first 1 of 2 by path" ), one::toString );

        final Map<String, Object> none = run( args( "count", 0 ) );
        assertEquals( List.of(), none.get( "items" ) );
        assertEquals( 2, none.get( "total" ) );
        assertFalse( ( (String) none.get( "note" ) ).contains( "Listed" ) );
    }

    @Test
    public void testNothingInScope()
    {
        final Map<String, Object> structured = run( args() );

        assertEquals( Map.of( "project", "myproject", "branch", "draft", "total", 0, "checked", 0, "candidates", 0, "items", List.of(),
                              "note", structured.get( "note" ) ), structured );
    }

    @SuppressWarnings("unchecked")
    private static List<String> paths( final Map<String, Object> structured )
    {
        return ( (List<Map<String, Object>>) structured.get( "items" ) ).stream().map( item -> (String) item.get( "_path" ) ).toList();
    }
}
