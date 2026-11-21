package com.enonic.app.guillotine.mcp.tools;

import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.Cursors;
import com.enonic.app.guillotine.mcp.shape.Deadline;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.context.Context;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

public class FindBrokenReferencesToolTest
    extends ReferenceIndexTestSupport
{
    private static final String LANDING = DATASET_APP + ":landing";

    private static final String DELETED = uuid( 990 );

    private static final String NEVER = "00000000-0000-4000-8000-00000000dead";

    private static final long SECOND = 1_000_000_000L;

    private FindBrokenReferencesTool tool()
    {
        return new FindBrokenReferencesTool( contentService, serviceFacade.getContentTypeService(), guillotineConfigService, resolver );
    }

    private Map<String, Object> run( final Map<String, Object> args )
    {
        return run( tool(), args );
    }

    private Map<String, Object> run( final FindBrokenReferencesTool tool, final Map<String, Object> args )
    {
        final Map<String, Object> structured = structured( as( ALICE, () -> tool.execute( args ) ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.REFERENCES, "find-broken-references", structured ) );
        return structured;
    }

    /**
     * Two landing pages: one links to an existing article, a deleted article (in data), an article the caller cannot read and an image
     * that never existed (in rich text); the other only to the existing article.
     */
    private void landings()
    {
        item( uuid( 1 ), "/articles", "article-a", ARTICLE, List.of(), List.of() );
        item( uuid( 2 ), "/private", "secret", ARTICLE, List.of(), List.of() );
        hidden.add( uuid( 2 ) );
        item( uuid( 10 ), "/landings", "broken", LANDING, List.of( uuid( 1 ), DELETED, uuid( 2 ) ), List.of( NEVER ) );
        item( uuid( 11 ), "/landings", "fine", LANDING, List.of( uuid( 1 ) ), List.of() );
    }

    @Test
    public void testListsItemsWithMissingOrUnreadableTargets()
    {
        landings();

        final Map<String, Object> structured = run( args( "path", "/landings" ) );

        assertEquals( List.of( Map.of( "_id", uuid( 10 ), "_path", "/landings/broken", "displayName", "Broken", "type", LANDING, "missing",
                                       List.of( DELETED, uuid( 2 ), NEVER ) ) ), structured.get( "items" ) );
        assertEquals( 2, structured.get( "total" ) );
        assertEquals( 2, structured.get( "checked" ) );
        assertNull( structured.get( "nextCursor" ) );
        assertNull( structured.get( "incomplete" ) );
        final String note = (String) structured.get( "note" );
        assertTrue( note.startsWith( "Checked items 1-2 of 2 by path. " ), note );
        assertTrue( note.contains( "Not checked: external URLs and URL strings in component configs." ), note );
        assertEquals( List.of( uuid( 10 ), uuid( 11 ) ), outboundCalls );
        assertEquals( Map.of( "pathMatch", Map.of( "field", "_path", "path", "/content/landings", "minimumMatch", 2 ) ),
                      unwrap( ( (Map<?, ?>) dsl( queries.get( 0 ) ).get( "boolean" ) ).get( "must" ) ) );
        for ( final Context context : contexts )
        {
            assertEquals( "com.enonic.cms.myproject", context.getRepositoryId().toString() );
            assertEquals( ALICE_KEY, context.getAuthInfo().getUser().getKey() );
        }
    }

    @Test
    public void testTypesSelectTheItemsToCheck()
    {
        landings();

        final Map<String, Object> structured = run( args( "types", List.of( "landing" ) ) );

        assertEquals( 2, structured.get( "total" ) );
        assertEquals( List.of( uuid( 10 ), uuid( 11 ) ), outboundCalls );
        assertEquals( Map.of( "like", Map.of( "field", "type", "value", "*:landing" ) ),
                      unwrap( ( (Map<?, ?>) dsl( queries.get( 0 ) ).get( "boolean" ) ).get( "must" ) ) );
    }

    @Test
    public void testOneCallChecksAtMostOneHundredItemsAndContinuesWithTheCursor()
    {
        IntStream.range( 0, 105 ).forEach( n -> item( uuid( 100 + n ), "/pages", "page-%03d".formatted( n ), LANDING,
                                                      n == 104 ? List.of( DELETED ) : List.of(), List.of() ) );

        final Map<String, Object> first = run( args( "path", "/pages" ) );
        assertEquals( 100, first.get( "checked" ) );
        assertEquals( true, first.get( "incomplete" ) );
        assertEquals( Cursors.encode( 100 ), first.get( "nextCursor" ) );
        assertEquals( List.of(), first.get( "items" ) );
        assertTrue( ( (String) first.get( "note" ) ).startsWith( "Checked items 1-100 of 105 by path; continue with nextCursor" ) );

        final Map<String, Object> second = run( args( "path", "/pages", "cursor", first.get( "nextCursor" ) ) );
        assertEquals( 5, second.get( "checked" ) );
        assertNull( second.get( "nextCursor" ) );
        assertEquals( List.of( "/pages/page-104" ), paths( second ) );
        assertTrue( ( (String) second.get( "note" ) ).startsWith( "Checked items 101-105 of 105 by path. " ) );
    }

    @Test
    public void testTheTargetCapLeavesTheItemThatWouldExceedItForTheNextCall()
    {
        item( uuid( 10 ), "/pages", "a", LANDING, List.of( uuid( 900 ), uuid( 901 ), uuid( 902 ), uuid( 903 ) ), List.of() );
        item( uuid( 11 ), "/pages", "b", LANDING, List.of( uuid( 904 ) ), List.of() );
        item( uuid( 12 ), "/pages", "c", LANDING, List.of( uuid( 905 ) ), List.of() );
        final FindBrokenReferencesTool tool =
            new FindBrokenReferencesTool( contentService, serviceFacade.getContentTypeService(), guillotineConfigService, resolver, 3,
                                          System::nanoTime );

        final Map<String, Object> first = structured( as( ALICE, () -> tool.execute( args() ) ) );
        assertEquals( 1, first.get( "checked" ), "the first item is checked even when it alone exceeds the cap" );
        assertEquals( List.of( "/pages/a" ), paths( first ) );
        assertEquals( true, first.get( "incomplete" ) );
        assertEquals( Cursors.encode( 1 ), first.get( "nextCursor" ) );

        final Map<String, Object> second = structured( as( ALICE, () -> tool.execute( args( "cursor", first.get( "nextCursor" ) ) ) ) );
        assertEquals( 2, second.get( "checked" ) );
        assertEquals( List.of( "/pages/b", "/pages/c" ), paths( second ) );
        assertNull( second.get( "incomplete" ) );
    }

    private FindBrokenReferencesTool timed( final LongSupplier nanoTime )
    {
        return new FindBrokenReferencesTool( contentService, serviceFacade.getContentTypeService(), guillotineConfigService, resolver,
                                             FindBrokenReferencesTool.MAX_TARGETS, nanoTime );
    }

    @Test
    public void testASlowBackendStopsAtTheDeadlineAndContinuesWithTheCursor()
    {
        IntStream.range( 0, 50 ).forEach( n -> item( uuid( 100 + n ), "/pages", "page-%02d".formatted( n ), LANDING,
                                                     n == 2 ? List.of( DELETED ) : List.of(), List.of() ) );
        final LongSupplier oneSecondPerCall = () -> backendCalls * SECOND;

        final Map<String, Object> structured = run( timed( oneSecondPerCall ), args( "path", "/pages" ) );

        final int checked = (Integer) structured.get( "checked" );
        assertEquals( Deadline.LIMIT_SECONDS - 1, checked, "one source query, then one outbound read per item until 20 s" );
        assertEquals( Deadline.LIMIT_SECONDS + 2, backendCalls, "after the deadline only one target lookup and the label lookup run" );
        assertEquals( true, structured.get( "incomplete" ) );
        assertEquals( Cursors.encode( checked ), structured.get( "nextCursor" ) );
        assertEquals( List.of( "/pages/page-02" ), paths( structured ) );
        assertTrue( ( (String) structured.get( "note" ) ).startsWith(
            "Checked items 1-19 of 50 by path (stopped at the per-call time limit of 20 seconds); continue with nextCursor" ),
                    structured::toString );
    }

    @Test
    public void testTheDeadlineDuringTargetLookupsKeepsOnlyFullyCheckedItems()
    {
        for ( int n = 0; n < 3; n++ )
        {
            final int base = 1000 + n * 300;
            item( uuid( 10 + n ), "/pages", "page-" + n, LANDING, IntStream.range( base, base + 300 ).mapToObj( ContentStoreTestSupport::uuid )
                .toList(), List.of() );
        }
        final int beforeTargetLookups = 4;
        final LongSupplier passedAfterFirstBatch =
            () -> backendCalls > beforeTargetLookups ? Deadline.LIMIT_SECONDS * SECOND : 0;

        final Map<String, Object> structured = run( timed( passedAfterFirstBatch ), args() );

        assertEquals( 1, structured.get( "checked" ), "the first batch covers page-0 and part of page-1" );
        assertEquals( List.of( "/pages/page-0" ), paths( structured ) );
        assertEquals( Cursors.encode( 1 ), structured.get( "nextCursor" ) );
        assertEquals( true, structured.get( "incomplete" ) );
    }

    @Test
    public void testTheDeadlineStillFinishesTheFirstItemsTargetLookups()
    {
        for ( int n = 0; n < 3; n++ )
        {
            final int base = 1000 + n * 600;
            item( uuid( 10 + n ), "/pages", "page-" + n, LANDING, IntStream.range( base, base + 600 ).mapToObj( ContentStoreTestSupport::uuid )
                .toList(), List.of() );
        }
        final int beforeTargetLookups = 4;
        final LongSupplier passedAfterFirstBatch =
            () -> backendCalls > beforeTargetLookups ? Deadline.LIMIT_SECONDS * SECOND : 0;

        final Map<String, Object> structured = run( timed( passedAfterFirstBatch ), args() );

        assertEquals( 1, structured.get( "checked" ), "page-0 needs two batches of 500, and both run" );
        assertEquals( List.of( "/pages/page-0" ), paths( structured ) );
        assertEquals( Cursors.encode( 1 ), structured.get( "nextCursor" ) );
        assertEquals( beforeTargetLookups + 3, backendCalls, "two target batches and the label lookup" );
    }

    @Test
    public void testAnInterruptedCallChecksOnlyTheFirstItem()
    {
        landings();
        Thread.currentThread().interrupt();
        final Map<String, Object> structured;
        try
        {
            structured = run( args( "path", "/landings" ) );
        }
        finally
        {
            assertTrue( Thread.interrupted(), "the interrupt flag is kept" );
        }

        assertEquals( 1, structured.get( "checked" ) );
        assertEquals( List.of( "/landings/broken" ), paths( structured ) );
        assertEquals( Cursors.encode( 1 ), structured.get( "nextCursor" ) );
        assertEquals( List.of( uuid( 10 ) ), outboundCalls );
    }

    @Test
    public void testItemsBeyondThePagingWindowAreReportedAsUnchecked()
    {
        IntStream.range( 0, Cursors.MAX_WINDOW + 1 )
            .forEach( n -> put( uuid( 10000 + n ), "/pages", "page-%05d".formatted( n ), LANDING, null, false ) );

        final Map<String, Object> last = run( args( "cursor", Cursors.encode( Cursors.MAX_WINDOW - 100 ) ) );

        assertEquals( 100, last.get( "checked" ) );
        assertNull( last.get( "nextCursor" ) );
        assertEquals( true, last.get( "incomplete" ) );
        assertTrue( ( (String) last.get( "note" ) ).contains( "narrow with path or types" ), last::toString );
    }

    @Test
    public void testManyMissingTargetsAreCountedAndTheFirstListed()
    {
        final List<String> missing = IntStream.range( 0, 25 ).mapToObj( n -> uuid( 900 + n ) ).toList();
        item( uuid( 10 ), "/landings", "many", LANDING, missing, List.of() );

        final Map<String, Object> structured = run( args() );

        @SuppressWarnings("unchecked") final Map<String, Object> item = ( (List<Map<String, Object>>) structured.get( "items" ) ).get( 0 );
        assertEquals( missing.subList( 0, 20 ), item.get( "missing" ) );
        assertEquals( 25, item.get( "missingCount" ) );
    }

    @Test
    public void testATruncatedResultContinuesAtTheFirstItemDropped()
    {
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        IntStream.range( 0, 10 ).forEach( n -> item( uuid( 100 + n ), "/pages", "page-%02d".formatted( n ), LANDING,
                                                     n % 2 == 1 ? List.of( DELETED, NEVER ) : List.of(), List.of() ) );

        final Map<String, Object> structured = run( args() );

        final List<String> kept = paths( structured );
        assertTrue( kept.size() < 5 && !kept.isEmpty(), structured::toString );
        assertEquals( true, structured.get( "truncated" ) );
        final int firstDropped = 2 * kept.size() + 1;
        assertEquals( Cursors.encode( firstDropped ), structured.get( "nextCursor" ) );

        final Map<String, Object> rest = run( args( "cursor", structured.get( "nextCursor" ) ) );
        assertEquals( "/pages/page-%02d".formatted( firstDropped ), paths( rest ).get( 0 ) );
    }

    @Test
    public void testNothingInScopeSaysSo()
    {
        final Map<String, Object> structured = run( args( "path", "/nowhere" ) );

        assertEquals( 0, structured.get( "total" ) );
        assertEquals( List.of(), structured.get( "items" ) );
        assertTrue( ( (String) structured.get( "note" ) ).startsWith( "No content in scope that the caller can read." ) );
    }

    @Test
    public void testACursorPastTheEndIsAnError()
    {
        landings();

        final String message = assertThrows( ToolError.class,
                                             () -> as( ALICE, () -> tool().execute( args( "cursor", Cursors.encode( 5 ) ) ) ) ).userMessage();
        assertTrue( message.startsWith( "Invalid cursor" ), message );
    }

    @SuppressWarnings("unchecked")
    private static List<String> paths( final Map<String, Object> structured )
    {
        return ( (List<Map<String, Object>>) structured.get( "items" ) ).stream().map( item -> (String) item.get( "_path" ) ).toList();
    }
}
