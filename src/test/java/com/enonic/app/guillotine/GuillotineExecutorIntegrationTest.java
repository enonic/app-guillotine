package com.enonic.app.guillotine;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.enonic.app.guillotine.graphql.BaseGraphQLIntegrationTest;
import com.enonic.app.guillotine.graphql.ContentFixtures;
import com.enonic.app.guillotine.graphql.ResourceHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class GuillotineExecutorIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void execute_round_trips_to_plain_json_without_http()
    {
        when( contentService.getById( ContentId.from( "contentid" ) ) ).thenReturn( ContentFixtures.createMediaContent() );
        when( contentService.getById( ContentId.from( "referenceid_1" ) ) ).thenReturn( ContentFixtures.createMediaContent() );
        when( contentService.getOutboundDependencies( Mockito.any( ContentId.class ) ) ).thenReturn( ContentIds.from( "referenceid_1" ) );

        final GuillotineExecutor executor =
            new GuillotineExecutor( serviceFacade, applicationService, extensionsExtractorService, guillotineConfigService );

        final Map<String, Object> result = createAdminContext().callWith(
            () -> executor.apply( Map.of( "query", ResourceHelper.readGraphQLQuery( "graphql/getContent.graphql" ) ) ) );

        assertFalse( result.containsKey( "errors" ), () -> "Unexpected errors: " + result.get( "errors" ) );
        assertTrue( result.containsKey( "data" ) );
        assertInstanceOf( Map.class, result.get( "data" ), "result must be plain JDK types (GraphQL spec map)" );
    }
}
