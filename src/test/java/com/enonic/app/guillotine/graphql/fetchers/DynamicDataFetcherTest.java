package com.enonic.app.guillotine.graphql.fetchers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.Scalars;
import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;

import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.script.ScriptValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicDataFetcherTest
{
    @AfterEach
    public void cleanUp()
    {
        PortalRequestAccessor.remove();
    }

    @Test
    public void returnsSerializedResolverResult()
        throws Exception
    {
        final ScriptValue returnValue = scriptValueOf( "ok" );
        final ScriptValue resolveFunction = mock( ScriptValue.class );
        when( resolveFunction.call( Mockito.any() ) ).thenReturn( returnValue );

        final DynamicDataFetcher fetcher = new DynamicDataFetcher( resolveFunction );

        assertEquals( "ok", fetcher.get( mockEnvironment() ) );
    }

    @Test
    public void runsWithoutBoundPortalRequest()
        throws Exception
    {
        // Resolvers no longer depend on PortalRequest, so a query executed outside a portal context
        // (no bound PortalRequest) must not fail.
        PortalRequestAccessor.remove();

        final ScriptValue returnValue = scriptValueOf( "ok" );
        final ScriptValue resolveFunction = mock( ScriptValue.class );
        when( resolveFunction.call( Mockito.any() ) ).thenReturn( returnValue );

        final DynamicDataFetcher fetcher = new DynamicDataFetcher( resolveFunction );

        assertEquals( "ok", fetcher.get( mockEnvironment() ) );
    }

    private static ScriptValue scriptValueOf( final Object value )
    {
        final ScriptValue scriptValue = mock( ScriptValue.class );
        when( scriptValue.isValue() ).thenReturn( true );
        when( scriptValue.getValue() ).thenReturn( value );
        return scriptValue;
    }

    private static DataFetchingEnvironment mockEnvironment()
    {
        // Resolve a non-HeadlessCms root field type so get() takes the plain doGet branch.
        final GraphQLFieldDefinition fieldDefinition = mock( GraphQLFieldDefinition.class );
        when( fieldDefinition.getType() ).thenReturn( Scalars.GraphQLString );

        final ExecutionStepInfo stepInfo = mock( ExecutionStepInfo.class );
        when( stepInfo.getParent() ).thenReturn( null );
        when( stepInfo.getFieldDefinition() ).thenReturn( fieldDefinition );

        final DataFetchingEnvironment environment = mock( DataFetchingEnvironment.class );
        when( environment.getExecutionStepInfo() ).thenReturn( stepInfo );
        return environment;
    }
}
