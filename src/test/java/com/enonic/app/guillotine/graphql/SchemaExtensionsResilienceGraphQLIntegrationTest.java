package com.enonic.app.guillotine.graphql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.app.Application;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.resource.ResourceKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SchemaExtensionsResilienceGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    private static final ApplicationKey HEALTHY_APP = ApplicationKey.from( "myapplication" );

    private static final ApplicationKey BROKEN_APP = ApplicationKey.from( "brokenapp" );

    @Override
    protected List<Application> createApplications()
    {
        // the broken application comes last, so its partially extracted types would overwrite the healthy ones
        return List.of( createApplication( HEALTHY_APP.toString() ), createApplication( BROKEN_APP.toString() ) );
    }

    @Override
    protected ResourceKey resolveScript( final ResourceKey resourceKey )
    {
        if ( BROKEN_APP.equals( resourceKey.getApplicationKey() ) )
        {
            return ResourceKey.from( HEALTHY_APP, "/brokenapp" + resourceKey.getPath() );
        }
        return resourceKey;
    }

    @Test
    public void brokenApplicationExtensionsAreDiscardedAndSchemaIsStillCreated()
    {
        final GraphQLSchema schema = getBean().createSchema();

        // nothing of the broken application leaked into the schema
        assertNull( schema.getType( "BrokenAppType" ) );
        assertNull( schema.getType( "BrokenAppEnum" ) );

        // the healthy application's definition of the shared type name survived
        final GraphQLObjectType googleBooks = (GraphQLObjectType) schema.getType( "GoogleBooks" );
        assertNotNull( googleBooks );
        assertNotNull( googleBooks.getFieldDefinition( "title" ) );
        assertNull( googleBooks.getFieldDefinition( "clobbered" ) );

        // types, interfaces and unions without description are valid and make it into the schema
        assertNotNull( schema.getType( "NoDescriptionType" ) );
        assertNotNull( schema.getType( "NoDescriptionInterface" ) );
        assertNotNull( schema.getType( "NoDescriptionUnion" ) );

        final Map<String, Object> result = executeQuery( schema, "{ customField googleBooks { title } }" );

        assertFalse( result.containsKey( "errors" ), () -> "Unexpected errors: " + result.get( "errors" ) );
        final Map<String, Object> data = CastHelper.cast( result.get( "data" ) );
        assertEquals( ArrayHelper.forceArray( Arrays.asList( "Value 1", "Value 2" ) ), data.get( "customField" ) );
        final List<Map<String, Object>> googleBooksField = CastHelper.cast( data.get( "googleBooks" ) );
        assertEquals( 2, googleBooksField.size() );
        assertEquals( "Title 1", googleBooksField.get( 0 ).get( "title" ) );
    }
}
