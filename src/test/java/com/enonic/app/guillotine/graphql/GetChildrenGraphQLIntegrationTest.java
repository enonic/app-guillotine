package com.enonic.app.guillotine.graphql;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentIdsByParentResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.content.Media;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GetChildrenGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{

    @BeforeEach
    public void setUp()
    {
        final Media mediaContent = ContentFixtures.createMediaContent();
        when( contentService.findIdsByParent( any( FindContentByParentParams.class ) ) ).thenReturn(
            FindContentIdsByParentResult.create().totalHits( 1 ).contentIds( ContentIds.from( mediaContent.getId() ) ).build() );

        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenReturn( Contents.from( mediaContent ) );
    }

    @Test
    public void testGetChildrenField()
    {
        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getChildren.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        assertNotNull( CastHelper.cast( getFieldFromGuillotine( result, "getChildren" ) ) );
    }

    @Test
    public void testGetChildrenFieldNoResult()
    {
        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getChildren.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        assertNotNull( CastHelper.cast( getFieldFromGuillotine( result, "getChildren" ) ) );
    }

    @Test
    public void testGetChildrenConnectionField()
    {
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
        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result =
            executeQuery( graphQLSchema, ResourceHelper.readGraphQLQuery( "graphql/getChildrenConnection.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        assertNotNull( CastHelper.cast( getFieldFromGuillotine( result, "getChildrenConnection" ) ) );
    }
}
