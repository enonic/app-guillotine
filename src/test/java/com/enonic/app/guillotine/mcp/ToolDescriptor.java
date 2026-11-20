package com.enonic.app.guillotine.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

/**
 * A group's descriptor {@code mcp/<group>/<group>.yaml} as XP reads it, and validation of tool results against its output schemas.
 */
public final class ToolDescriptor
{
    private static final SchemaRegistry SCHEMAS = SchemaRegistry.withDefaultDialect( SpecificationVersion.DRAFT_2020_12 );

    private static final ObjectMapper JSON = new ObjectMapper();

    private ToolDescriptor()
    {
    }

    public static boolean exists( final McpGroup group )
    {
        return ToolDescriptor.class.getResource( resource( group ) ) != null;
    }

    public static Map<String, Object> descriptor( final McpGroup group )
    {
        try (InputStream in = ToolDescriptor.class.getResourceAsStream( resource( group ) ))
        {
            if ( in == null )
            {
                throw new IllegalArgumentException( "No descriptor for group " + group.groupName() );
            }
            return new YAMLMapper().readValue( in, new TypeReference<>()
            {
            } );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    public static List<Map<String, Object>> tools( final McpGroup group )
    {
        return JSON.convertValue( descriptor( group ).get( "tools" ), new TypeReference<>()
        {
        } );
    }

    public static Map<String, Object> tool( final McpGroup group, final String name )
    {
        return tools( group ).stream().filter( tool -> name.equals( tool.get( "name" ) ) ).findFirst().orElseThrow();
    }

    /**
     * What is wrong with {@code schema} as a JSON Schema (draft 2020-12, the dialect XP's MCP SDK validates with); empty when valid.
     */
    public static List<String> schemaErrors( final Object schema )
    {
        final Schema metaSchema = SCHEMAS.getSchema( SchemaLocation.of( "https://json-schema.org/draft/2020-12/schema" ) );
        return metaSchema.validate( JSON.valueToTree( schema ) ).stream().map( Error::toString ).toList();
    }

    /**
     * What is wrong with {@code value} as a structured result of {@code toolName}; empty when it matches the declared output schema.
     */
    public static List<String> outputSchemaErrors( final McpGroup group, final String toolName, final Object value )
    {
        final Object schema = tool( group, toolName ).get( "outputSchema" );
        if ( schema == null )
        {
            throw new IllegalArgumentException( toolName + " declares no outputSchema" );
        }
        return SCHEMAS.getSchema( JSON.valueToTree( schema ) ).validate( JSON.valueToTree( value ) ).stream().map( Error::toString ).toList();
    }

    private static String resource( final McpGroup group )
    {
        return "/mcp/" + group.groupName() + "/" + group.groupName() + ".yaml";
    }
}
