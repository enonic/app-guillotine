package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;

import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultBudgetTest
{
    private static final int MAX = 1000;

    private static final int DEFAULT_MAX_RESULT_CHARS = 20000;

    @Test
    public void testStructuredExactlyOnBudgetIsUntouched()
    {
        final Map<String, Object> structured = exact( MAX );
        assertEquals( MAX, McpJson.length( structured ) );

        final Map<String, Object> result = ResultBudget.of( MAX ).apply( "Summary.", structured );

        assertEquals( McpResult.success( "Summary.", structured ), result );
        assertTrue( McpJson.length( result ) > 2 * MAX, () -> "wire " + McpJson.length( result ) );
    }

    @Test
    public void testStructuredOneCharOverBudgetIsTruncated()
    {
        final Map<String, Object> input = new LinkedHashMap<>( exact( MAX + 1 ) );

        final Map<String, Object> result = ResultBudget.of( MAX ).apply( "Summary.", input );

        final Map<String, Object> structured = assertWholeWithinBudget( result, MAX );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( Map.of( "truncated", true, "chars", McpJson.length( input.get( "text" ) ) ), structured.get( "text" ) );
        assertTrue( hint( structured ).contains( "replaced 'text'" ), hint( structured ) );
        assertTrue( hint( structured ).endsWith( ". " + ResultBudget.DEFAULT_NARROW_HINT ), hint( structured ) );
    }

    @Test
    public void testSummaryAndTextCopyDoNotCountAgainstTheBudget()
    {
        final Map<String, Object> input = Map.of( "text", "x".repeat( 900 ) );
        assertTrue( McpJson.length( input ) < MAX );

        final Map<String, Object> result = ResultBudget.of( MAX ).apply( "s".repeat( MAX / 4 ), input );

        assertEquals( input, assertWholeWithinBudget( result, MAX ) );
        assertTrue( McpJson.length( result ) > 2 * MAX, () -> "wire " + McpJson.length( result ) );
    }

    @Test
    public void testEscapedCharactersCountAsWrittenInTheStructuredJson()
    {
        final Map<String, Object> input = Map.of( "id", "1", "quotes", "\"\\".repeat( 300 ) );
        assertTrue( McpJson.length( input ) > MAX );

        final Map<String, Object> result = ResultBudget.of( MAX ).apply( "Summary with \"quotes\".", input );

        assertEquals( true, assertWholeWithinBudget( result, MAX ).get( "truncated" ) );
    }

    @Test
    public void testWireSizeOfEscapeHeavyResultOnBudgetStaysBounded()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        for ( int i = 0; McpJson.length( input ) < MAX - 40; i++ )
        {
            input.put( "q" + i, "\"\\\"\\\"" );
        }
        final String summary = "\"".repeat( MAX );

        final Map<String, Object> result = ResultBudget.of( MAX ).apply( summary, input );

        assertEquals( input, assertWholeWithinBudget( result, MAX ) );
        assertTrue( McpJson.length( result ) > 5 * MAX / 2, () -> "wire " + McpJson.length( result ) );
    }

    @Test
    public void testHugeSummaryIsShortened()
    {
        final Map<String, Object> result = ResultBudget.of( MAX ).apply( "s".repeat( 5000 ), Map.of( "id", "1" ) );

        assertWholeWithinBudget( result, MAX );
        assertTrue( text( result ).startsWith( "s".repeat( MAX / 4 - 1 ) + "\u2026\n" ) );
    }

    @Test
    public void testSummaryIsShortenedByItsEscapedLength()
    {
        final Map<String, Object> result = ResultBudget.of( MAX ).apply( "\u0001".repeat( 5000 ), Map.of( "id", "1" ) );

        assertWholeWithinBudget( result, MAX );
        final String text = text( result );
        final String summary = text.substring( 0, text.indexOf( '\n' ) );
        assertTrue( McpJson.length( summary ) - 2 <= MAX / 4, summary );
        assertTrue( summary.length() > MAX / 4 / 6 - 2, summary );
    }

    @Test
    public void testFiftyTypicalHitsFitTheDefaultBudget()
    {
        final List<Object> hits = new ArrayList<>();
        for ( int i = 0; i < 50; i++ )
        {
            hits.add( Map.of( "_id", "45d19296-5f2a-4d2c-9a8e-%012d".formatted( i ), "_path", "/articles/article-" + i, "displayName",
                              "Article number " + i, "type", "com.example.mcpdataset:article", "modifiedTime",
                              "2026-01-31T12:00:00.000Z", "language", "en", "workflow", "READY", "padding", "p".repeat( 90 ) ) );
        }
        final Map<String, Object> input = Map.of( "project", "blog", "branch", "master", "total", 50, "hits", hits );
        assertTrue( McpJson.length( input ) > DEFAULT_MAX_RESULT_CHARS / 2 );

        final Map<String, Object> structured =
            assertWholeWithinBudget( ResultBudget.of( DEFAULT_MAX_RESULT_CHARS ).apply( "Hits.", input ),
                                     DEFAULT_MAX_RESULT_CHARS );

        assertNull( structured.get( "truncated" ) );
        assertEquals( 50, ( (List<?>) structured.get( "hits" ) ).size() );
    }

    @Test
    public void testKeepingNullsKeepsNullFieldsAndStillFits()
    {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put( "get", null );
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "data", Map.of( "guillotine", nested ) );
        input.put( "items", java.util.Arrays.asList( "a", null ) );

        final Map<String, Object> kept = shape( ResultBudget.of( MAX ).keepingNulls(), input );
        assertEquals( input, kept );
        assertTrue( McpJson.write( kept ).contains( "\"get\":null" ) );

        input.put( "big", "b".repeat( 3000 ) );
        final Map<String, Object> cut = shape( ResultBudget.of( MAX ).keepingNulls(), input );
        assertEquals( Map.of( "guillotine", nested ), cut.get( "data" ) );
        assertEquals( true, cut.get( "truncated" ) );

        assertEquals( Map.of( "data", Map.of( "guillotine", Map.of() ), "items", java.util.Arrays.asList( "a", null ) ),
                      shape( ResultBudget.of( MAX ), new LinkedHashMap<>( Map.of( "data", Map.of( "guillotine", nested ), "items",
                                                                                   java.util.Arrays.asList( "a", null ) ) ) ) );
    }

    @Test
    public void testListOnlyDropsTrailingItemsAndNeverReplaces()
    {
        final List<Object> items = new ArrayList<>();
        for ( int i = 0; i < 50; i++ )
        {
            items.add( Map.of( "name", "p" + i, "description", "d".repeat( 100 ) ) );
        }

        final Map<String, Object> structured =
            assertWholeWithinBudget( ResultBudget.listOnly( MAX, "projects", "Pass a name." ).apply( "List.", Map.of( "projects", items ) ),
                                     MAX );

        final List<?> kept = (List<?>) structured.get( "projects" );
        assertEquals( items.subList( 0, kept.size() ), kept );
        assertTrue( hint( structured ).endsWith( "Pass a name." ), hint( structured ) );

        final Map<String, Object> huge = Map.of( "projects", List.of( Map.of( "d", "d".repeat( 3000 ) ) ) );
        final Map<String, Object> single =
            assertWholeWithinBudget( ResultBudget.listOnly( MAX, "projects", "Pass a name." ).apply( "List.", huge ), MAX );
        assertEquals( List.of(), single.get( "projects" ) );
    }

    @Test
    public void testErrorsArePreserved()
    {
        final List<Object> errors = List.of( Map.of( "message", "Bad", "path", List.of( "a" ) ) );

        final Map<String, Object> structured =
            shape( ResultBudget.of( MAX ), Map.of( "errors", errors, "items", List.of( "x".repeat( 3000 ), "y".repeat( 3000 ) ) ) );

        assertEquals( errors, structured.get( "errors" ) );
    }

    @Test
    public void testLongSummaryIsNotCutInsideASurrogatePair()
    {
        final int max = MAX / 4;
        final String summary = "x".repeat( max - 2 ) + "\uD83D\uDE00" + "tail";

        final String text = text( ResultBudget.of( MAX ).apply( summary, Map.of() ) );

        final String fitted = text.substring( 0, text.indexOf( '\n' ) );
        assertEquals( "x".repeat( max - 2 ) + "\u2026", fitted );
    }

    @Test
    public void testFromConfig()
    {
        final GuillotineConfigService config = mock( GuillotineConfigService.class );
        when( config.getMcpMaxResultChars() ).thenReturn( 5000 );

        assertEquals( 5000, ResultBudget.fromConfig( config, "data" ).maxResultChars() );
    }

    @Test
    public void testSubtreeExactlyOnLimitIsUntouched()
    {
        final Map<String, Object> input = Map.of( "data", "x".repeat( ResultBudget.DEFAULT_MAX_SUBTREE_CHARS - 2 ) );

        final Map<String, Object> result = shape( ResultBudget.of( 20000, "data" ), input );

        assertEquals( input, result );
    }

    @Test
    public void testSubtreeOneCharOverLimitIsReplacedWithMarker()
    {
        final Map<String, Object> input = Map.of( "data", "x".repeat( ResultBudget.DEFAULT_MAX_SUBTREE_CHARS - 1 ), "id", "1" );

        final Map<String, Object> result = shape( ResultBudget.of( 20000, "data" ), input );

        assertEquals( Map.of( "truncated", true, "chars", ResultBudget.DEFAULT_MAX_SUBTREE_CHARS + 1 ), result.get( "data" ) );
        assertEquals( "1", result.get( "id" ) );
        assertEquals( true, result.get( "truncated" ) );
        assertTrue( hint( result ).contains( "replaced 'data' (6001 chars, over the 6000-char limit for one value)" ), hint( result ) );
    }

    @Test
    public void testChildSubtreeKeyLimitsEachChildSeparately()
    {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "body", "x".repeat( 7000 ) );
        data.put( "title", "kept" );
        data.put( "intro", "y".repeat( 5000 ) );
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "content", Map.of( "_id", "1", "data", data ) );

        final Map<String, Object> result =
            shape( ResultBudget.of( 20000, "data.*" ).withNarrowHint( "Pass fields to read them." ), input );

        final Map<?, ?> resultData = (Map<?, ?>) ( (Map<?, ?>) result.get( "content" ) ).get( "data" );
        assertEquals( Map.of( "truncated", true, "chars", 7002 ), resultData.get( "body" ) );
        assertEquals( "kept", resultData.get( "title" ) );
        assertEquals( "y".repeat( 5000 ), resultData.get( "intro" ) );
        assertEquals( "1", ( (Map<?, ?>) result.get( "content" ) ).get( "_id" ) );
        assertEquals( "Result truncated to fit 20000 characters: replaced 'content.data.body' (7002 chars, over the 6000-char limit " +
                          "for one value). Pass fields to read them.", hint( result ) );

        final Map<String, Object> scalar = Map.of( "content", Map.of( "data", "z".repeat( 7000 ) ) );
        assertEquals( scalar, shape( ResultBudget.of( 20000, "data.*" ), scalar ) );
    }

    @Test
    public void testNestedSubtreeInListIsReplacedByPath()
    {
        final List<Object> hits = new ArrayList<>();
        hits.add( Map.of( "id", "a", "data", Map.of( "title", "short" ) ) );
        hits.add( Map.of( "id", "b", "data", Map.of( "body", "y".repeat( 7000 ) ) ) );

        final Map<String, Object> result = shape( ResultBudget.of( 20000, "data" ), Map.of( "hits", hits ) );

        final List<?> resultHits = (List<?>) result.get( "hits" );
        assertEquals( Map.of( "id", "a", "data", Map.of( "title", "short" ) ), resultHits.get( 0 ) );
        assertEquals( true, ( (Map<?, ?>) ( (Map<?, ?>) resultHits.get( 1 ) ).get( "data" ) ).get( "truncated" ) );
        assertTrue( hint( result ).contains( "'hits[1].data'" ), hint( result ) );
    }

    @Test
    public void testTrailingListItemsAreDropped()
    {
        final List<Object> hits = new ArrayList<>();
        for ( int i = 0; i < 30; i++ )
        {
            hits.add( Map.of( "id", String.valueOf( i ), "title", "t".repeat( 900 ) ) );
        }
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "project", "blog" );
        input.put( "branch", "master" );
        input.put( "total", 30 );
        input.put( "hits", hits );

        final Map<String, Object> result = shape( ResultBudget.of( 20000 ), input );

        assertWithinBudget( result, 20000 );
        final List<?> kept = (List<?>) result.get( "hits" );
        assertTrue( kept.size() >= 20 && kept.size() < 30, String.valueOf( kept.size() ) );
        assertEquals( hits.subList( 0, kept.size() ), kept );
        assertEquals( 30, result.get( "total" ) );
        assertTrue( hint( result ).contains( "dropped " + ( 30 - kept.size() ) + " trailing item(s) of 'hits'" ), hint( result ) );
    }

    @Test
    public void testNextCursorFollowsLastKeptItem()
    {
        final Map<String, Object> args = Map.of( "query", "news", "count", 30 );
        final ProjectBranch target = new ProjectBranch( "blog", "master" );
        final Page page = Cursors.page( "search", args, target, "only the total" );
        final List<Object> hits = new ArrayList<>();
        for ( int i = 0; i < 30; i++ )
        {
            hits.add( Map.of( "id", String.valueOf( i ), "title", "t".repeat( 900 ) ) );
        }
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "hits", hits );
        input.put( "nextCursor", page.nextCursor( 100, 30 ) );

        final Map<String, Object> result =
            assertWholeWithinBudget( ResultBudget.of( 20000 ).apply( "Page.", input, "hits", page, 100 ), 20000 );

        assertWithinBudget( result, 20000 );
        final int kept = ( (List<?>) result.get( "hits" ) ).size();
        assertTrue( kept < 30 );
        final Map<String, Object> nextArgs = new LinkedHashMap<>( args );
        nextArgs.put( "cursor", result.get( "nextCursor" ) );
        assertEquals( kept, Cursors.page( "search", nextArgs, target, "only the total" ).offset() );
    }

    @Test
    public void testDescendsIntoTheLargestChild()
    {
        final Map<String, Object> content = new LinkedHashMap<>();
        content.put( "id", "1" );
        content.put( "displayName", "Article" );
        content.put( "data", Map.of( "intro", "short", "body", "b".repeat( 3000 ) ) );

        final Map<String, Object> result = shape( ResultBudget.of( MAX ), Map.of( "content", content ) );

        assertWithinBudget( result, MAX );
        final Map<?, ?> resultContent = (Map<?, ?>) result.get( "content" );
        assertEquals( "Article", resultContent.get( "displayName" ) );
        assertEquals( "short", ( (Map<?, ?>) resultContent.get( "data" ) ).get( "intro" ) );
        assertEquals( true, ( (Map<?, ?>) ( (Map<?, ?>) resultContent.get( "data" ) ).get( "body" ) ).get( "truncated" ) );
        assertTrue( hint( result ).contains( "'content.data.body'" ), hint( result ) );
    }

    @Test
    public void testPathologicalSingleHugeString()
    {
        final Map<String, Object> result = shape( ResultBudget.of( 20000 ), Map.of( "body", "\"\\".repeat( 500_000 ) ) );

        assertWithinBudget( result, 20000 );
        assertEquals( Map.of( "truncated", true, "chars", 2_000_002 ), result.get( "body" ) );
    }

    @Test
    public void testHugeStringInsideSingleItemLists()
    {
        final Map<String, Object> result = shape( ResultBudget.of( MAX ), Map.of( "hits", List.of( List.of( "z".repeat( 50_000 ) ) ) ) );

        assertWithinBudget( result, MAX );
        assertInstanceOf( Map.class, ( (List<?>) ( (List<?>) result.get( "hits" ) ).get( 0 ) ).get( 0 ) );
        assertTrue( hint( result ).contains( "'hits[0][0]'" ), hint( result ) );
    }

    @Test
    public void testManyTinyFieldsFallBackToProtectedKeys()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "project", "blog" );
        input.put( "branch", "draft" );
        input.put( "nextCursor", "abc" );
        for ( int i = 0; i < 500; i++ )
        {
            input.put( "k" + i, i );
        }

        final Map<String, Object> result = shape( ResultBudget.of( MAX ), input );

        assertWithinBudget( result, MAX );
        assertEquals( "blog", result.get( "project" ) );
        assertEquals( "draft", result.get( "branch" ) );
        assertFalse( result.containsKey( "nextCursor" ) );
        assertTrue( hint( result ).contains( "dropped all content" ), hint( result ) );
    }

    @Test
    public void testHugeProtectedValueFallsBackToMinimalResult()
    {
        final Map<String, Object> result = shape( ResultBudget.of( MAX ), Map.of( "project", "p".repeat( 5000 ) ) );

        assertWithinBudget( result, MAX );
        assertEquals( 2, result.size() );
        assertEquals( true, result.get( "truncated" ) );
    }

    @Test
    public void testNullsAreRemovedBeforeMeasuring()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "a", "x" );
        input.put( "b", null );

        assertEquals( Map.of( "a", "x" ), shape( ResultBudget.of( MAX ), input ) );
    }

    @Test
    public void testBudgetIsClampedToMinimum()
    {
        assertEquals( ResultBudget.MIN_RESULT_CHARS, ResultBudget.of( 10 ).maxResultChars() );
        assertEquals( 20000, ResultBudget.of( 20000 ).maxResultChars() );
        assertEquals( ResultBudget.DEFAULT_MAX_SUBTREE_CHARS, ResultBudget.of( 20000 ).maxSubtreeChars() );
    }

    @Test
    public void testHintListsAtMostThreeCuts()
    {
        final Map<String, Object> input = new LinkedHashMap<>();
        for ( int i = 0; i < 5; i++ )
        {
            input.put( "data" + i, Map.of( "data", "d".repeat( 7000 ) ) );
        }

        final Map<String, Object> result = shape( new ResultBudget( 20000, 6000, List.of( "data" ) ), input );

        assertTrue( hint( result ).contains( "; and 2 more" ), hint( result ) );
    }

    @Test
    public void testRandomStructuresStayValidAndWithinBudget()
    {
        final Random random = new Random( 42 );
        int truncated = 0;
        for ( int i = 0; i < 300; i++ )
        {
            final int max = 1000 + random.nextInt( 20000 );
            final Map<String, Object> input = randomMap( random, 0 );

            final Map<String, Object> result =
                assertWholeWithinBudget( new ResultBudget( max, 500 + random.nextInt( 6000 ), List.of( "data" ) ).apply( "Random.", input ),
                                         max );

            assertWithinBudget( result, max );
            if ( result.containsKey( "truncated" ) )
            {
                truncated++;
            }
            else
            {
                assertEquals( Nulls.clean( input ), result );
            }
        }
        assertTrue( truncated > 30 && truncated < 270, String.valueOf( truncated ) );
    }

    private static Map<String, Object> randomMap( final Random random, final int depth )
    {
        final Map<String, Object> map = new LinkedHashMap<>();
        final int size = random.nextInt( depth == 0 ? 8 : 5 );
        for ( int i = 0; i < size; i++ )
        {
            map.put( random.nextInt( 4 ) == 0 ? "data" : "f" + i, randomValue( random, depth + 1 ) );
        }
        return map;
    }

    private static Object randomValue( final Random random, final int depth )
    {
        final int kind = depth > 3 ? random.nextInt( 3 ) : random.nextInt( 5 );
        return switch ( kind )
        {
            case 0 -> "s".repeat( random.nextInt( random.nextInt( 10 ) == 0 ? 30000 : 300 ) );
            case 1 -> random.nextInt( 3 ) == 0 ? null : random.nextLong();
            case 2 -> random.nextBoolean();
            case 3 -> randomMap( random, depth );
            default ->
            {
                final List<Object> list = new ArrayList<>();
                final int size = random.nextInt( 12 );
                for ( int i = 0; i < size; i++ )
                {
                    list.add( randomValue( random, depth + 1 ) );
                }
                yield list;
            }
        };
    }

    private static Map<String, Object> shape( final ResultBudget budget, final Map<String, Object> input )
    {
        return assertWholeWithinBudget( budget.apply( "Summary.", input ), budget.maxResultChars() );
    }

    /**
     * The structured object fits {@code max}; the whole result, which also carries it escaped in the text after a summary, stays within
     * {@link #maxWireChars}.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> assertWholeWithinBudget( final Map<String, Object> result, final int max )
    {
        final String json = McpJson.write( result );
        assertTrue( json.length() <= maxWireChars( max ), json.length() + " > " + maxWireChars( max ) );
        assertEquals( result, McpJson.readObject( json ) );
        final Map<String, Object> structured = (Map<String, Object>) result.get( "structuredContent" );
        final String text = text( result );
        assertEquals( McpJson.write( structured ), text.substring( text.indexOf( '\n' ) + 1 ) );
        assertWithinBudget( structured, max );
        return structured;
    }

    private static int maxWireChars( final int max )
    {
        return 3 * max + max / 4 + 100;
    }

    private static Map<String, Object> exact( final int length )
    {
        return Map.of( "text", "x".repeat( length - McpJson.length( Map.of( "text", "" ) ) ) );
    }

    private static void assertWithinBudget( final Map<String, Object> result, final int max )
    {
        final String json = McpJson.write( result );
        assertTrue( json.length() <= max, json.length() + " > " + max );
        if ( result.containsKey( "truncated" ) )
        {
            assertEquals( true, result.get( "truncated" ) );
            assertInstanceOf( String.class, result.get( "hint" ) );
        }
        else
        {
            assertNull( result.get( "hint" ) );
        }
    }

    private static String hint( final Map<String, Object> result )
    {
        return (String) result.get( "hint" );
    }
}
