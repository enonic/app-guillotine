package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.portal.PortalRequest;

public class GuillotineDataFetcher
    implements DataFetcher<Object>
{
    private final Supplier<PortalRequest> portalRequestSupplier;

    public GuillotineDataFetcher( final Supplier<PortalRequest> portalRequestSupplier )
    {
        this.portalRequestSupplier = portalRequestSupplier;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Map<Object, Object> localContext = new HashMap<>();

        localContext.computeIfAbsent( Constants.PROJECT_ARG,
                                      v -> Objects.requireNonNull( environment.getArgument( Constants.PROJECT_ARG ) ) );
        localContext.computeIfAbsent( Constants.BRANCH_ARG,
                                      v -> Objects.requireNonNullElse( environment.getArgument( Constants.BRANCH_ARG ),
                                                                       ContentConstants.BRANCH_DRAFT.getValue() ) );
        localContext.computeIfAbsent( Constants.SITE_ARG,
                                      v -> environment.getArgument( Constants.SITE_ARG ) != null ? environment.getArgument(
                                          Constants.SITE_ARG ) : portalRequestSupplier.get().getHeaders().get( Constants.SITE_HEADER ) );

        return DataFetcherResult.newResult().data( new Object() ).localContext( Collections.unmodifiableMap( localContext ) ).build();
    }
}
