package com.enonic.app.guillotine.graphql;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class GraphQLExecutor
    implements ScriptBean
{
    private PortalRequest request;

    @Override
    public void initialize( final BeanContext context )
    {
        this.request = context.getBinding( PortalRequest.class ).get();
    }

    public Object execute( GraphQLSchema graphQLSchema, String query, Map<String, Object> variables, Map<String, Object> queryContext )
    {
        GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query( query ).variables(
            Objects.requireNonNullElse( variables, Collections.emptyMap() ) ).localContext( request ).root( queryContext ).build();

        return new ExecutionResultMapper( graphQL.execute( executionInput ) );
    }
}
