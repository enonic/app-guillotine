package com.enonic.app.guillotine.graphql.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.repository.RepositoryId;

public class GuillotineLocalContextHelper
{
    public static <T> T executeInContext( final DataFetchingEnvironment environment, Callable<T> callable )
    {
        final Map<String, Object> localContext = environment.getLocalContext();

        final Branch branch = Branch.from( localContext.get( Constants.BRANCH_ARG ).toString() );
        final RepositoryId repositoryId = ProjectName.from( localContext.get( Constants.PROJECT_ARG ).toString() ).getRepoId();

        return ContextBuilder.from( ContextAccessor.current() ).branch( branch ).repositoryId( repositoryId ).build().callWith( callable );
    }

    public static String getSiteKey( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
        return Objects.toString( localContext.get( Constants.SITE_ARG ), null );
    }

    public static Map<String, Object> applyAttachmentsInfo( final DataFetchingEnvironment environment, final String sourceId,
                                                            final Map<String, Object> attachments )
    {
        final Map<String, Object> parentLocalContext = environment.getLocalContext();

        final Map<String, Object> localContext = new HashMap<>( parentLocalContext );

        localContext.put( Constants.CONTENT_ID_FIELD, sourceId );

        if ( attachments != null && !attachments.isEmpty() )
        {
            localContext.put( Constants.ATTACHMENTS_FIELD, attachments );
        }

        return localContext;
    }

    public static String getContextProperty( final DataFetchingEnvironment environment, final String propertyName )
    {
        return getContextProperty( environment, propertyName, String.class );
    }

    @SuppressWarnings("unchecked")
    public static <T> T getContextProperty( final DataFetchingEnvironment environment, final String propertyName, final Class<T> clazz )
    {
        final Map<String, Object> localContext = environment.getLocalContext();
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
            throw new ClassCastException(
                "Cannot cast object of type " + ( value == null ? "null" : value.getClass().getName() ) + " to " + clazz.getName() );
        }
    }

    public static String getSiteBaseUrl( final DataFetchingEnvironment environment )
    {
        return getContextProperty( environment, Constants.SITE_BASE_URL );
    }
}
