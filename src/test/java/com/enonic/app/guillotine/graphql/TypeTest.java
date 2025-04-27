package com.enonic.app.guillotine.graphql;

import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.graphql.helper.GraphQLTypeUnwrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeTest
{
    @Test
    void testType()
    {
        final GraphQLOutputType type = GraphQLObjectType.newObject()
            .name( "Content" )
            .description( "Content type" )
            .field( GraphQLFieldDefinition.newFieldDefinition()
                .name( "id" )
                .type( Scalars.GraphQLInt )
                .build() )
            .build();

        assertEquals( "Content", GraphQLTypeUnwrapper.unwrapType( type ).getName() );
        assertEquals( "Content", GraphQLTypeUnwrapper.unwrapType( new GraphQLNonNull( type ) ).getName() );
        assertEquals( "Content", GraphQLTypeUnwrapper.unwrapType( new GraphQLNonNull( new GraphQLList( type ) ) ).getName() );
        assertEquals( "Content", GraphQLTypeUnwrapper.unwrapType( new GraphQLList( type ) ).getName() );
    }
}
