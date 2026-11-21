package com.enonic.app.guillotine.mcp.shape;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.GraphQLContext;
import graphql.schema.Coercing;
import graphql.schema.CoercingSerializeException;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.fetchers.DynamicDataFetcher;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.node.NodeNotFoundException;
import com.enonic.xp.query.QueryException;
import com.enonic.xp.repository.IndexException;
import com.enonic.xp.script.ScriptValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphQlErrorsTest
{
    private static final GraphQLScalarType STORED_DATE = GraphQLScalarType.newScalar()
        .name( "StoredDate" )
        .coercing( new Coercing<Object, Object>()
        {
            @Override
            public Object serialize( final Object value, final GraphQLContext context, final Locale locale )
            {
                throw new CoercingSerializeException( new DateTimeParseException( "Text could not be parsed", value.toString(), 5 ) );
            }
        } )
        .build();

    private static final Map<String, RuntimeException> FAILURES = new LinkedHashMap<>();

    static
    {
        FAILURES.put( "npe", new NullPointerException( "Cannot invoke \"com.enonic.xp.content.Content.getId()\" because \"c\" is null" ) );
        FAILURES.put( "state", new IllegalStateException( "secret at com.enonic.xp.core.impl.Foo.run(Foo.java:12)" ) );
        FAILURES.put( "closed", new IndexException( "Index [search-com.enonic.cms.blog] is closed" ) );
        FAILURES.put( "wrappedState", new RuntimeException( "context", new IllegalArgumentException( "x" ) ) );
        FAILURES.put( "argument", new IllegalArgumentException( "first must be positive" ) );
        FAILURES.put( "wrapped", new RuntimeException( new IllegalArgumentException( "Bad key" ) ) );
        FAILURES.put( "notFound", new NodeNotFoundException( "Node /a not found" ) );
        FAILURES.put( "date", new DateTimeParseException( "Text '2024-13' could not be parsed", "2024-13", 5 ) );
        FAILURES.put( "query", new QueryException( "Unexpected token at line 1, column 9" ) );
        FAILURES.put( "percent", new IllegalArgumentException( "100% sure" ) );
        FAILURES.put( "blank", new IllegalArgumentException() );
        FAILURES.put( "number", new NumberFormatException( "For input string: \"abc\"" ) );
        FAILURES.put( "base64", base64Failure() );
        FAILURES.put( "search", new IndexException( "Search request failed, query: [{ \"from\" : 50000, \"query\" : { \"_nodetype\" : 1 } }]" ) );
        FAILURES.put( "timeout", new IndexException( "Search request failed after [30s], query: [{ \"match_all\" : { } }]" ) );
        FAILURES.put( "forbidden", new ForbiddenAccessException(
            User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).build() ) );
    }

    @Test
    public void testInstrumentationKeepsOnlyCallerMessages()
    {
        assertEquals( GraphQlErrors.INTERNAL_ERROR, message( "npe" ) );
        assertEquals( GraphQlErrors.INTERNAL_ERROR, message( "state" ) );
        assertEquals( GraphQlErrors.INTERNAL_ERROR, message( "closed" ) );
        assertEquals( GraphQlErrors.INTERNAL_ERROR, message( "wrappedState" ) );
        assertEquals( "first must be positive", message( "argument" ) );
        assertEquals( "Bad key", message( "wrapped" ) );
        assertEquals( "Node /a not found", message( "notFound" ) );
        assertEquals( "Text '2024-13' could not be parsed", message( "date" ) );
        assertEquals( "Unexpected token at line 1, column 9", message( "query" ) );
        assertEquals( "100% sure", message( "percent" ) );
        assertEquals( "Invalid request for this field.", message( "blank" ) );
        assertEquals( GraphQlErrors.UNREADABLE_ARGUMENT, message( "number" ) );
        assertEquals( GraphQlErrors.UNREADABLE_ARGUMENT, message( "base64" ) );
        assertEquals( GraphQlErrors.SEARCH_FAILED, message( "search" ) );
        assertEquals( GraphQlErrors.SEARCH_TIMED_OUT, message( "timeout" ) );
        assertEquals( GraphQlErrors.ACCESS_DENIED, message( "forbidden" ) );
    }

    @Test
    public void testDeniedAccessNamesTheProjectAndBranchOfTheContext()
    {
        final AuthenticationInfo alice = AuthenticationInfo.create()
            .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).build() )
            .principals( RoleKeys.AUTHENTICATED )
            .build();
        final String expected = "Access denied: the caller cannot read branch draft of project blog. Pass branch master to read " +
            "published content, or ask an administrator for a role in project blog.";

        final String partial = ContextBuilder.copyOf( ContextAccessor.current() ).authInfo( alice ).build().callWith(
            () -> new ProjectBranch( "blog", "draft" ).callWith( () -> message( "forbidden" ) ) );
        assertEquals( expected, partial );

        final ToolError failed = assertThrows( ToolError.class, () -> ContextBuilder.copyOf( ContextAccessor.current() )
            .authInfo( alice )
            .build()
            .callWith( () -> new ProjectBranch( "blog", "draft" ).callWith( () -> {
                final Map<String, Object> response = execute( "{ fail(kind: \"forbidden\") }" );
                return GraphQlErrors.partialErrors( response.get( "data" ), (List<?>) response.get( "errors" ) );
            } ) ) );
        assertEquals( expected, failed.userMessage() );
    }

    @Test
    public void testNoJavaNameOrIndexRequestReachesTheResult()
    {
        for ( final String kind : FAILURES.keySet() )
        {
            final Map<String, Object> response = execute( "{ ok fail(kind: \"" + kind + "\") }" );
            final String json =
                McpJson.write( GraphQlErrors.partialErrors( response.get( "data" ), (List<?>) response.get( "errors" ) ) );

            for ( final String leak : List.of( "Exception", "java.", "For input string", "base64", "com.enonic", "Foo.java", "_nodetype", "50000", "fetching data",
                                               "locations", "extensions" ) )
            {
                assertFalse( json.contains( leak ), () -> leak + " in " + json );
            }
        }
    }

    @Test
    public void testUnserializableValueIsInternalError()
    {
        final Map<String, Object> response = execute( "{ ok date }" );

        final List<Map<String, Object>> errors =
            GraphQlErrors.partialErrors( response.get( "data" ), (List<?>) response.get( "errors" ) );

        assertEquals( List.of( Map.of( "message", GraphQlErrors.INTERNAL_ERROR, "path", List.of( "date" ) ) ), errors );
    }

    @Test
    public void testInvalidArgumentFromExtensionResolverIsInternalError()
    {
        final Map<String, Object> response = execute( "{ ok extension }" );

        final List<Map<String, Object>> errors =
            GraphQlErrors.partialErrors( response.get( "data" ), (List<?>) response.get( "errors" ) );

        assertEquals( List.of( Map.of( "message", GraphQlErrors.INTERNAL_ERROR, "path", List.of( "extension" ) ) ), errors );
    }

    @Test
    public void testErrorsWithoutDataAreToolError()
    {
        final Map<String, Object> response = execute( "{ fail(kind: \"argument\") }" );

        assertNull( ( (Map<?, ?>) response.get( "data" ) ).get( "fail" ) );
        final ToolError error = assertThrows( ToolError.class, () -> GraphQlErrors.partialErrors( response.get( "data" ),
                                                                                                   (List<?>) response.get( "errors" ) ) );
        assertEquals( "Query failed: fail: first must be positive.", error.userMessage() );
    }

    @Test
    public void testResponseWithoutErrorsHasNoErrors()
    {
        final Map<String, Object> response = execute( "{ ok }" );

        assertTrue( GraphQlErrors.partialErrors( response.get( "data" ), (List<?>) response.get( "errors" ) ).isEmpty() );
        assertTrue( GraphQlErrors.partialErrors( null, null ).isEmpty() );
        assertSame( GraphQlErrors.hideInternalErrors(), GraphQlErrors.hideInternalErrors() );
    }

    @Test
    public void testRootObjectWithoutValuesIsNoData()
    {
        final Map<String, Object> failed = new LinkedHashMap<>();
        failed.put( "queryDsl", null );

        assertFalse( GraphQlErrors.hasData( null ) );
        assertFalse( GraphQlErrors.hasData( Map.of( "guillotine", Map.of() ) ) );
        assertFalse( GraphQlErrors.hasData( Map.of( "guillotine", failed ) ) );
        assertTrue( GraphQlErrors.hasData( Map.of( "guillotine", Map.of( "queryDsl", List.of() ) ) ) );
        assertTrue( GraphQlErrors.hasData( Map.of( "a", failed, "b", Map.of( "get", Map.of() ) ) ) );
        assertTrue( GraphQlErrors.hasData( Map.of( "scalar", 1 ) ) );
    }

    @Test
    public void testErrorsAreCappedInCountMessageAndPath()
    {
        final List<Map<String, Object>> raw = new ArrayList<>();
        for ( int i = 0; i < 15; i++ )
        {
            raw.add( Map.of( "message", "m".repeat( 1000 ), "path", List.of( "a".repeat( 500 ), i ), "extensions", Map.of( "x", 1 ) ) );
        }

        final List<Map<String, Object>> errors = GraphQlErrors.partialErrors( Map.of( "ok", 1 ), raw );

        assertEquals( GraphQlErrors.MAX_ERRORS + 1, errors.size() );
        assertEquals( Map.of( "message", "and 5 more errors" ), errors.get( GraphQlErrors.MAX_ERRORS ) );
        assertEquals( GraphQlErrors.MAX_MESSAGE_CHARS, ( (String) errors.get( 0 ).get( "message" ) ).length() );
        assertEquals( 100, ( (String) ( (List<?>) errors.get( 0 ).get( "path" ) ).get( 0 ) ).length() );
        assertEquals( 3, ( (List<?>) errors.get( 3 ).get( "path" ) ).get( 1 ) );
        assertEquals( Map.of( "message", "Unknown error" ), GraphQlErrors.partialErrors( Map.of( "ok", 1 ), List.of( Map.of() ) ).get( 0 ) );
    }

    @Test
    public void testOnlyCallerErrorClassesKeepTheirMessage()
    {
        assertTrue( GraphQlErrors.callerError( new ForbiddenAccessException( null ) ) instanceof ForbiddenAccessException );
        assertTrue( GraphQlErrors.callerError( new IllegalAccessException( "Unauthorized" ) ) instanceof IllegalAccessException );
        assertTrue( GraphQlErrors.callerError(
            new RuntimeException( new RuntimeException( new IllegalArgumentException( "x" ) ) ) ) instanceof IllegalArgumentException );
        assertNull( GraphQlErrors.callerError( new IllegalStateException( new IllegalArgumentException( "x" ) ) ) );
        assertNull( GraphQlErrors.callerError( null ) );
    }

    private static IllegalArgumentException base64Failure()
    {
        try
        {
            Base64.getDecoder().decode( "%%%" );
            throw new AssertionError( "decoded" );
        }
        catch ( IllegalArgumentException e )
        {
            return e;
        }
    }

    private static String message( final String kind )
    {
        final Map<String, Object> response = execute( "{ ok fail(kind: \"" + kind + "\") }" );
        final List<Map<String, Object>> errors =
            GraphQlErrors.partialErrors( response.get( "data" ), (List<?>) response.get( "errors" ) );
        assertEquals( 1, errors.size() );
        assertEquals( List.of( "fail" ), errors.get( 0 ).get( "path" ) );
        return (String) errors.get( 0 ).get( "message" );
    }

    private static Map<String, Object> execute( final String query )
    {
        final ScriptValue extensionResolver = mock( ScriptValue.class );
        when( extensionResolver.call( any() ) ).thenAnswer( invocation -> {
            throw new IllegalArgumentException( "Unsupported type \"org.openjdk.nashorn.api.scripting.ScriptObjectMirror\". " +
                                                    "Type of value must be String, Double, Integer or Boolean." );
        } );
        final GraphQLCodeRegistry registry = GraphQLCodeRegistry.newCodeRegistry()
            .dataFetcher( FieldCoordinates.coordinates( "Query", "extension" ), new DynamicDataFetcher( extensionResolver ) )
            .dataFetcher( FieldCoordinates.coordinates( "Query", "ok" ), (DataFetcher<?>) environment -> "fine" )
            .dataFetcher( FieldCoordinates.coordinates( "Query", "date" ), (DataFetcher<?>) environment -> "2024-13-45" )
            .dataFetcher( FieldCoordinates.coordinates( "Query", "fail" ), (DataFetcher<?>) environment -> {
                throw FAILURES.get( environment.<String>getArgument( "kind" ) );
            } )
            .build();
        final GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name( "Query" )
            .field( field -> field.name( "ok" ).type( Scalars.GraphQLString ) )
            .field( field -> field.name( "date" ).type( STORED_DATE ) )
            .field( field -> field.name( "extension" ).type( Scalars.GraphQLString ) )
            .field( field -> field.name( "fail" )
                .type( Scalars.GraphQLString )
                .argument( argument -> argument.name( "kind" ).type( Scalars.GraphQLString ) ) )
            .build();
        final GraphQLSchema schema = GraphQLSchema.newSchema().query( queryType ).codeRegistry( registry ).build();
        return GraphQL.newGraphQL( schema ).instrumentation( GraphQlErrors.hideInternalErrors() ).build().execute( query ).toSpecification();
    }
}
