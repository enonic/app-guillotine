package com.enonic.app.guillotine.mcp.shape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.GraphqlErrorBuilder;
import graphql.execution.ResultPath;

import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class McpResultTest
{
    private static final String INTERNAL = "Internal error while resolving this field; see the server log.";

    @Test
    public void testNestedNullFieldsAreRemovedAndArrayPositionsKept()
    {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put( "keep", 1 );
        inner.put( "drop", null );
        inner.put( "empty", Map.of() );
        final Map<String, Object> item = new LinkedHashMap<>();
        item.put( "a", null );
        item.put( "b", "x" );
        final Map<String, Object> input = new LinkedHashMap<>();
        input.put( "inner", inner );
        input.put( "list", new ArrayList<>( Arrays.asList( "a", null, item, List.of() ) ) );
        input.put( "gone", null );

        final Map<String, Object> cleaned = Nulls.clean( input );

        assertEquals( Map.of( "keep", 1, "empty", Map.of() ), cleaned.get( "inner" ) );
        assertEquals( Arrays.asList( "a", null, Map.of( "b", "x" ), List.of() ), cleaned.get( "list" ) );
        assertFalse( cleaned.containsKey( "gone" ) );
    }

    @Test
    public void testSuccessTextIsSummaryAndJsonOfStructuredContent()
    {
        final Map<String, Object> structured = new LinkedHashMap<>();
        structured.put( "project", "blog" );
        structured.put( "branch", "master" );
        structured.put( "missing", null );

        final Map<String, Object> result = McpResult.success( "Found 1 item.", structured );

        assertEquals( Map.of( "project", "blog", "branch", "master" ), result.get( "structuredContent" ) );
        assertEquals( "Found 1 item.\n{\"project\":\"blog\",\"branch\":\"master\"}", text( result ) );
        assertFalse( result.containsKey( "isError" ) );
    }

    @Test
    public void testBudgetedSuccess()
    {
        final Map<String, Object> result = McpResult.success( "Big.", Map.of( "body", "x".repeat( 5000 ) ), ResultBudget.of( 1000 ) );

        final Map<?, ?> structured = (Map<?, ?>) result.get( "structuredContent" );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( "Big.\n" + McpJson.write( structured ), text( result ) );
    }

    @Test
    public void testBudgetedPagedSuccess()
    {
        final Page page = new Page( 0, 10, "h" );
        final Map<String, Object> result =
            McpResult.success( "Page.", Map.of( "hits", List.of( 1, 2 ) ), ResultBudget.of( 1000 ), "hits", page, 2 );

        assertEquals( Map.of( "hits", List.of( 1, 2 ) ), result.get( "structuredContent" ) );
    }

    @Test
    public void testErrorFormat()
    {
        final Map<String, Object> result = McpResult.error( "Something failed." );

        assertEquals( Map.of( "content", List.of( Map.of( "type", "text", "text", "Something failed." ) ), "isError", true ), result );
    }

    @Test
    public void testToolErrorCarriesHint()
    {
        assertEquals( "Bad count. Use 0 to 50.", text( McpResult.error( new ToolError( "Bad count.", "Use 0 to 50." ) ) ) );
        assertEquals( "Bad count.", text( McpResult.error( new ToolError( "Bad count." ) ) ) );
        assertEquals( "Bad count.", new ToolError( "Bad count.", "" ).userMessage() );
    }

    @Test
    public void testPartialGraphQlErrorsGoNextToData()
    {
        final List<Object> errors = List.of( Map.of( "message",
                                                     "Exception while fetching data (/guillotine/get) : java.lang.IllegalArgumentException: Bad key",
                                                     "path", List.of( "guillotine", "get" ), "locations", List.of(),
                                                     "extensions", Map.of( "classification", "DataFetchingException" ) ) );
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "get", null );
        data.put( "query", List.of() );

        final Map<String, Object> result =
            McpResult.fromGraphQl( "Partial.", Map.of( "project", "blog", "items", List.of() ), data, errors, ResultBudget.of( 20000 ) );

        assertFalse( result.containsKey( "isError" ) );
        assertEquals( Map.of( "project", "blog", "items", List.of(), "errors",
                              List.of( Map.of( "message", "Bad key", "path", List.of( "guillotine", "get" ) ) ) ),
                      result.get( "structuredContent" ) );
        assertFalse( text( result ).contains( "Exception" ), text( result ) );
    }

    @Test
    public void testGraphQlErrorsWithoutDataAreError()
    {
        final List<Object> errors = List.of( GraphqlErrorBuilder.newError()
                                                 .message( "Validation error: Field 'nope' is undefined" )
                                                 .path( ResultPath.parse( "/guillotine/nope" ) )
                                                 .build(), Map.of( "message", "second" ) );
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "guillotine", null );

        final Map<String, Object> result = McpResult.fromGraphQl( "x", Map.of(), data, errors, ResultBudget.of( 20000 ) );

        assertEquals( true, result.get( "isError" ) );
        assertEquals( "Query failed: guillotine.nope: Validation error: Field 'nope' is undefined; second", text( result ) );
        assertEquals( true, McpResult.fromGraphQl( "x", Map.of(), null, errors, ResultBudget.of( 20000 ) ).get( "isError" ) );
    }

    @Test
    public void testValidationFailureEndsItsSentenceBeforeTheHint()
    {
        final List<Map<String, Object>> undefined = GraphQlErrors.sanitize( List.of( Map.of( "message",
            "Validation error (FieldUndefined@[guillotine/nope]) : Field 'nope' in type 'HeadlessCms' is undefined" ) ) );
        assertEquals( "Query failed: Validation error (FieldUndefined@[guillotine/nope]) : Field 'nope' in type 'HeadlessCms' is " +
                          "undefined. " + GraphQlErrors.VALIDATION_HINT, GraphQlErrors.failure( undefined ) );

        final List<Map<String, Object>> withPeriod =
            GraphQlErrors.sanitize( List.of( Map.of( "message", "Validation error (WrongType@[guillotine]) : Bad value." ) ) );
        assertEquals( "Query failed: Validation error (WrongType@[guillotine]) : Bad value. " + GraphQlErrors.VALIDATION_HINT,
                      GraphQlErrors.failure( withPeriod ) );
    }

    @Test
    public void testRootObjectWithoutValuesIsNoData()
    {
        final List<Object> errors = List.of( Map.of( "message", "Invalid argument \"first\"", "path", List.of( "guillotine", "queryDsl" ) ) );
        final Map<String, Object> failed = new LinkedHashMap<>();
        failed.put( "queryDsl", null );

        for ( final Object data : List.of( Map.of( "guillotine", Map.of() ), Map.of( "guillotine", failed ) ) )
        {
            final Map<String, Object> result = McpResult.fromGraphQl( "x", Map.of(), data, errors, ResultBudget.of( 20000 ) );
            assertEquals( true, result.get( "isError" ), data::toString );
            assertEquals( "Query failed: guillotine.queryDsl: Invalid argument \"first\"", text( result ) );
        }

        assertFalse( McpResult.hasData( Map.of( "guillotine", Map.of() ) ) );
        assertTrue( McpResult.hasData( Map.of( "guillotine", Map.of( "queryDsl", List.of() ) ) ) );
        assertTrue( McpResult.hasData( Map.of( "a", failed, "b", Map.of( "get", Map.of() ) ) ) );
        assertTrue( McpResult.hasData( Map.of( "scalar", 1 ) ) );
    }

    @Test
    public void testNoDataAndNoErrorsIsEmptySuccess()
    {
        final Map<String, Object> result = McpResult.fromGraphQl( "Nothing.", Map.of( "items", List.of() ), null, null,
                                                                  ResultBudget.of( 20000 ) );

        assertEquals( Map.of( "items", List.of() ), result.get( "structuredContent" ) );
        assertEquals( Map.of( "items", List.of() ),
                      McpResult.fromGraphQl( "Ok.", Map.of( "items", List.of() ), "scalar", List.of(), ResultBudget.of( 20000 ) )
                          .get( "structuredContent" ) );
    }

    @Test
    public void testGraphQlErrorMessagesAreCleaned()
    {
        final List<Map<String, Object>> errors =
            GraphQlErrors.sanitize( Arrays.asList( Map.of( "message", "com.enonic.xp.node.NodeNotFoundException: Node not found" ),
                                                   Map.of( "path", List.of() ), "plain", null,
                                                   Map.of( "message", "java.lang.NullPointerException" ) ) );

        assertEquals( List.of( Map.of( "message", "Node not found" ), Map.of( "message", "Unknown error" ), Map.of( "message", "plain" ),
                               Map.of( "message", INTERNAL ) ), errors );
        assertTrue( GraphQlErrors.sanitize( null ).isEmpty() );
        assertEquals( "a: x", GraphQlErrors.describe( List.of( Map.of( "message", "x", "path", List.of( "a" ) ) ) ) );
    }

    @Test
    public void testInternalAndClassNameFormsAreSanitized()
    {
        assertEquals( "Node /a/b not found", GraphQlErrors.cleanMessage( "NodeNotFoundException: Node /a/b not found" ) );
        assertEquals( INTERNAL, GraphQlErrors.cleanMessage(
            "Cannot invoke \"com.enonic.xp.content.Content.getId()\" because \"content\" is null" ) );
        assertEquals( INTERNAL, GraphQlErrors.cleanMessage(
            "class java.lang.String cannot be cast to class java.util.Map (java.lang.String and java.util.Map are in module java.base " +
                "of loader 'bootstrap')" ) );
        assertEquals( INTERNAL, GraphQlErrors.cleanMessage(
            "Exception while fetching data (/guillotine/get) : java.lang.ClassCastException" ) );
        assertEquals( INTERNAL, GraphQlErrors.cleanMessage( "Index 3 out of bounds for length 2 (IndexOutOfBoundsException)" ) );
        assertEquals( "Failed in for key", GraphQlErrors.cleanMessage( "Failed in com.enonic.app.guillotine.Foo.bar() for key" ) );
        assertEquals( "Access denied", GraphQlErrors.cleanMessage( "ForbiddenAccessException Access denied" ) );
        assertEquals( "Validation error (FieldUndefined@[guillotine/nope]) : Field 'nope' in type 'HeadlessCms' is undefined",
                      GraphQlErrors.cleanMessage(
                          "Validation error (FieldUndefined@[guillotine/nope]) : Field 'nope' in type 'HeadlessCms' is undefined" ) );
        for ( final String message : List.of( "NodeNotFoundException: x", "com.enonic.xp.node.NodeNotFoundException: x",
                                              "Wrapped: java.lang.IllegalStateException: x", "at com.enonic.xp.core.impl.Foo.run(Foo.java)" ) )
        {
            final String cleaned = GraphQlErrors.cleanMessage( message );
            assertFalse( cleaned.matches( "(?s).*(Exception|com\\.enonic|java\\.lang).*" ), cleaned );
        }
    }

    @Test
    public void testDottedNamesThatAreNotJavaNamesAreKept()
    {
        assertEquals( "Content type com.example.app:article is not allowed under /site",
                      GraphQlErrors.cleanMessage( "Content type com.example.app:article is not allowed under /site" ) );
        assertEquals( "Cannot reach www.example.com or cdn.example.co.uk",
                      GraphQlErrors.cleanMessage( "Cannot reach www.example.com or cdn.example.co.uk" ) );
        assertEquals( "File photo.final.jpg is too large", GraphQlErrors.cleanMessage( "File photo.final.jpg is too large" ) );

        assertEquals( "Value is not a", GraphQlErrors.cleanMessage( "Value is not a java.util.Map" ) );
        assertEquals( "Failed in (Handler.java:12)",
                      GraphQlErrors.cleanMessage( "Failed in com.example.app.handler.run(Handler.java:12)" ) );
        assertEquals( "Failed in", GraphQlErrors.cleanMessage( "Failed in com.example.app.handler.Inner$1" ) );
        assertEquals( "Failed in for key", GraphQlErrors.cleanMessage( "Failed in com.example.app.handler.lookup() for key" ) );
        assertEquals( "Bad value", GraphQlErrors.cleanMessage( "com.example.app.InvalidValueException: Bad value" ) );
    }

    @Test
    public void testJsonHelpers()
    {
        assertEquals( Map.of(), McpJson.readObject( null ) );
        assertEquals( Map.of(), McpJson.readObject( "  " ) );
        assertThrows( IllegalArgumentException.class, () -> McpJson.readObject( "{" ) );
        assertThrows( IllegalArgumentException.class, () -> McpJson.readObject( "1" ) );
        assertEquals( "{\"a\":1,\"b\":{\"c\":2,\"d\":3}}",
                      McpJson.writeSorted( Map.of( "b", new LinkedHashMap<>( Map.of( "d", 3, "c", 2 ) ), "a", 1 ) ) );
        assertEquals( 7, McpJson.length( Map.of( "a", 1 ) ) );
        assertThrows( IllegalStateException.class, () -> McpJson.write( new Object() ) );
    }

    @Test
    public void testErrorResultIsCappedWithEscaping()
    {
        final Map<String, Object> result = McpResult.error( "\"quoted\\\" ".repeat( 2000 ) );

        assertTrue( McpJson.length( result ) <= ResultBudget.MIN_RESULT_CHARS, () -> "length " + McpJson.length( result ) );
        assertTrue( McpJson.length( result ) > ResultBudget.MIN_RESULT_CHARS / 2 );
        assertTrue( text( result ).endsWith( "… (message truncated)" ) );
        assertEquals( "short", text( McpResult.error( "short" ) ) );
    }

    @Test
    public void testSanitizeCapsCountMessagesAndPaths()
    {
        final List<Map<String, Object>> raw = new ArrayList<>();
        for ( int i = 0; i < 15; i++ )
        {
            raw.add( Map.of( "message", "m".repeat( 1000 ), "path", List.of( "a".repeat( 500 ), i ) ) );
        }

        final List<Map<String, Object>> errors = GraphQlErrors.sanitize( raw );

        assertEquals( GraphQlErrors.MAX_ERRORS + 1, errors.size() );
        assertEquals( Map.of( "message", "and 5 more errors" ), errors.get( GraphQlErrors.MAX_ERRORS ) );
        assertEquals( GraphQlErrors.MAX_MESSAGE_CHARS, ( (String) errors.get( 0 ).get( "message" ) ).length() );
        assertEquals( 100, ( (String) ( (List<?>) errors.get( 0 ).get( "path" ) ).get( 0 ) ).length() );
        assertEquals( 3, ( (List<?>) errors.get( 3 ).get( "path" ) ).get( 1 ) );
    }

    @Test
    public void testGraphQlFailureWithManyLongErrorsFitsBudget()
    {
        final List<Map<String, Object>> raw = new ArrayList<>();
        for ( int i = 0; i < 250; i++ )
        {
            raw.add( Map.of( "message", "Content not found: \"" + "k".repeat( 4096 ) + "\"", "path", List.of( "a" + i ) ) );
        }
        final Map<String, Object> structured = new LinkedHashMap<>();
        structured.put( "data", null );

        final Map<String, Object> result = McpResult.fromGraphQl( "Query", structured, null, raw, ResultBudget.of( 20000 ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( McpJson.length( result ) <= ResultBudget.MIN_RESULT_CHARS, () -> "length " + McpJson.length( result ) );
        assertTrue( text( result ).startsWith( "Query failed: a0: Content not found: \"kkk" ) );
    }

    @Test
    public void testPreviewAndShorten()
    {
        assertEquals( McpJson.PREVIEW_CHARS, McpJson.preview( "x".repeat( 5000 ) ).length() );
        assertEquals( "\"ab\"", McpJson.preview( "ab" ) );
        assertEquals( "ab…", McpJson.shorten( "abcdef", 3 ) );
        assertEquals( null, McpJson.shorten( null, 3 ) );
    }
}
