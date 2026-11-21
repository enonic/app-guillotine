package com.enonic.app.guillotine.graphql.fetchers;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetGraphQLTypeNameDataFetcherTest
{
    private static final GraphQLObjectType ARTICLE =
        newObject( "com_example_myapp_Article", "Article - com.example.myapp:article", List.of( outputField( "id", Scalars.GraphQLID ) ) );

    private static final GraphQLObjectType UNTYPED_CONTENT =
        newObject( "UntypedContent", "UntypedContent", List.of( outputField( "id", Scalars.GraphQLID ) ) );

    @Test
    public void generatedTypeName()
    {
        final GuillotineContext context = GuillotineContext.create().build();
        context.registerContentType( "com.example.myapp:article", ARTICLE );

        assertEquals( "com_example_myapp_Article", fetch( context, "com.example.myapp:article" ) );
    }

    @Test
    public void untypedContentWhenNoTypeWasGenerated()
    {
        final GuillotineContext context = GuillotineContext.create().build();
        context.registerContentType( "com.example.myapp:article", ARTICLE );

        assertEquals( "UntypedContent", fetch( context, "com.example.other:page" ) );
    }

    @Test
    public void untypedContentWhenTypeIsNotInSchema()
    {
        final GuillotineContext context = GuillotineContext.create().build();
        context.registerContentType( "com.example.myapp:landing",
                                     newObject( "com_example_myapp_Landing", "Landing", List.of( outputField( "id", Scalars.GraphQLID ) ) ) );

        assertEquals( "UntypedContent", fetch( context, "com.example.myapp:landing" ) );
    }

    private static String fetch( final GuillotineContext context, final String contentType )
    {
        final GraphQLSchema schema = GraphQLSchema.newSchema()
            .query( newObject( "Query", "Query",
                               List.of( outputField( "article", ARTICLE ), outputField( "untyped", UNTYPED_CONTENT ) ) ) )
            .build();

        final DataFetchingEnvironment environment = mock( DataFetchingEnvironment.class );
        when( environment.getSource() ).thenReturn( Map.of( "name", contentType ) );
        when( environment.getGraphQLSchema() ).thenReturn( schema );

        return new GetGraphQLTypeNameDataFetcher( context ).get( environment );
    }
}
