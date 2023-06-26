package com.enonic.app.guillotine.graphql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaExtensionsGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void testExtensions()
    {
        GraphQLSchema graphQLSchema = getBean().createSchema();

        GraphQLObjectType queryType = graphQLSchema.getQueryType();

        assertNotNull( queryType.getFieldDefinition( "customField" ) );

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/schemaExtension.graphql" ) );

        assertTrue( result.containsKey( "data" ) );
        Map<String, Object> data = CastHelper.cast( result.get( "data" ) );

        // verify customField field
        assertTrue( data.containsKey( "customField" ) );
        assertEquals( ArrayHelper.forceArray( Arrays.asList( "Value 1", "Value 2" ) ), data.get( "customField" ) );

        // verify googleBooks field
        List<Map<String, Object>> googleBooksField = CastHelper.cast( data.get( "googleBooks" ) );
        assertEquals( 2, googleBooksField.size() );

        Map<String, Object> googleBooks_1 = googleBooksField.get( 0 );

        assertEquals( "Title 1", googleBooks_1.get( "title" ) );
        assertEquals( "Description 1", googleBooks_1.get( "description" ) );
        Map<String, Object> author_1 = CastHelper.cast( googleBooks_1.get( "author" ) );
        assertEquals( "Author 1", author_1.get( "name" ) );

        Map<String, Object> googleBooks_2 = googleBooksField.get( 1 );

        assertEquals( "Title 2", googleBooks_2.get( "title" ) );
        assertEquals( "Description 2", googleBooks_2.get( "description" ) );
        Map<String, Object> author_2 = CastHelper.cast( googleBooks_2.get( "author" ) );
        assertEquals( "Author 2", author_2.get( "name" ) );

        // verify testInterface field
        Map<String, Object> testInterfaceField = CastHelper.cast( data.get( "testInterface" ) );
        assertEquals( "Value", testInterfaceField.get( "extraField" ) );
        assertEquals( "No Name", testInterfaceField.get( "name" ) );

        // verify testUnion field
        Map<String, Object> testUnionField = CastHelper.cast( data.get( "testUnion" ) );
        assertEquals( "GoogleBooks", testUnionField.get( "__typename" ) );
        assertEquals( "Title", testUnionField.get( "title" ) );
    }
}
