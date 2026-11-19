package com.enonic.app.guillotine.mcp.shape;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;

import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.exception.NotFoundException;

public final class GraphQlErrors
{
    public static final int MAX_ERRORS = 10;

    public static final int MAX_MESSAGE_CHARS = 300;

    private static final int MAX_PATH_SEGMENT_CHARS = 100;

    private static final String INTERNAL_ERROR = "Internal error while resolving this field; see the server log.";

    private static final Pattern INTERNAL = Pattern.compile(
        "NullPointerException|ClassCastException|IndexOutOfBounds|Cannot invoke \"|cannot be cast to|because \"[^\"]*\" is null|" +
            "Cannot (?:read|load|store) .* because" );

    private static final Pattern QUALIFIED_EXCEPTION =
        Pattern.compile( "\\b(?:[a-z_$][\\w$]*\\.)+[A-Z][\\w$]*(?:Exception|Error)\\b:?\\s*" );

    private static final Pattern EXCEPTION = Pattern.compile( "\\b[A-Z][\\w$]*(?:Exception|Error)\\b:?\\s*" );

    private static final Pattern QUALIFIED_NAME =
        Pattern.compile( "\\b[a-z][\\w$]*+(?:\\.[a-zA-Z_$][\\w$]*+){2,}+(\\(\\))?+(?=(\\()?)(?![:\\w$])" );

    private static final Pattern JAVA_NAME_PART = Pattern.compile( "\\$|\\.[A-Z_]" );

    private static final Pattern VALIDATION = Pattern.compile( "Validation error \\(\\w+@" );

    private static final int MAX_FAILURE_CHARS = 700;

    public static final String VALIDATION_HINT =
        "The root field is `guillotine { ... }`; call describe-type to see the fields of a content type.";

    private static final Pattern SEARCH_FAILED =
        Pattern.compile( "^(?:(?:[a-z_$][\\w$]*\\.)*[A-Z][\\w$]*(?:Exception|Error):\\s*)?Search request failed( after)?" );

    private static final String SEARCH_FAILED_MESSAGE =
        "Search failed: offset or after reaches beyond the first 10000 results, or the index cannot run this query. " +
            "Page less deep, or narrow the query with a filter.";

    private static final String SEARCH_TIMED_OUT_MESSAGE =
        "Search timed out. Narrow the query with a filter, or ask for fewer results or aggregations.";

    private static final Pattern FETCH_PREFIX = Pattern.compile( "^Exception while fetching data \\([^)]*\\)\\s*:\\s*" );

    private static final Logger LOG = LoggerFactory.getLogger( GraphQlErrors.class );

    private static final Instrumentation HIDE_INTERNAL_ERRORS = new HideInternalErrors();

    private GraphQlErrors()
    {
    }

    /**
     * An instrumentation that replaces the message of a field that failed with an exception other than a caller error (see
     * {@link #isCallerError(Throwable)}) by a generic text, and logs the exception. Resolvers of Guillotine extensions can throw
     * anything, with any detail in the message.
     */
    public static Instrumentation hideInternalErrors()
    {
        return HIDE_INTERNAL_ERRORS;
    }

    /**
     * Exceptions whose message tells the caller what to change: invalid arguments and keys, dates, missing content, denied access,
     * and a search the index refused. A plain {@link RuntimeException} that only wraps its cause, as some Guillotine resolvers throw,
     * is judged by that cause.
     */
    static boolean isCallerError( final Throwable exception )
    {
        if ( exception != null && exception.getClass() == RuntimeException.class && exception.getCause() != null &&
            ( exception.getMessage() == null || exception.getMessage().equals( exception.getCause().toString() ) ) )
        {
            return isCallerError( exception.getCause() );
        }
        return exception instanceof IllegalArgumentException || exception instanceof DateTimeException ||
            exception instanceof NotFoundException || exception instanceof ForbiddenAccessException ||
            exception instanceof IllegalAccessException ||
            exception != null && exception.getMessage() != null && SEARCH_FAILED.matcher( exception.getMessage() ).lookingAt();
    }

    /**
     * Reduces GraphQL errors (spec maps from {@code ExecutionResult.toSpecification()} or {@link GraphQLError} objects) to
     * {@code {message, path}} with Java class names stripped from the message. Keeps at most {@link #MAX_ERRORS} errors, each message
     * cut to {@link #MAX_MESSAGE_CHARS}, and adds a final {@code {message: "and N more errors"}} for the rest.
     */
    public static List<Map<String, Object>> sanitize( final List<?> errors )
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
                final Map<String, Object> more = new LinkedHashMap<>();
                more.put( "message", "and " + ( errors.size() - MAX_ERRORS ) + " more errors" );
                result.add( more );
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
            else if ( error != null )
            {
                result.add( entry( error, null ) );
            }
        }
        return result;
    }

    /**
     * Error text for a query that returned no data; validation errors (unknown fields, wrong arguments) get a next step.
     */
    public static String failure( final List<Map<String, Object>> errors )
    {
        final String text = "Query failed: " + describe( errors );
        final boolean validation = errors.stream().anyMatch( error -> VALIDATION.matcher( String.valueOf( error.get( "message" ) ) ).find() );
        if ( !validation )
        {
            return text;
        }
        final String described = text.length() > MAX_FAILURE_CHARS ? text.substring( 0, MAX_FAILURE_CHARS ) + "\u2026" : text;
        return described + ( described.endsWith( "." ) || described.endsWith( "\u2026" ) ? " " : ". " ) + VALIDATION_HINT;
    }

    public static String describe( final List<Map<String, Object>> errors )
    {
        return errors.stream().map( error -> {
            final Object path = error.get( "path" );
            return path instanceof List<?> list && !list.isEmpty()
                ? list.stream().map( String::valueOf ).collect( Collectors.joining( "." ) ) + ": " + error.get( "message" )
                : String.valueOf( error.get( "message" ) );
        } ).collect( Collectors.joining( "; " ) );
    }

    static String cleanMessage( final Object message )
    {
        if ( message == null )
        {
            return "Unknown error";
        }
        final String withoutPrefix = FETCH_PREFIX.matcher( String.valueOf( message ) ).replaceFirst( "" );
        // the index error echoes the whole search request
        final Matcher searchFailed = SEARCH_FAILED.matcher( withoutPrefix );
        if ( searchFailed.lookingAt() )
        {
            return searchFailed.group( 1 ) != null ? SEARCH_TIMED_OUT_MESSAGE : SEARCH_FAILED_MESSAGE;
        }
        if ( INTERNAL.matcher( withoutPrefix ).find() )
        {
            return INTERNAL_ERROR;
        }
        String cleaned = QUALIFIED_EXCEPTION.matcher( withoutPrefix ).replaceAll( "" );
        cleaned = EXCEPTION.matcher( cleaned ).replaceAll( "" );
        cleaned = QUALIFIED_NAME.matcher( cleaned ).replaceAll( GraphQlErrors::qualifiedName ).replaceAll( "\\s{2,}", " " ).trim();
        return cleaned.isEmpty() ? "Unknown error" : cleaned;
    }

    /**
     * Drops Java class and method names ({@code java.util.Map}, {@code com.foo.Bar.run()}, {@code a.b.c$1}) but keeps lowercase dotted
     * names such as host names, file names and application keys ({@code com.example.app:article}).
     */
    private static String qualifiedName( final MatchResult match )
    {
        final boolean javaName = match.group( 1 ) != null || match.group( 2 ) != null || JAVA_NAME_PART.matcher( match.group() ).find();
        return javaName ? "" : Matcher.quoteReplacement( match.group() );
    }

    private static Map<String, Object> entry( final Object message, final Object path )
    {
        final Map<String, Object> entry = new LinkedHashMap<>();
        entry.put( "message", McpJson.shorten( cleanMessage( message ), MAX_MESSAGE_CHARS ) );
        if ( path instanceof List<?> list && !list.isEmpty() )
        {
            entry.put( "path", list.stream()
                .map( segment -> segment instanceof String name ? McpJson.shorten( name, MAX_PATH_SEGMENT_CHARS ) : segment )
                .toList() );
        }
        return entry;
    }

    private static final class HideInternalErrors
        extends SimplePerformantInstrumentation
    {
        @Override
        public CompletableFuture<ExecutionResult> instrumentExecutionResult( final ExecutionResult executionResult,
                                                                             final InstrumentationExecutionParameters parameters,
                                                                             final InstrumentationState state )
        {
            if ( executionResult.getErrors().stream().noneMatch( HideInternalErrors::internal ) )
            {
                return CompletableFuture.completedFuture( executionResult );
            }
            final List<GraphQLError> errors = executionResult.getErrors().stream().map( error -> {
                if ( !internal( error ) )
                {
                    return error;
                }
                LOG.warn( "MCP query field {} failed", error.getPath(), ( (ExceptionWhileDataFetching) error ).getException() );
                return GraphqlErrorBuilder.newError()
                    .message( INTERNAL_ERROR )
                    .path( error.getPath() )
                    .locations( error.getLocations() )
                    .build();
            } ).toList();
            return CompletableFuture.completedFuture( executionResult.transform( builder -> builder.errors( errors ) ) );
        }

        private static boolean internal( final GraphQLError error )
        {
            return error instanceof ExceptionWhileDataFetching fetching && !isCallerError( fetching.getException() );
        }
    }
}
