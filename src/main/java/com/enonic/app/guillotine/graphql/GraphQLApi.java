package com.enonic.app.guillotine.graphql;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class GraphQLApi
    implements ScriptBean
{
    private Supplier<ServiceFacade> serviceFacadeSupplier;

    private Supplier<ApplicationService> applicationServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.serviceFacadeSupplier = context.getService( ServiceFacade.class );
        this.applicationServiceSupplier = context.getService( ApplicationService.class );
    }

    public GraphQLSchema createSchema()
    {
        GuillotineContext context = new GuillotineContext( CreateSchemaParams.create().setApplications( getApplicationNames() ).build() );

        new TypeFactory( context, serviceFacadeSupplier.get() ).createTypes();

        GraphQLSchema.Builder graphQLSchema = GraphQLSchema.newSchema();

        graphQLSchema.query( new QueryFactory( context, serviceFacadeSupplier.get() ).create() );
        graphQLSchema.codeRegistry( context.getCodeRegistry() );
        graphQLSchema.additionalTypes( context.getDictionary() );

        return graphQLSchema.build();
    }

    public Object execute( GraphQLSchema graphQLSchema, String query, Map<String, Object> variables, Map<String, Object> queryContext )
    {
        GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query( query ).variables(
            Objects.requireNonNullElse( variables, Collections.emptyMap() ) ).root( queryContext ).build();

        return new ExecutionResultMapper( graphQL.execute( executionInput ) );
    }

    private List<String> getApplicationNames()
    {
        return applicationServiceSupplier.get().getInstalledApplications().stream().map(
            application -> application.getKey().getName() ).collect( Collectors.toList() );
    }

}
