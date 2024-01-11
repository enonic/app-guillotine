package com.enonic.app.guillotine.graphql;

import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.site.Site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GetSiteBySiteHeaderTest
    extends BaseGraphQLIntegrationTest
{
    @Override
    protected PortalRequest modifyPortalRequest( final PortalRequest portalRequest )
    {
        portalRequest.getHeaders().put( Constants.SITE_HEADER, "/siteKey" );

        return portalRequest;
    }

    @Test
    public void testGetSiteField()
    {
        when( contentService.findNearestSiteByPath( any( ContentPath.class ) ) ).thenReturn(
            Site.create().name( "site" ).type( ContentTypeName.site() ).parentPath( ContentPath.ROOT ).data(
                new PropertyTree() ).displayName( "Site" ).id( ContentId.from( "siteId" ) ).build() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).build();

        ExecutionInput executionInput =
            ExecutionInput.newExecutionInput().query( ResourceHelper.readGraphQLQuery( "graphql/getSiteBySiteHeader.graphql" ) ).build();

        ExecutionResultMapper executionResultMapper = new ExecutionResultMapper( graphQL.execute( executionInput ) );

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        executionResultMapper.serialize( generator );

        Map<String, Object> response = CastHelper.cast( generator.getRoot() );

        assertFalse( response.containsKey( "errors" ) );
        assertTrue( response.containsKey( "data" ) );

        Map<String, Object> getGetField = CastHelper.cast( getFieldFromGuillotine( response, "getSite" ) );

        assertNotNull( getGetField );
        assertEquals( "siteId", getGetField.get( "_id" ) );
        assertEquals( "Site", getGetField.get( "displayName" ) );
    }
}
