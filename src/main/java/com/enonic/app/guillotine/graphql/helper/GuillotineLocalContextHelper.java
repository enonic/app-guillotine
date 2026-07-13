package com.enonic.app.guillotine.graphql.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.Content;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.repository.RepositoryId;

@SuppressWarnings("unchecked")
public class GuillotineLocalContextHelper
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>()
    {
    };

    public static <T> T executeInContext( final DataFetchingEnvironment environment, Callable<T> callable )
    {
        final Map<String, Object> localContext = getLocalContext( environment );

        final Branch branch = localContext.get( Constants.BRANCH_ARG ) != null
            ? Branch.from( localContext.get( Constants.BRANCH_ARG ).toString() )
            : ContextAccessor.current().getBranch();

        final RepositoryId repositoryId = localContext.get( Constants.PROJECT_ARG ) != null ? ProjectName.from(
            localContext.get( Constants.PROJECT_ARG ).toString() ).getRepoId() : ContextAccessor.current().getRepositoryId();

        return ContextBuilder.from( ContextAccessor.current() ).branch( branch ).repositoryId( repositoryId ).build().callWith( callable );
    }

    public static String getSiteKey( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> localContext = getLocalContext( environment );
        return Objects.toString( localContext.get( Constants.SITE_ARG ), null );
    }

    public static Map<String, Object> getLocalContext( final DataFetchingEnvironment environment )
    {
        if ( environment.getLocalContext() == null )
        {
            return new HashMap<>();
        }
        return environment.getLocalContext();
    }

    public static Map<String, Object> newLocalContext( final DataFetchingEnvironment environment )
    {
        return new HashMap<>( getLocalContext( environment ) );
    }

    public static ProjectName getProjectName( final DataFetchingEnvironment environment )
    {
        final String value = getContextProperty( environment, Constants.PROJECT_ARG );
        return value != null ? ProjectName.from( value ) : ProjectName.from( ContextAccessor.current().getRepositoryId() );
    }

    public static Branch getBranch( final DataFetchingEnvironment environment )
    {
        final String value = getContextProperty( environment, Constants.BRANCH_ARG );
        return value != null ? Branch.from( value ) : ContextAccessor.current().getBranch();
    }

    public static String getSiteBaseUrl( final DataFetchingEnvironment environment )
    {
        return getContextProperty( environment, Constants.SITE_BASE_URL );
    }

    public static String getMediaBaseUrl( final DataFetchingEnvironment environment )
    {
        return getContextProperty( environment, Constants.MEDIA_BASE_URL );
    }

    public static String resolveMediaBaseUrl( final DataFetchingEnvironment environment )
    {
        final String mediaBaseUrl = getMediaBaseUrl( environment );
        return mediaBaseUrl != null ? mediaBaseUrl : getSiteBaseUrl( environment );
    }

    public static String getPageBaseUrl( final DataFetchingEnvironment environment )
    {
        return getContextProperty( environment, Constants.PAGE_BASE_URL );
    }

    public static String stripMediaEndpoint( final DataFetchingEnvironment environment, final String url )
    {
        return getMediaBaseUrl( environment ) != null ? replaceEndpointSegment( url ) : url;
    }

    public static String replaceEndpointSegment( final String url )
    {
        return url == null ? null : url.replace( "/_/", "/" );
    }

    public static String prependBaseUrl( final String baseUrl, final String url )
    {
        if ( baseUrl == null || baseUrl.isBlank() || url == null )
        {
            return url;
        }
        final String normalizedBaseUrl = baseUrl.endsWith( "/" ) ? baseUrl.substring( 0, baseUrl.length() - 1 ) : baseUrl;
        final String normalizedUrl = url.startsWith( "/" ) ? url : "/" + url;
        return normalizedBaseUrl + normalizedUrl;
    }

    public static String getContextProperty( final DataFetchingEnvironment environment, final String propertyName )
    {
        return getContextProperty( environment, propertyName, String.class );
    }

    public static <T> T getContextProperty( final DataFetchingEnvironment environment, final String propertyName, final Class<T> clazz )
    {
        final Map<String, Object> localContext = getLocalContext( environment );
        final Object value = localContext.get( propertyName );
        if ( value == null )
        {
            return null;
        }
        else if ( clazz.isInstance( value ) )
        {
            return (T) value;
        }
        else if ( clazz == String.class )
        {
            return (T) String.valueOf( value );
        }
        else
        {
            throw new ClassCastException( "Cannot cast object of type " + value.getClass().getName() + " to " + clazz.getName() );
        }
    }

    public static Content resolveContent( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> currentContent = GuillotineLocalContextHelper.getCurrentContent( environment );
        return ContentDeserializer.deserialize( currentContent );
    }

    public static String mapToJson( final Map<String, Object> map )
    {
        try
        {
            return MAPPER.writeValueAsString( map );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalStateException( "Failed to serialize Map to JSON string", e );
        }
    }

    public static void putCurrentContent( final Map<String, Object> localContext, final Map<String, Object> content )
    {
        if ( content == null )
        {
            localContext.remove( Constants.CURRENT_CONTENT_FIELD );
            return;
        }
        localContext.put( Constants.CURRENT_CONTENT_FIELD, mapToJson( content ) );
    }

    public static Map<String, Object> getCurrentContent( final DataFetchingEnvironment environment )
    {
        final Object value = getLocalContext( environment ).get( Constants.CURRENT_CONTENT_FIELD );
        if ( value == null )
        {
            return null;
        }
        if ( value instanceof String )
        {
            try
            {
                return MAPPER.readValue( (String) value, MAP_TYPE );
            }
            catch ( JsonProcessingException e )
            {
                throw new IllegalStateException( "Failed to deserialize " + Constants.CURRENT_CONTENT_FIELD + " from JSON", e );
            }
        }
        throw new IllegalStateException(
            "Unexpected type for " + Constants.CURRENT_CONTENT_FIELD + " in localContext: " + value.getClass().getName() );
    }
}
