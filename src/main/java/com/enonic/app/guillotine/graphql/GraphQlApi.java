package com.enonic.app.guillotine.graphql;

import java.util.function.Supplier;

import graphql.schema.GraphQLSchema;

import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class GraphQlApi
    implements ScriptBean
{
    private Supplier<ServiceFacade> serviceFacadeSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.serviceFacadeSupplier = context.getService( ServiceFacade.class );
    }

    public GraphQLSchema createSchema( final CreateSchemaParams params )
    {
        GuillotineContext context = new GuillotineContext( params );

        new TypeFactory( context, serviceFacadeSupplier.get() ).createTypes();

        GraphQLSchema.Builder graphQLSchema = GraphQLSchema.newSchema();

        graphQLSchema.query( new QueryFactory( context, serviceFacadeSupplier.get() ).create() );
        graphQLSchema.subscription( new SubscriptionFactory( context ).create() );
        graphQLSchema.codeRegistry( context.getCodeRegistry() );
        graphQLSchema.additionalTypes( context.getDictionary() );

        return graphQLSchema.build();
    }

}
