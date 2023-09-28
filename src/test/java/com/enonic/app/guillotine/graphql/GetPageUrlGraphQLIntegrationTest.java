package com.enonic.app.guillotine.graphql;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.site.Site;

import static com.enonic.app.guillotine.graphql.ResourceHelper.readGraphQLQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GetPageUrlGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void testMediaAndAttachmentUrls()
    {
        when( serviceFacade.getPortalUrlService().pageUrl( any( PageUrlParams.class ) ) ).thenReturn( "pageUrl" );
        when( contentService.getById( ContentId.from( "contentId" ) ) ).thenReturn( ContentFixtures.createMediaContent() );
        when( contentService.getByPath( Mockito.any() ) ).thenReturn( Site.create().
            description( "Site" ).
            name( "test-site" ).
            parentPath( ContentPath.ROOT ).
            language( Locale.ENGLISH ).
            build() );

        GraphQLSchema graphQLSchema = getBean().createSchema();

        Map<String, Object> result = executeQuery( graphQLSchema, readGraphQLQuery( "graphql/getPageUrl.graphql" ) );

        assertFalse( result.containsKey( "errors" ) );
        assertTrue( result.containsKey( "data" ) );

        Map<String, Object> getField = CastHelper.cast( getFieldFromGuillotine( result, "get" ) );
        assertEquals( "pageUrl", getField.get( "pageUrl" ) );
    }
}
