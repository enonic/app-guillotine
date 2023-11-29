package com.enonic.app.guillotine.graphql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.factory.TestFixtures;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SchemaExtensionsGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @BeforeEach
    public void setUp()
    {
        when( contentService.findNearestSiteByPath( ContentPath.from( "/siteKey" ) ) ).thenReturn(
            TestFixtures.createSite( "siteName", "siteDescription" ) );

        when( contentService.getByPath( ContentPath.from( "/contentKey" ) ) ).thenReturn( ContentFixtures.createMediaContent() );

        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.create().add( ContentId.from( "contentId" ) ).build() ).build() );

        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenReturn(
            Contents.from( ContentFixtures.createMediaContent() ) );
    }


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

        // verify local context
        Map<String, Object> testLocalContext = CastHelper.cast( data.get( "testLocalContext" ) );
        Map<String, Object> instanceOfChildType = CastHelper.cast( testLocalContext.get( "child" ) );
        assertEquals( "a=1 and b=2", instanceOfChildType.get( "field" ) );

        assertTrue( result.containsKey( "errors" ) );
        List<Map<String, Object>> errors = CastHelper.cast( result.get( "errors" ) );
        assertEquals( 1, errors.size() );
        assertEquals(
            "Exception while fetching data (/invalidLocalContext) : Unsupported type \"org.openjdk.nashorn.api.scripting.ScriptObjectMirror\". Type of value must be String, Double, Integer or Boolean.",
            errors.get( 0 ).get( "message" ) );

        // verify testGetSiteUsingExtension field
        Map<String, Object> testGetSiteUsingExtension = CastHelper.cast( data.get( "testGetSiteUsingExtension" ) );
        assertNotNull( testGetSiteUsingExtension );
        assertEquals( "portal:site", testGetSiteUsingExtension.get( "type" ) );

        // verify testGetSiteUsingExtension field
        Map<String, Object> testGetContentUsingExtension = CastHelper.cast( data.get( "testGetContentUsingExtension" ) );
        assertNotNull( testGetContentUsingExtension );
        assertEquals( "media:image", testGetContentUsingExtension.get( "type" ) );

        // verify testQueryUsingExtension field
        List<Map<String, Object>> testQueryUsingExtension = CastHelper.cast( data.get( "testQueryUsingExtension" ) );
        assertNotNull( testQueryUsingExtension );
        assertEquals( 1, testQueryUsingExtension.size() );
    }
}
