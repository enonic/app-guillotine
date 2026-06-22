package com.enonic.app.guillotine.graphql.fetchers;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GraphQLTypeChecker;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.mapper.DataFetchingEnvironmentMapper;
import com.enonic.xp.script.ScriptValue;

public class DynamicDataFetcher
    implements DataFetcher<Object>
{
    private final ScriptValue resolveFunction;

    public DynamicDataFetcher( final ScriptValue resolveFunction )
    {
        this.resolveFunction = resolveFunction;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
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
