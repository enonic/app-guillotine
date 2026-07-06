package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.url.BaseUrlParams;
import com.enonic.xp.portal.url.UrlTypeConstants;
import com.enonic.xp.project.ProjectName;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;

public class GuillotineDataFetcher
    implements DataFetcher<Object>
{
    private final Supplier<ServiceFacade> serviceFacadeSupplier;

    private final Supplier<GuillotineConfigService> guillotineConfigServiceSupplier;

    public GuillotineDataFetcher( final Supplier<ServiceFacade> serviceFacadeSupplier,
                                  final Supplier<GuillotineConfigService> guillotineConfigServiceSupplier )
    {
        this.serviceFacadeSupplier = serviceFacadeSupplier;
        this.guillotineConfigServiceSupplier = guillotineConfigServiceSupplier;
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
                                                                           "Branch must be provided" ).getValue() );

        localContext.putIfAbsent( Constants.PROJECT_ARG, projectName );
        localContext.putIfAbsent( Constants.BRANCH_ARG, branch );

        final String mediaBaseUrl = environment.getArgument( Constants.MEDIA_BASE_URL_ARG );
        if ( mediaBaseUrl != null && !mediaBaseUrl.isBlank() )
        {
            requireAllowedBaseUrl( Constants.MEDIA_BASE_URL_ARG, "allowedMediaBaseUrls", mediaBaseUrl,
                                   guillotineConfigServiceSupplier.get().isMediaBaseUrlAllowed( mediaBaseUrl ) );
            localContext.putIfAbsent( Constants.MEDIA_BASE_URL, mediaBaseUrl );
        }

        final String pageBaseUrl = environment.getArgument( Constants.PAGE_BASE_URL_ARG );
        if ( pageBaseUrl != null && !pageBaseUrl.isBlank() )
        {
            requireAllowedBaseUrl( Constants.PAGE_BASE_URL_ARG, "allowedPageBaseUrls", pageBaseUrl,
                                   guillotineConfigServiceSupplier.get().isPageBaseUrlAllowed( pageBaseUrl ) );
            localContext.putIfAbsent( Constants.PAGE_BASE_URL, pageBaseUrl );
        }

        final String siteKey = environment.getArgument( Constants.SITE_ARG );
        if ( siteKey != null && !siteKey.isBlank() )
        {
            requireSiteExists( projectName, branch, siteKey );

            localContext.putIfAbsent( Constants.SITE_ARG, siteKey );

            final String baseUrl = resolveBaseUrl( projectName, branch, siteKey );
            // the bare project prefix is the fallback for a site/project without a configured Base URL:
            // URLs then stay request-based (relativised and vhost-remapped on mounted endpoints)
            if ( baseUrl != null && !baseUrl.equals( "/site/" + projectName + "/" + branch ) )
            {
                localContext.putIfAbsent( Constants.SITE_BASE_URL, baseUrl );
            }
        }

        return DataFetcherResult.newResult().data( new Object() ).localContext( Collections.unmodifiableMap( localContext ) ).build();
    }

    private static void requireAllowedBaseUrl( final String argumentName, final String configName, final String value,
                                                final boolean allowed )
    {
        if ( !allowed )
        {
            throw new IllegalArgumentException(
                String.format( "Value \"%s\" of the \"%s\" argument is not allowed by the \"%s\" configuration", value, argumentName,
                               configName ) );
        }
    }

    private void requireSiteExists( final String projectName, final String branch, final String siteKey )
    {
        if ( "/".equals( siteKey ) )
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
                    return siteKey.startsWith( "/" )
                        ? contentService.contentExists( ContentPath.from( siteKey ) )
                        : contentService.contentExists( ContentId.from( siteKey ) );
                }
                catch ( IllegalArgumentException e )
                {
                    return false;
                }
            } );

        if ( !exists )
        {
            throw new IllegalArgumentException(
                String.format( "Content for the \"%s\" argument not found: \"%s\"", Constants.SITE_ARG, siteKey ) );
        }
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
