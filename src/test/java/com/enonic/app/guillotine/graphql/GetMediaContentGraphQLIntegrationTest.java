package com.enonic.app.guillotine.graphql;

import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;

import static com.enonic.app.guillotine.graphql.ResourceHelper.readGraphQLQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GetMediaContentGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();
    }

    @Test
    public void testMediaAndAttachmentUrls()
    {
        when( serviceFacade.getPortalUrlGeneratorService().attachmentUrl( any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            "url?a=1&b=2&b=3&c" );
        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( ContentFixtures.createMediaContent() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/getMediaContent.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> attachmentUrlField = CastHelper.cast( getFieldFromGuillotine( result, "attachmentUrl" ) );
        assertEquals( "url?a=1&b=2&b=3&c", attachmentUrlField.get( "mediaUrl" ) );
    }

    @Test
    public void testDownloadAttachmentUrl()
    {
        when( serviceFacade.getPortalUrlGeneratorService().attachmentUrl( any( AttachmentUrlGeneratorParams.class ) ) ).thenReturn(
            "url?download" );
        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( ContentFixtures.createMediaContent() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/getMediaContent.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> downloadAttachmentUrlField = CastHelper.cast( getFieldFromGuillotine( result, "downloadAttachmentUrl" ) );
        assertEquals( "url?download", downloadAttachmentUrlField.get( "mediaUrl" ) );
    }
}
