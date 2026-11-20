package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class McpResultTest
{
    @Test
    public void testSuccessTextIsTheCompactJsonOfStructuredContent()
    {
        final Map<String, Object> structured = new LinkedHashMap<>();
        new ProjectBranch( "blog", "master" ).putInto( structured );
        structured.put( "missing", null );

        final Map<String, Object> result = McpResult.success( structured );

        assertEquals( "{\"project\":\"blog\",\"branch\":\"master\"}", text( result ) );
        assertEquals( Map.of( "project", "blog", "branch", "master" ), structured( result ) );
        assertFalse( result.containsKey( "isError" ) );
    }

    @Test
    public void testNullFieldsAndNullArrayItemsAreRemoved()
    {
        final Map<String, Object> item = new LinkedHashMap<>();
        item.put( "a", null );
        item.put( "b", "x" );
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "list", new ArrayList<>( Arrays.asList( "a", null, item, Arrays.asList( null, 1 ) ) ) );
        input.put( "gone", null );
        input.put( "empty", Map.of() );

        final Map<String, Object> cleaned = Nulls.clean( input );

        assertEquals( Map.of( "list", List.of( "a", Map.of( "b", "x" ), List.of( 1 ) ), "empty", Map.of() ), cleaned );
        assertEquals( "{\"list\":[\"a\",{\"b\":\"x\"},[1]],\"empty\":{}}", text( McpResult.success( input ) ) );
    }

    @Test
    public void testKeepingNullFieldsStillDropsNullArrayItems()
    {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "get", null );
        data.put( "items", Arrays.asList( null, Map.of( "x", 1 ) ) );

        assertEquals( "{\"get\":null,\"items\":[{\"x\":1}]}", McpJson.write( Nulls.keepingNullFields( data ) ) );
    }

    @Test
    public void testToolErrorBecomesErrorResultWithHint()
    {
        final Map<String, Object> result = McpResult.error( new ToolError( "Invalid cursor.", "Repeat the call without cursor." ) );

        assertEquals( Map.of( "content", List.of( Map.of( "type", "text", "text", "Invalid cursor. Repeat the call without cursor." ) ),
                              "isError", true ), result );
        assertEquals( "Bad.", new ToolError( "Bad.", null ).userMessage() );
        assertEquals( "Bad.", new ToolError( "Bad.", "" ).userMessage() );
    }

    @Test
    public void testErrorResultIsCappedWithEscaping()
    {
        final Map<String, Object> result = McpResult.error( "\"quoted\\\" ".repeat( 2000 ) );

        assertTrue( McpJson.length( result ) <= ResultBudget.MIN_RESULT_CHARS, () -> "length " + McpJson.length( result ) );
        assertTrue( McpJson.length( result ) > ResultBudget.MIN_RESULT_CHARS / 2 );
        assertTrue( text( result ).endsWith( "… (message truncated)" ) );
    }

    @Test
    public void testErrorIsNotCutInsideASurrogatePair()
    {
        final String text = text( McpResult.error( "😀".repeat( 1000 ) ) );

        assertFalse( Character.isHighSurrogate( text.charAt( text.length() - "… (message truncated)".length() - 1 ) ) );
    }

    @Test
    public void testPreviewAndShorten()
    {
        assertEquals( McpJson.PREVIEW_CHARS, McpJson.preview( "x".repeat( 5000 ) ).length() );
        assertEquals( "\"ab\"", McpJson.preview( "ab" ) );
        assertEquals( "ab…", McpJson.shorten( "abcdef", 3 ) );
        assertEquals( "a…", McpJson.shorten( "a😀b", 3 ) );
        assertEquals( null, McpJson.shorten( null, 3 ) );
    }
}
