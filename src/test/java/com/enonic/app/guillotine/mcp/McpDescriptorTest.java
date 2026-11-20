package com.enonic.app.guillotine.mcp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The descriptors under {@code mcp/<group>/} as XP loads them: one per group that has tools, declaring exactly the registered tools.
 */
public class McpDescriptorTest
{
    private static final Map<String, Object> ANNOTATIONS =
        Map.of( "readOnlyHint", true, "destructiveHint", false, "idempotentHint", true, "openWorldHint", false );

    private static final Set<String> ALWAYS_LOADED = Set.of( "list-projects", "search" );

    private final Map<McpGroup, List<String>> registered =
        new McpTools( mock( ServiceFacade.class ), mock( GuillotineConfigService.class ) ).names();

    @Test
    public void testEachGroupWithToolsHasADescriptorAndControllerDeclaringExactlyThem()
    {
        for ( final McpGroup group : McpGroup.values() )
        {
            final List<String> tools = registered.getOrDefault( group, List.of() );
            final String controller = "/mcp/" + group.groupName() + "/" + group.groupName() + ".js";
            if ( tools.isEmpty() )
            {
                assertFalse( ToolDescriptor.exists( group ), group.groupName() );
                assertNull( getClass().getResource( controller ), controller );
                continue;
            }
            assertNotNull( getClass().getResource( controller ), controller );
            assertEquals( tools, ToolDescriptor.tools( group ).stream().map( tool -> tool.get( "name" ) ).toList(), group.groupName() );
        }
        assertEquals( List.of( "list-projects" ), registered.get( McpGroup.CONTENT ) );
    }

    @Test
    public void testEveryDescriptorAllowsSignedInUsersExplicitly()
    {
        for ( final McpGroup group : describedGroups() )
        {
            final Map<String, Object> descriptor = ToolDescriptor.descriptor( group );
            assertEquals( "MCP", descriptor.get( "kind" ) );
            assertEquals( List.of( "role:system.authenticated" ), descriptor.get( "allow" ), group.groupName() );
            assertEquals( Set.of( "kind", "allow", "tools" ), descriptor.keySet() );
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEveryToolIsAReadOnlyToolWithValidClosedSchemas()
    {
        for ( final McpGroup group : describedGroups() )
        {
            for ( final Map<String, Object> tool : ToolDescriptor.tools( group ) )
            {
                final String name = (String) tool.get( "name" );
                assertTrue( name.matches( "[a-z]+(-[a-z]+)*" ), name );
                assertTrue( ( "com.enonic.app.guillotine__" + group.groupName() + "__" + name ).length() <= 128, name );
                assertTrue( tool.get( "title" ) instanceof String title && !title.isBlank(), name );
                assertTrue( tool.get( "description" ) instanceof String description && !description.isBlank(), name );
                assertEquals( ANNOTATIONS, tool.get( "annotations" ), name );

                final Map<String, Object> input = (Map<String, Object>) tool.get( "inputSchema" );
                assertEquals( "object", input.get( "type" ), name );
                assertEquals( false, input.get( "additionalProperties" ), name );
                assertEquals( List.of(), ToolDescriptor.schemaErrors( input ), name );
                if ( tool.get( "outputSchema" ) != null )
                {
                    assertEquals( List.of(), ToolDescriptor.schemaErrors( tool.get( "outputSchema" ) ), name );
                }
            }
        }
    }

    @Test
    public void testSchemaCheckCatchesAnInvalidSchema()
    {
        assertFalse( ToolDescriptor.schemaErrors( Map.of( "type", "objekt" ) ).isEmpty() );
        assertFalse( ToolDescriptor.schemaErrors( Map.of( "type", "object", "required", "name" ) ).isEmpty() );
    }

    @Test
    public void testOnlyEntryToolsAreAlwaysLoaded()
    {
        for ( final McpGroup group : describedGroups() )
        {
            for ( final Map<String, Object> tool : ToolDescriptor.tools( group ) )
            {
                final Object meta = tool.get( "_meta" );
                if ( ALWAYS_LOADED.contains( tool.get( "name" ) ) )
                {
                    assertEquals( Map.of( "anthropic/alwaysLoad", true ), meta, (String) tool.get( "name" ) );
                }
                else
                {
                    assertNull( meta, (String) tool.get( "name" ) );
                }
            }
        }
    }

    private List<McpGroup> describedGroups()
    {
        final List<McpGroup> groups = List.copyOf( registered.keySet() );
        assertFalse( groups.isEmpty() );
        return groups;
    }
}
