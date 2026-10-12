package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.project.ProjectName;

import static java.util.Objects.requireNonNull;

public class GuillotineDataFetcher
    implements DataFetcher<Object>
{
    private final Supplier<ServiceFacade> serviceFacadeSupplier;

    public GuillotineDataFetcher( final Supplier<ServiceFacade> serviceFacadeSupplier )
    {
        this.serviceFacadeSupplier = serviceFacadeSupplier;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Map<Object, Object> localContext = new HashMap<>();

        final PortalRequest portalRequest = PortalRequestAccessor.get();
        final boolean useContextOnly = portalRequest != null && portalRequest.getMode() != null;

        final String projectName = useContextOnly || environment.getArgument( Constants.PROJECT_ARG ) == null
            ? ProjectName.from( requireNonNull( ContextAccessor.current().getRepositoryId(), "Project must be provided" ) ).toString()
            : environment.getArgument( Constants.PROJECT_ARG );

        final String branch = useContextOnly || environment.getArgument( Constants.BRANCH_ARG ) == null ? requireNonNull(
            ContextAccessor.current().getBranch(), "Branch must be provided" ).getValue() : environment.getArgument( Constants.BRANCH_ARG );

        localContext.putIfAbsent( Constants.PROJECT_ARG, projectName );
        localContext.putIfAbsent( Constants.BRANCH_ARG, branch );

        final String scopeKey = environment.getArgument( Constants.SCOPE_ARG );
        if ( scopeKey != null && !scopeKey.isBlank() )
        {
            requireScopeExists( projectName, branch, scopeKey );
            localContext.putIfAbsent( Constants.SCOPE_ARG, scopeKey );
        }

        return DataFetcherResult.newResult().data( new Object() ).localContext( Collections.unmodifiableMap( localContext ) ).build();
    }

    private void requireScopeExists( final String projectName, final String branch, final String scopeKey )
    {
        if ( "/".equals( scopeKey ) )
        {
            // project root: always resolvable
            return;
        }

        final boolean exists = ContextBuilder.copyOf( ContextAccessor.current() )
            .repositoryId( ProjectName.from( projectName ).getRepoId() )
            .branch( branch )
            .build()
            .callWith( () -> {
                final ContentService contentService = serviceFacadeSupplier.get().getContentService();
                try
                {
                    return scopeKey.startsWith( "/" )
                        ? contentService.contentExists( ContentPath.from( scopeKey ) )
                        : contentService.contentExists( ContentId.from( scopeKey ) );
                }
                catch ( IllegalArgumentException e )
                {
                    return false;
                }
            } );

        if ( !exists )
        {
            throw new IllegalArgumentException(
                String.format( "Content for the \"%s\" argument not found: \"%s\"", Constants.SCOPE_ARG, scopeKey ) );
        }
    }
}
