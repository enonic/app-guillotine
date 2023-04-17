package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;

public class SubscriptionFactory
{
    private final GuillotineContext guillotineContext;

    public SubscriptionFactory( final GuillotineContext guillotineContext )
    {
        this.guillotineContext = guillotineContext;
    }

    public GraphQLObjectType create()
    {
        GraphQLObjectType subscriptionType =
            newObject( "Subscription", "Subscription", List.of( outputField( "event", createEventType() ) ) );

        guillotineContext.registerType( subscriptionType.getName(), subscriptionType );

        return subscriptionType;
    }

    private GraphQLObjectType createEventType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "type", Scalars.GraphQLString ) );
        fields.add( outputField( "timestamp", Scalars.GraphQLString ) );
        fields.add( outputField( "localOrigin", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "distributed", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "dataAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType eventType = newObject( "Event", "Event", fields );

        guillotineContext.registerType( eventType.getName(), eventType );

        guillotineContext.registerDataFetcher( eventType.getName(), "dataAsJson", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "dataAsJson" );
        } );

        return eventType;
    }
}
