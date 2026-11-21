package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultBudgetTest
{
    private static final ResultBudget BUDGET = new ResultBudget( 1000 );

    @Test
    public void testResultExactlyOnBudgetIsUntouched()
    {
        final Map<String, Object> input = Map.of( "body", "" );
        final Map<String, Object> exact = Map.of( "body", "x".repeat( 1000 - McpJson.length( input ) ) );

        assertEquals( exact, structured( BUDGET.fit( exact ) ) );
        assertEquals( 1000, McpJson.length( exact ) );
    }

    @Test
    public void testFitKeepsOnlyProjectBranchAndTotalWhenOverBudget()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        new ProjectBranch( "blog", "draft" ).putInto( input );
        input.put( "total", 3 );
        input.put( "body", "x".repeat( 1000 ) );

        final Map<String, Object> result = structured( BUDGET.withNarrowHint( "Ask for fewer fields." ).fit( input ) );

        assertEquals( Map.of( "project", "blog", "branch", "draft", "total", 3, "truncated", true, "hint",
                              "Result does not fit 1000 characters. Ask for fewer fields." ), result );
    }

    @Test
    public void testFitPageSetsNextCursorWhenEverythingFits()
    {
        final Map<String, Object> result = structured( BUDGET.fitPage( Map.of( "hits", List.of( 1, 2 ) ), "hits", new Page( 0, 2 ), 5 ) );

        assertEquals( Map.of( "hits", List.of( 1, 2 ), "nextCursor", Cursors.encode( 2 ) ), result );
        assertEquals( Map.of( "hits", List.of() ), structured( BUDGET.fitPage( Map.of(), "hits", new Page( 0, 10 ), 0 ) ) );
    }

    @Test
    public void testFitPageDropsTrailingItemsAndMovesNextCursor()
    {
        final List<Object> hits = new ArrayList<>();
        for ( int i = 0; i < 10; i++ )
        {
            hits.add( Map.of( "_id", "item-" + i, "body", "x".repeat( 150 ) ) );
        }
        final Map<String, Object> input = new LinkedHashMap<>();
        new ProjectBranch( "blog", "master" ).putInto( input );
        input.put( "total", 100 );
        input.put( "hits", hits );

        final Map<String, Object> result = structured( BUDGET.fitPage( input, "hits", new Page( 20, 10 ), 100 ) );

        final List<?> kept = (List<?>) result.get( "hits" );
        assertTrue( kept.size() > 0 && kept.size() < 10, () -> "kept " + kept.size() );
        assertEquals( hits.subList( 0, kept.size() ), kept );
        assertEquals( Cursors.encode( 20 + kept.size() ), result.get( "nextCursor" ) );
        assertEquals( true, result.get( "truncated" ) );
        assertEquals( "Result truncated to fit 1000 characters: dropped " + ( 10 - kept.size() ) + " trailing item(s) of 'hits'. " +
                          "Narrow the request. Continue with nextCursor.", result.get( "hint" ) );
        assertEquals( 100, result.get( "total" ) );
        assertTrue( McpJson.length( result ) <= 1000 );
    }

    @Test
    public void testFitPageCursorPointsAtFirstDroppedItemAndNeverAtNothing()
    {
        final List<Object> hits = new ArrayList<>();
        for ( int i = 0; i < 10; i++ )
        {
            hits.add( "x".repeat( 150 ) );
        }

        final Map<String, Object> result = structured( BUDGET.fitPage( Map.of( "hits", hits ), "hits", new Page( 0, 10 ), 10 ) );

        assertEquals( Cursors.encode( ( (List<?>) result.get( "hits" ) ).size() ), result.get( "nextCursor" ) );
        final Map<String, Object> single =
            structured( BUDGET.fitPage( Map.of( "hits", List.of( "x".repeat( 2000 ) ) ), "hits", new Page( 0, 10 ), 1 ) );
        assertEquals( List.of(), single.get( "hits" ) );
        assertNull( single.get( "nextCursor" ) );
        assertEquals( "Result truncated to fit 1000 characters: the first item of 'hits' alone is larger, so this page has no items. " +
                          "Narrow the request.", single.get( "hint" ) );
    }

    @Test
    public void testFitPageSkipsAFirstItemLargerThanTheBudgetWithACursorPastIt()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        new ProjectBranch( "blog", "master" ).putInto( input );
        input.put( "total", 30 );
        input.put( "hits", List.of( "x".repeat( 2000 ), "small", "small" ) );

        final Map<String, Object> result = structured( BUDGET.fitPage( input, "hits", new Page( 20, 3 ), 30 ) );

        assertEquals( List.of(), result.get( "hits" ) );
        assertEquals( Cursors.encode( 21 ), result.get( "nextCursor" ) );
        assertEquals( 30, result.get( "total" ) );
        assertEquals( "blog", result.get( "project" ) );
        assertEquals( true, result.get( "truncated" ) );
        assertEquals( "Result truncated to fit 1000 characters: the first item of 'hits' alone is larger, so this page has no items. " +
                          "Narrow the request. Continue with nextCursor to skip that item.", result.get( "hint" ) );
        assertTrue( McpJson.length( result ) <= 1000 );
    }

    @Test
    public void testFitPageFallsBackWhenTheOtherFieldsAloneDoNotFit()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "total", 30 );
        input.put( "aggregations", "x".repeat( 2000 ) );
        input.put( "hits", List.of( "small", "small" ) );

        final Map<String, Object> result = structured( BUDGET.fitPage( input, "hits", new Page( 0, 2 ), 30 ) );

        assertEquals( Map.of( "total", 30, "hits", List.of(), "truncated", true, "hint",
                              "Result does not fit 1000 characters. Narrow the request." ), result );
    }

    @Test
    public void testFitPageKeepsTheSkipCursorWhenTheOtherFieldsDoNotFitEither()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        new ProjectBranch( "blog", "master" ).putInto( input );
        input.put( "total", 30 );
        input.put( "aggregations", "x".repeat( 900 ) );
        input.put( "hits", List.of( "y".repeat( 2000 ), "small" ) );

        final Map<String, Object> result = structured( BUDGET.fitPage( input, "hits", new Page( 0, 2 ), 30 ) );

        assertEquals( Map.of( "project", "blog", "branch", "master", "total", 30, "hits", List.of(), "nextCursor", Cursors.encode( 1 ),
                              "truncated", true, "hint", "Result truncated to fit 1000 characters: the first item of 'hits' alone is " +
                                  "larger, so this page has no items. Narrow the request. Continue with nextCursor to skip that item." ),
                      result );
    }

    @Test
    public void testFitPageDoesNotSkipASmallFirstItemCrowdedOutByOtherFields()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "total", 30 );
        input.put( "aggregations", "x".repeat( 900 ) );
        input.put( "hits", List.of( "y".repeat( 150 ), "small" ) );

        final Map<String, Object> result = structured( BUDGET.fitPage( input, "hits", new Page( 0, 2 ), 30 ) );

        assertNull( result.get( "nextCursor" ) );
        assertEquals( Map.of( "total", 30, "hits", List.of(), "truncated", true, "hint",
                              "Result does not fit 1000 characters. Narrow the request." ), result );
    }

    @Test
    public void testFitListDropsTrailingItemsWithoutACursor()
    {
        final List<Object> projects = new ArrayList<>();
        for ( int i = 0; i < 10; i++ )
        {
            projects.add( Map.of( "name", "project-" + i, "description", "x".repeat( 150 ) ) );
        }
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "defaultBranch", "draft" );
        input.put( "projects", projects );

        final Map<String, Object> result = structured( BUDGET.withNarrowHint( "Pass a project name." ).fitList( input, "projects" ) );

        final List<?> kept = (List<?>) result.get( "projects" );
        assertTrue( kept.size() > 0 && kept.size() < 10, () -> "kept " + kept.size() );
        assertEquals( projects.subList( 0, kept.size() ), kept );
        assertEquals( "draft", result.get( "defaultBranch" ) );
        assertFalse( result.containsKey( "nextCursor" ) );
        assertEquals( "Result truncated to fit 1000 characters: dropped " + ( 10 - kept.size() ) + " trailing item(s) of 'projects'. " +
                          "Pass a project name.", result.get( "hint" ) );
        assertTrue( McpJson.length( result ) <= 1000 );
        assertEquals( Map.of( "projects", List.of( 1 ) ), structured( BUDGET.fitList( Map.of( "projects", List.of( 1 ) ), "projects" ) ) );

        final Map<String, Object> emptied =
            structured( BUDGET.fitList( Map.of( "caller", "alice", "projects", List.of( "x".repeat( 2000 ) ) ), "projects" ) );
        assertEquals( Map.of( "caller", "alice", "projects", List.of(), "truncated", true, "hint",
                              "Result truncated to fit 1000 characters: dropped 1 trailing item(s) of 'projects'. Narrow the request." ),
                      emptied );
    }

    @Test
    public void testFitPageMeasuresWithoutNulls()
    {
        final Map<String, Object> hit = new LinkedHashMap<>();
        hit.put( "_id", "a" );
        hit.put( "unused", null );
        final List<Object> hits = new ArrayList<>();
        hits.add( hit );
        hits.add( null );
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "hits", hits );
        input.put( "note", null );

        assertEquals( Map.of( "hits", List.of( Map.of( "_id", "a" ) ) ),
                      structured( BUDGET.fitPage( input, "hits", new Page( 0, 10 ), 1 ) ) );
    }

    @Test
    public void testFitSubtreesReplacesTheLargestValueWithAMarker()
    {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "title", "Hello" );
        data.put( "body", "x".repeat( 3000 ) );
        data.put( "tags", List.of( "a", "b" ) );
        final Map<String, Object> input = new LinkedHashMap<>();
        new ProjectBranch( "blog", "master" ).putInto( input );
        input.put( "data", data );

        final Map<String, Object> result = structured( BUDGET.fitSubtrees( input, "data" ) );

        assertEquals( Map.of( "title", "Hello", "body", Map.of( "truncated", true, "chars", 3002 ), "tags", List.of( "a", "b" ) ),
                      result.get( "data" ) );
        assertEquals( "blog", result.get( "project" ) );
        assertEquals( true, result.get( "truncated" ) );
        assertEquals( "Result truncated to fit 1000 characters: replaced 'data.body' (3002 chars). Narrow the request.",
                      result.get( "hint" ) );
    }

    @Test
    public void testFitSubtreesDropsTrailingListItemsAndKeepsTheRestWhole()
    {
        final List<Object> items = new ArrayList<>();
        for ( int i = 0; i < 6; i++ )
        {
            items.add( Map.of( "text", "y".repeat( 400 ) ) );
        }
        final Map<String, Object> input = Map.of( "data", Map.of( "title", "t", "items", items ) );

        final Map<String, Object> result = structured( BUDGET.fitSubtrees( input, "data" ) );

        assertEquals( Map.of( "title", "t", "items", items.subList( 0, 2 ) ), result.get( "data" ) );
        assertEquals( "Result truncated to fit 1000 characters: dropped 4 trailing item(s) of 'data.items'. Narrow the request.",
                      result.get( "hint" ) );
    }

    @Test
    public void testFitSubtreesListsAtMostThreeCuts()
    {
        final Map<String, Object> data = new LinkedHashMap<>();
        for ( int i = 0; i < 6; i++ )
        {
            data.put( "f" + i, "y".repeat( 300 ) );
        }

        final Map<String, Object> result = structured( BUDGET.fitSubtrees( Map.of( "data", data ), "data" ) );

        assertEquals( Map.of( "truncated", true, "chars", 302 ), ( (Map<?, ?>) result.get( "data" ) ).get( "f0" ) );
        assertTrue( ( (String) result.get( "hint" ) ).startsWith(
            "Result truncated to fit 1000 characters: replaced 'data.f0' (302 chars); replaced 'data.f1' (302 chars); " +
                "replaced 'data.f2' (302 chars); and " ), result.get( "hint" ).toString() );
        assertTrue( McpJson.length( result ) <= 1000 );
    }

    @Test
    public void testFitSubtreesCutsOneHugeItemInsteadOfDroppingSmallOnes()
    {
        final List<Object> hits = new ArrayList<>();
        hits.add( Map.of( "id", "big", "body", "b".repeat( 100_000 ) ) );
        for ( int i = 0; i < 20; i++ )
        {
            hits.add( Map.of( "id", "small-" + i ) );
        }

        final Map<String, Object> result =
            structured( new ResultBudget( 20000 ).fitSubtrees( Map.of( "data", Map.of( "hits", hits ) ), "data" ) );

        final List<?> kept = (List<?>) ( (Map<?, ?>) result.get( "data" ) ).get( "hits" );
        assertEquals( 21, kept.size() );
        assertEquals( Map.of( "id", "big", "body", Map.of( "truncated", true, "chars", 100_002 ) ), kept.get( 0 ) );
        assertEquals( "Result truncated to fit 20000 characters: replaced 'data.hits[0].body' (100002 chars). Narrow the request.",
                      result.get( "hint" ) );
    }

    @Test
    public void testFitSubtreesOnAWideObjectIsFastAndKeepsLeadingFields()
    {
        final Map<String, Object> data = new LinkedHashMap<>();
        for ( int i = 0; i < 5000; i++ )
        {
            data.put( "field" + i, "v".repeat( 100 ) );
        }

        final long start = System.nanoTime();
        final Map<String, Object> result = structured( new ResultBudget( 20000 ).fitSubtrees( Map.of( "data", data ), "data" ) );
        final long millis = ( System.nanoTime() - start ) / 1_000_000;

        final Map<?, ?> kept = (Map<?, ?>) result.get( "data" );
        assertTrue( kept.size() > 100, () -> "kept " + kept.size() );
        assertEquals( "v".repeat( 100 ), kept.get( "field0" ) );
        assertTrue( McpJson.length( result ) > 15000 && McpJson.length( result ) <= 20000 );
        assertTrue( millis < 2000, () -> millis + " ms" );
    }

    @Test
    public void testFitSubtreesOnManySmallItemsIsFastAndKeepsAPrefix()
    {
        final List<Object> items = new ArrayList<>();
        for ( int i = 0; i < 3000; i++ )
        {
            items.add( Map.of( "id", i, "title", "t".repeat( 40 ), "x", "x".repeat( 40 ) ) );
        }
        final ResultBudget budget = new ResultBudget( 20000 );

        final long start = System.nanoTime();
        final Map<String, Object> result = structured( budget.fitSubtrees( Map.of( "data", Map.of( "items", items ) ), "data" ) );
        final long millis = ( System.nanoTime() - start ) / 1_000_000;

        final List<?> kept = (List<?>) ( (Map<?, ?>) result.get( "data" ) ).get( "items" );
        assertTrue( kept.size() > 150, () -> "kept " + kept.size() );
        assertEquals( items.subList( 0, kept.size() ), kept );
        assertTrue( McpJson.length( result ) > 15000 && McpJson.length( result ) <= 20000 );
        assertTrue( millis < 2000, () -> millis + " ms" );
    }

    @Test
    public void testFitSubtreesKeepingNullFields()
    {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "get", null );
        data.put( "list", Arrays.asList( "z".repeat( 2000 ), null ) );
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "data", data );

        final Map<String, Object> result = structured( BUDGET.keepingNullFields().fitSubtrees( input, "data" ) );

        final Map<?, ?> fitted = (Map<?, ?>) result.get( "data" );
        assertTrue( fitted.containsKey( "get" ) );
        assertNull( fitted.get( "get" ) );
        assertEquals( List.of( Map.of( "truncated", true, "chars", 2002 ) ), fitted.get( "list" ) );
    }

    @Test
    public void testFitSubtreesFallsBackWhenOtherKeysAloneDoNotFit()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        new ProjectBranch( "blog", "master" ).putInto( input );
        input.put( "note", "n".repeat( 2000 ) );
        input.put( "data", Map.of( "a", 1 ) );

        assertEquals( Map.of( "project", "blog", "branch", "master", "truncated", true, "hint",
                              "Result does not fit 1000 characters. Narrow the request." ),
                      structured( BUDGET.fitSubtrees( input, "data" ) ) );
    }

    @Test
    public void testRandomStructuresStayWithinBudget()
    {
        final Random random = new Random( 42 );
        for ( int run = 0; run < 200; run++ )
        {
            final Map<String, Object> input = new LinkedHashMap<>();
            new ProjectBranch( "blog", "master" ).putInto( input );
            input.put( "data", randomValue( random, 0 ) );
            final List<Object> hits = new ArrayList<>();
            for ( int i = random.nextInt( 30 ); i > 0; i-- )
            {
                hits.add( randomValue( random, 1 ) );
            }
            input.put( "hits", hits );

            for ( final Map<String, Object> result : List.of( BUDGET.fitSubtrees( input, "data" ), BUDGET.fit( input ),
                                                             BUDGET.fitPage( input, "hits", new Page( 0, 30 ), 1000 ) ) )
            {
                final Map<String, Object> structured = structured( result );
                assertTrue( McpJson.length( structured ) <= 1000, () -> "length " + McpJson.length( structured ) );
                assertFalse( McpJson.write( structured ).contains( "null" ) );
            }
        }
    }

    @Test
    public void testFromConfigAndMinimum()
    {
        final GuillotineConfigService config = mock( GuillotineConfigService.class );
        when( config.getMcpMaxResultChars() ).thenReturn( 5000 );

        assertEquals( 5000, ResultBudget.fromConfig( config ).maxResultChars() );
        assertEquals( ResultBudget.MIN_RESULT_CHARS, new ResultBudget( 10 ).maxResultChars() );
    }

    private static Object randomValue( final Random random, final int depth )
    {
        final int kind = depth > 3 ? random.nextInt( 3 ) : random.nextInt( 5 );
        return switch ( kind )
        {
            case 0 -> "s".repeat( random.nextInt( 600 ) );
            case 1 -> random.nextInt();
            case 2 -> null;
            case 3 ->
            {
                final List<Object> list = new ArrayList<>();
                for ( int i = random.nextInt( 6 ); i > 0; i-- )
                {
                    list.add( randomValue( random, depth + 1 ) );
                }
                yield list;
            }
            default ->
            {
                final Map<String, Object> map = new LinkedHashMap<>();
                for ( int i = random.nextInt( 6 ); i > 0; i-- )
                {
                    map.put( "k" + i, randomValue( random, depth + 1 ) );
                }
                yield map;
            }
        };
    }
}
