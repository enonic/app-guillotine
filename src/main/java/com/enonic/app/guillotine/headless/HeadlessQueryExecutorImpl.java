package com.enonic.app.guillotine.headless;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

@Component(immediate = true, service = HeadlessQueryExecutor.class)
public class HeadlessQueryExecutorImpl
    implements HeadlessQueryExecutor
{
    private final GraphQLSchemaProvider schemaProvider;

    @Activate
    public HeadlessQueryExecutorImpl( final @Reference GraphQLSchemaProvider schemaProvider )
    {
        this.schemaProvider = schemaProvider;
    }

    @Override
    public Object execute( final String schemaId, final String query, final Map<String, Object> variables )
    {
        GraphQLSchema graphQLSchema = schemaProvider.getSchema( schemaId, GraphQLSchema.class );

        GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query( query ).variables(
            Objects.requireNonNullElse( variables, Collections.emptyMap() ) ).build();

        return new ExecutionResultMapper( graphQL.execute( executionInput ) );
    }
}
