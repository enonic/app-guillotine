package com.enonic.app.guillotine.mcp.shape;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.SerializationError;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;

import com.enonic.app.guillotine.graphql.fetchers.DynamicDataFetcher;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.exception.NotFoundException;
import com.enonic.xp.query.QueryException;

/**
 * GraphQL errors as tools return them. What a failed field may tell the caller is decided once, by {@link #hideInternalErrors()} at
 * execution; {@link #partialErrors} then only reduces the errors to {@code {message, path}} and caps their number and size.
 */
public final class GraphQlErrors
{
    public static final int MAX_ERRORS = 10;

    public static final int MAX_MESSAGE_CHARS = 300;

    static final String INTERNAL_ERROR = "Internal error while resolving this field; see the server log.";

    static final String SEARCH_FAILED =
        "Search failed: offset or after reaches beyond the first 10000 results, or the index cannot run this query. " +
            "Page less deep, or narrow the query with a filter.";

    static final String SEARCH_TIMED_OUT = "Search timed out. Narrow the query with a filter, or ask for fewer results or aggregations.";

    static final String ACCESS_DENIED = "Access denied: the caller cannot read this content.";

    private static final String ACCESS_DENIED_PREFIX = "Access denied:";

    private static final String SEARCH_FAILED_PREFIX = "Search request failed";

    private static final int MAX_PATH_SEGMENT_CHARS = 100;

    private static final String EXTENSION_RESOLVER = DynamicDataFetcher.class.getName();

    private static final Logger LOG = LoggerFactory.getLogger( GraphQlErrors.class );

    private static final Instrumentation HIDE_INTERNAL_ERRORS = new HideInternalErrors();

    private GraphQlErrors()
    {
    }

    /**
     * An instrumentation that rewrites the error of every field whose resolver threw: a caller error (see {@link #callerError})
     * keeps only its own message, without exception class or fetch prefix; anything else, including exceptions from resolvers of
     * Guillotine extensions and stored values a scalar cannot serialize, reads {@link #INTERNAL_ERROR} and is logged.
     */
    public static Instrumentation hideInternalErrors()
    {
        return HIDE_INTERNAL_ERRORS;
    }

    /**
     * The errors of a GraphQL response that has data, reduced to {@code {message, path}}: at most {@link #MAX_ERRORS}, each message
     * cut to {@link #MAX_MESSAGE_CHARS}, with a last {@code {message: "and N more errors"}} for the rest. A response with errors and
     * no data (see {@link #hasData}) is a {@link ToolError} instead; when access was denied, its message alone.
     */
    public static List<Map<String, Object>> partialErrors( final Object data, final List<?> errors )
    {
        final List<Map<String, Object>> reduced = reduce( errors );
        if ( !reduced.isEmpty() && !hasData( data ) )
        {
            final String denied = reduced.stream()
                .map( error -> String.valueOf( error.get( "message" ) ) )
                .filter( message -> message.startsWith( ACCESS_DENIED_PREFIX ) )
                .findFirst()
                .orElse( null );
            final String failed = "Query failed: " + describe( reduced );
            throw new ToolError( denied != null ? denied : failed.endsWith( "." ) ? failed : failed + ".", null );
        }
        return reduced;
    }

    /**
     * Whether GraphQL {@code data} holds a value: a root object such as {@code guillotine} counts only when one of its fields is
     * non-null, since GraphQL keeps it when every field below it failed.
     */
    public static boolean hasData( final Object data )
    {
        if ( data instanceof Map<?, ?> map )
        {
            return map.values()
                .stream()
                .anyMatch( value -> value instanceof Map<?, ?> object ? object.values().stream().anyMatch( Objects::nonNull ) : value != null );
        }
        return data != null;
    }

    static List<Map<String, Object>> reduce( final List<?> errors )
    {
        final List<Map<String, Object>> result = new ArrayList<>();
        if ( errors == null )
        {
            return result;
        }
        for ( final Object error : errors )
        {
            if ( result.size() == MAX_ERRORS )
            {
                result.add( Map.of( "message", "and " + ( errors.size() - MAX_ERRORS ) + " more errors" ) );
                break;
            }
            if ( error instanceof GraphQLError graphQLError )
            {
                result.add( entry( graphQLError.getMessage(), graphQLError.getPath() ) );
            }
            else if ( error instanceof Map<?, ?> map )
            {
                result.add( entry( map.get( "message" ), map.get( "path" ) ) );
            }
        }
        return result;
    }

    private static String describe( final List<Map<String, Object>> errors )
    {
        return errors.stream().map( error -> {
            final String message = String.valueOf( error.get( "message" ) );
            return error.get( "path" ) instanceof List<?> path
                ? path.stream().map( String::valueOf ).collect( Collectors.joining( "." ) ) + ": " + message
                : message;
        } ).collect( Collectors.joining( "; " ) );
    }

    private static Map<String, Object> entry( final Object message, final Object path )
    {
        final Map<String, Object> entry = new LinkedHashMap<>();
        entry.put( "message", message == null ? "Unknown error" : McpJson.shorten( message.toString(), MAX_MESSAGE_CHARS ) );
        if ( path instanceof List<?> list && !list.isEmpty() )
        {
            entry.put( "path", list.stream()
                .map( segment -> segment instanceof String name ? McpJson.shorten( name, MAX_PATH_SEGMENT_CHARS ) : segment )
                .toList() );
        }
        return entry;
    }

    /**
     * The exception whose message tells the caller what to change, or {@code null}: invalid arguments, keys and dates, a query
     * expression XP cannot parse, missing content, denied access, and a search the index refused. A plain {@link RuntimeException}
     * that only wraps its cause, as some Guillotine resolvers throw, is judged by that cause. An invalid argument thrown while an
     * extension resolver ran is the extension's failure, not the caller's: its message can name Java classes.
     */
    static Throwable callerError( final Throwable exception )
    {
        if ( exception == null )
        {
            return null;
        }
        if ( exception.getClass() == RuntimeException.class && exception.getCause() != null &&
            ( exception.getMessage() == null || exception.getMessage().equals( exception.getCause().toString() ) ) )
        {
            return callerError( exception.getCause() );
        }
        final boolean caller = ( exception instanceof IllegalArgumentException && !fromExtensionResolver( exception ) ) ||
            exception instanceof DateTimeException || exception instanceof NotFoundException || exception instanceof ForbiddenAccessException ||
            exception instanceof IllegalAccessException || exception instanceof QueryException || isSearchFailure( exception );
        return caller ? exception : null;
    }

    /**
     * The message a caller error shows. The index echoes the whole search request in its message, so a refused search reads as a
     * fixed text; XP names the user in a denied access, so that reads as {@link ProjectBranch#accessDenied()} of the context.
     */
    static String callerMessage( final Throwable exception )
    {
        if ( exception instanceof ForbiddenAccessException )
        {
            final ProjectBranch target = ProjectBranch.current();
            return target != null ? target.accessDenied().userMessage() : ACCESS_DENIED;
        }
        if ( isSearchFailure( exception ) )
        {
            return exception.getMessage().startsWith( SEARCH_FAILED_PREFIX + " after" ) ? SEARCH_TIMED_OUT : SEARCH_FAILED;
        }
        final String message = exception.getMessage();
        return message == null || message.isBlank() ? "Invalid request for this field." : message;
    }

    private static boolean fromExtensionResolver( final Throwable exception )
    {
        return Arrays.stream( exception.getStackTrace() ).anyMatch( frame -> EXTENSION_RESOLVER.equals( frame.getClassName() ) );
    }

    private static boolean isSearchFailure( final Throwable exception )
    {
        return exception.getMessage() != null && exception.getMessage().startsWith( SEARCH_FAILED_PREFIX );
    }

    private static final class HideInternalErrors
        extends SimplePerformantInstrumentation
    {
        @Override
        public CompletableFuture<ExecutionResult> instrumentExecutionResult( final ExecutionResult executionResult,
                                                                             final InstrumentationExecutionParameters parameters,
                                                                             final InstrumentationState state )
        {
            if ( executionResult.getErrors().stream().noneMatch( HideInternalErrors::rewritten ) )
            {
                return CompletableFuture.completedFuture( executionResult );
            }
            final List<GraphQLError> errors = executionResult.getErrors().stream().map( HideInternalErrors::rewrite ).toList();
            return CompletableFuture.completedFuture( executionResult.transform( builder -> builder.errors( errors ) ) );
        }

        private static boolean rewritten( final GraphQLError error )
        {
            return error instanceof ExceptionWhileDataFetching || error instanceof SerializationError;
        }

        private static GraphQLError rewrite( final GraphQLError error )
        {
            if ( error instanceof ExceptionWhileDataFetching fetching )
            {
                final Throwable caller = callerError( fetching.getException() );
                return caller != null ? rebuilt( error, callerMessage( caller ) ) : internal( error, fetching.getException() );
            }
            if ( error instanceof SerializationError serialization )
            {
                return internal( error, serialization.getException() );
            }
            return error;
        }

        private static GraphQLError internal( final GraphQLError error, final Throwable exception )
        {
            LOG.warn( "MCP query field {} failed", error.getPath(), exception );
            return rebuilt( error, INTERNAL_ERROR );
        }

        private static GraphQLError rebuilt( final GraphQLError error, final String message )
        {
            return GraphqlErrorBuilder.newError().message( message ).path( error.getPath() ).locations( error.getLocations() ).build();
        }
    }
}
