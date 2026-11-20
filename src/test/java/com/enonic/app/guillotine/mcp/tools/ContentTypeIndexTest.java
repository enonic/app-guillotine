package com.enonic.app.guillotine.mcp.tools;

import java.util.List;

import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.xp.schema.content.ContentTypeName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ContentTypeIndexTest
{
    @Test
    public void testTypeWhoseDescriptionAnExtensionReplacedIsFoundByItsGeneratedName()
    {
        final GraphQLInterfaceType content = GraphQLInterfaceType.newInterface()
            .name( "Content" )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "_id" ).type( Scalars.GraphQLID ) )
            .build();
        final GraphQLObjectType article = GraphQLObjectType.newObject()
            .name( "my_app_Article" )
            .description( "Articles, as our extension describes them" )
            .withInterface( content )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "_id" ).type( Scalars.GraphQLID ) )
            .build();
        final GraphQLObjectType untyped = GraphQLObjectType.newObject()
            .name( "UntypedContent" )
            .description( "UntypedContent" )
            .withInterface( content )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "_id" ).type( Scalars.GraphQLID ) )
            .build();
        final GraphQLSchema schema = GraphQLSchema.newSchema()
            .query( GraphQLObjectType.newObject()
                        .name( "Query" )
                        .field( GraphQLFieldDefinition.newFieldDefinition().name( "a" ).type( article ) )
                        .field( GraphQLFieldDefinition.newFieldDefinition().name( "u" ).type( untyped ) ) )
            .codeRegistry( GraphQLCodeRegistry.newCodeRegistry().typeResolver( "Content", env -> article ).build() )
            .build();

        final ContentTypeIndex index =
            new ContentTypeIndex( schema, List.of( ContentTypeName.from( "my.app:article" ), ContentTypeName.from( "other:thing" ) ) );

        assertEquals( "my_app_Article", index.graphQlTypeName( "my.app:article" ) );
        assertEquals( "my.app:article", index.contentTypeOf( "my_app_Article" ) );
        assertEquals( "UntypedContent", index.graphQlTypeName( "other:thing" ) );
        assertNull( index.contentTypeOf( "UntypedContent" ) );
        assertEquals( "UntypedContent", index.graphQlTypeName( "never:registered" ) );
    }
}
