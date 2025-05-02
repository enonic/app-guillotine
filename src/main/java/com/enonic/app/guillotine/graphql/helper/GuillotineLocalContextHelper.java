package com.enonic.app.guillotine.graphql.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.Content;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.repository.RepositoryId;

public class GuillotineLocalContextHelper
{
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

    public static Map<String, Object> applyAttachmentsInfo( final DataFetchingEnvironment environment, final String sourceId,
                                                            final Map<String, Object> attachments )
    {
        final Map<String, Object> localContext = newLocalContext( environment );

        localContext.put( Constants.CONTENT_ID_FIELD, sourceId );

        if ( attachments != null && !attachments.isEmpty() )
        {
            localContext.put( Constants.ATTACHMENTS_FIELD, attachments );
        }

        return localContext;
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

    public static String getContextProperty( final DataFetchingEnvironment environment, final String propertyName )
    {
        return getContextProperty( environment, propertyName, String.class );
    }

    @SuppressWarnings("unchecked")
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

    public static String getSiteBaseUrl( final DataFetchingEnvironment environment )
    {
        return getContextProperty( environment, Constants.SITE_BASE_URL );
    }

    @SuppressWarnings("unchecked")
    public static Content resolveContent( final DataFetchingEnvironment environment, final String contentId )
    {
        final Map<String, Object> contents = getContextProperty( environment, Constants.CONTENTS_FIELD, Map.class );

        if ( contents == null )
        {
            return null;
        }

        return ContentDeserializer.deserialize( contents.get( contentId ) );
    }

    public static Content resolveContent( final DataFetchingEnvironment environment )
    {
        final String contentId = getContextProperty( environment, Constants.CONTENT_ID_FIELD );
        return resolveContent( environment, contentId );
    }
}
