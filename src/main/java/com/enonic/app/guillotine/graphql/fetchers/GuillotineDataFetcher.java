package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
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
        final HashMap<Object, Object> localContext = new HashMap<>();

        final String siteKey = environment.getArgument( Constants.SITE_ARG );
        final String siteKeyHeader = portalRequestSupplier.get().getHeaders().get( Constants.SITE_HEADER );

        localContext.computeIfAbsent( Constants.SITE_ARG,
                                      v -> Objects.requireNonNullElse( siteKey, Objects.requireNonNullElse( siteKeyHeader, "/" ) ) );

        return DataFetcherResult.newResult().data( new Object() ).localContext( Collections.unmodifiableMap( localContext ) ).build();
    }
}
