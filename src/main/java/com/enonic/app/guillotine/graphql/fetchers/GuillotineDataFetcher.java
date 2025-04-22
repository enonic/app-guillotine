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
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.BaseUrlParams;
import com.enonic.xp.portal.url.UrlTypeConstants;
import com.enonic.xp.project.ProjectName;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;

public class GuillotineDataFetcher
    implements DataFetcher<Object>
{
    private final Supplier<PortalRequest> portalRequestSupplier;

    private final Supplier<ServiceFacade> serviceFacadeSupplier;

    public GuillotineDataFetcher( final Supplier<PortalRequest> portalRequestSupplier, final Supplier<ServiceFacade> serviceFacadeSupplier )
    {
        this.portalRequestSupplier = portalRequestSupplier;
        this.serviceFacadeSupplier = serviceFacadeSupplier;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Map<Object, Object> localContext = new HashMap<>();

        final String projectName = requireNonNullElseGet( environment.getArgument( Constants.PROJECT_ARG ), () -> ProjectName.from(
            requireNonNull( ContextAccessor.current().getRepositoryId(), "Project must be provided" ) ).toString() );

        final String branch = requireNonNullElseGet( environment.getArgument( Constants.BRANCH_ARG ),
                                                     () -> requireNonNull( ContextAccessor.current().getBranch(),
                                                                           "Branch must be provided" ) ).getValue();

        localContext.putIfAbsent( Constants.PROJECT_ARG, projectName );
        localContext.putIfAbsent( Constants.BRANCH_ARG, branch );

        final String siteKey = getSiteKey( environment );
        if ( siteKey != null )
        {
            localContext.putIfAbsent( Constants.SITE_ARG, siteKey );

            final String baseUrl = resolveBaseUrl( projectName, branch, siteKey );
            if ( baseUrl != null )
            {
                localContext.putIfAbsent( Constants.SITE_BASE_URL, baseUrl );
            }
        }

        return DataFetcherResult.newResult().data( new Object() ).localContext( Collections.unmodifiableMap( localContext ) ).build();
    }

    private String getSiteKey( final DataFetchingEnvironment environment )
    {
        final String siteKey = environment.getArgument( Constants.SITE_ARG );
        return siteKey != null ? siteKey : portalRequestSupplier.get().getHeaders().get( Constants.SITE_HEADER );
    }

    private String resolveBaseUrl( final String projectName, final String branch, final String siteKey )
    {
        final BaseUrlParams.Builder paramsBuilder =
            BaseUrlParams.create().setUrlType( UrlTypeConstants.ABSOLUTE ).setProjectName( projectName ).setBranch( branch );

        if ( siteKey.startsWith( "/" ) )
        {
            paramsBuilder.setPath( siteKey );
        }
        else
        {
            paramsBuilder.setId( siteKey );
        }
        return serviceFacadeSupplier.get().getPortalUrlService().baseUrl( paramsBuilder.build() );
    }
}
