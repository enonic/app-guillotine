package com.enonic.app.guillotine.graphql.fetchers;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GraphQLTypeChecker;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
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
        final PortalRequest portalRequest = PortalRequestAccessor.get();
        final ApplicationKey oldApplicationKey = portalRequest.getApplicationKey();

        portalRequest.setApplicationKey( applicationKey );

        PortalRequestAccessor.set( portalRequest );
        try
        {
            final GraphQLOutputType rootFieldType = resolveRootFieldType( environment );

            if ( GraphQLTypeChecker.isHeadlessCmsType( rootFieldType ) )
            {
                return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
            }
            else
            {
                return doGet( environment );
            }
        }
        finally
        {
            portalRequest.setApplicationKey( oldApplicationKey );
            PortalRequestAccessor.set( portalRequest );
        }
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        return GuillotineSerializer.serialize( resolveFunction.call( new DataFetchingEnvironmentMapper( environment ) ) );
    }

    private GraphQLOutputType resolveRootFieldType( final DataFetchingEnvironment environment )
    {
        ExecutionStepInfo rootStepInfo = environment.getExecutionStepInfo();

        while ( rootStepInfo.getParent() != null && rootStepInfo.getParent().getFieldDefinition() != null )
        {
            rootStepInfo = rootStepInfo.getParent();
        }

        return rootStepInfo.getFieldDefinition().getType();
    }
}
