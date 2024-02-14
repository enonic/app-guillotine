package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.PortalRequestHelper;
import com.enonic.app.guillotine.graphql.transformer.ContextualFieldResolver;
import com.enonic.app.guillotine.mapper.DataFetchingEnvironmentMapper;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.script.ScriptValue;

public class DynamicDataFetcher
    implements DataFetcher<Object>
{
    private final ScriptValue resolveFunction;

    private final ApplicationKey applicationKey;

    public DynamicDataFetcher( final ContextualFieldResolver fieldResolver )
    {
        this.resolveFunction = fieldResolver.getResolveFunction();
        this.applicationKey = fieldResolver.getApplicationKey();
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        PortalRequest oldPortalRequest = PortalRequestAccessor.get();
        PortalRequestAccessor.set( PortalRequestHelper.createPortalRequest( oldPortalRequest, applicationKey ) );
        try
        {
            return GuillotineSerializer.serialize( resolveFunction.call( new DataFetchingEnvironmentMapper( environment ) ) );
        }
        finally
        {
            PortalRequestAccessor.set( oldPortalRequest );
        }
    }
}
