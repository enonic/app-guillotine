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
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

/**
 * The tool descriptor {@code mcp/content/content.yaml} as XP reads it, and validation of tool results against its output schemas.
 */
public final class ToolDescriptor
{
    private static final String RESOURCE = "/mcp/content/content.yaml";

    private static final SchemaRegistry SCHEMAS = SchemaRegistry.withDefaultDialect( SpecificationVersion.DRAFT_2020_12 );

    private static final ObjectMapper JSON = new ObjectMapper();

    private ToolDescriptor()
    {
    }

    public static List<Map<String, Object>> tools()
    {
        try (InputStream in = ToolDescriptor.class.getResourceAsStream( RESOURCE ))
        {
            final Map<String, Object> descriptor = new YAMLMapper().readValue( in, new TypeReference<>()
            {
            } );
            return JSON.convertValue( descriptor.get( "tools" ), new TypeReference<>()
            {
            } );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    public static Map<String, Object> tool( final String name )
    {
        return tools().stream().filter( tool -> name.equals( tool.get( "name" ) ) ).findFirst().orElseThrow();
    }

    /**
     * What is wrong with {@code value} as a structured result of {@code toolName}; empty when it matches the declared output schema.
     */
    public static List<String> outputSchemaErrors( final String toolName, final Object value )
    {
        final Object schema = tool( toolName ).get( "outputSchema" );
        if ( schema == null )
        {
            throw new IllegalArgumentException( toolName + " declares no outputSchema" );
        }
        return SCHEMAS.getSchema( JSON.valueToTree( schema ) )
            .validate( JSON.valueToTree( value ) )
            .stream()
            .map( Error::toString )
            .toList();
    }
}
