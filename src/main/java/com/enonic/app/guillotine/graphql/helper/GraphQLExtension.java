package com.enonic.app.guillotine.graphql.helper;

import java.util.List;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

public class GraphQLExtension
{
    public static GraphQLObjectType newObject( GuillotineContext context, String typeName, String description,
                                               List<GraphQLFieldDefinition> fields )
    {
        return newObject( context, typeName, description, null, fields );
    }

    public static GraphQLObjectType newObject( GuillotineContext context, String typeName, String description, List<GraphQLType> interfaces,
                                               List<GraphQLFieldDefinition> fields )
    {

        return GraphQLHelper.newObject( typeName, description, interfaces, fields );
    }
}
