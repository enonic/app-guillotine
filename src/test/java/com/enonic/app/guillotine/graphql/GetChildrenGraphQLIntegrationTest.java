package com.enonic.app.guillotine.graphql;

import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentByParentResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GetChildrenGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void testGetChildrenField()
    {
        when( contentService.getByPath( ContentPath.from( "/hmdb" ) ) ).thenReturn( ContentFixtures.createMediaContent() );

        when( contentService.findByParent( any( FindContentByParentParams.class ) ) ).thenReturn(
            FindContentByParentResult.create().totalHits( 1 ).contents(
                Contents.from( ContentFixtures.createMediaContent() ) ).build() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getChildren.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        assertNotNull( CastHelper.cast( getFieldFromGuillotine( result, "getChildren" ) ) );
    }

    @Test
    public void testGetChildrenFieldNoResult()
    {
        when( contentService.getByPath( ContentPath.from( "/hmdb" ) ) ).thenReturn( null );

        when( contentService.findByParent( any( FindContentByParentParams.class ) ) ).thenReturn(
            FindContentByParentResult.create().totalHits( 0 ).contents( Contents.empty() ).build() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getChildren.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        assertNotNull( CastHelper.cast( getFieldFromGuillotine( result, "getChildren" ) ) );
    }

    @Test
    public void testGetChildrenConnectionField()
    {
        when( contentService.getByPath( ContentPath.from( "/hmdb" ) ) ).thenReturn( ContentFixtures.createMediaContent() );

        when( contentService.findByParent( any( FindContentByParentParams.class ) ) ).thenReturn(
            FindContentByParentResult.create().totalHits( 1 ).contents(
                Contents.from( ContentFixtures.createMediaContent() ) ).build() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result =
            executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getChildrenConnection.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        assertNotNull( CastHelper.cast( getFieldFromGuillotine( result, "getChildrenConnection" ) ) );
    }

    @Test
    public void testGetChildrenConnectionFieldParentNotExists()
    {
        when( contentService.getByPath( ContentPath.from( "/hmdb" ) ) ).thenReturn( null );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result =
            executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getChildrenConnection.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        assertNotNull( CastHelper.cast( getFieldFromGuillotine( result, "getChildrenConnection" ) ) );
    }
}
