package com.enonic.app.guillotine.mapper;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import com.enonic.app.guillotine.macro.HtmlEditorProcessedResult;
import com.enonic.app.guillotine.macro.MacroEditorJsonSerializer;
import com.enonic.xp.macro.MacroKey;
import com.enonic.xp.portal.url.ProcessedHtml;
import com.enonic.xp.testing.serializer.JsonMapGenerator;
import com.enonic.xp.util.GenericValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlEditorResultMapperTest
{
    @Test
    void serialize()
    {
        Map<String, Object> macroResult = new MacroEditorJsonSerializer(
            new ProcessedHtml.Macro( "307f02a2-7019-4012-807e-916df5779ae6", MacroKey.from( "myapp:mymacro" ), GenericValue.newObject()
                .put( "attr1", GenericValue.newList().add( GenericValue.stringValue( "val11" ) ).add( GenericValue.stringValue( "val12" ) ).build() )
                .put( "attr2", "val2" )
                .build(), "" ) ).serialize();

        HtmlEditorProcessedResult input = HtmlEditorProcessedResult.create().
            setProcessedHtml(
                "<p><editor-macro data-macro-name=\"mymacro\" data-macro-ref=\"307f02a2-7019-4012-807e-916df5779ae6\"></editor-macro></p>" ).
            setMacrosAsJson( Collections.singletonList( macroResult ) ).
            build();

        HtmlEditorResultMapper instance = new HtmlEditorResultMapper( input );

        JsonMapGenerator generator = new JsonMapGenerator();

        instance.serialize( generator );

        final JsonNode actualJson = (JsonNode) generator.getRoot();

        assertNotNull( actualJson );
        assertEquals(
            "<p><editor-macro data-macro-name=\"mymacro\" data-macro-ref=\"307f02a2-7019-4012-807e-916df5779ae6\"></editor-macro></p>",
            actualJson.path( "processedHtml" ).asText() );
        assertTrue( actualJson.path( "macrosAsJson" ).isArray() );

        JsonNode macrosAsJson = actualJson.path( "macrosAsJson" ).get( 0 );

        assertEquals( "mymacro", macrosAsJson.path( "name" ).asText() );

        JsonNode config = macrosAsJson.get( "config" );

        JsonNode macroConfig = config.get( "mymacro" );

        assertTrue( macroConfig.path( "attr1" ).isArray() );
        assertEquals( "val11", macroConfig.path( "attr1" ).get( 0 ).asText() );
        assertEquals( "val12", macroConfig.path( "attr1" ).get( 1 ).asText() );

        assertFalse( macroConfig.path( "attr2" ).isArray() );
        assertEquals( "val2", macroConfig.path( "attr2" ).asText() );

        assertTrue( macroConfig.path( "body" ).asText().isEmpty() );
        assertFalse( macrosAsJson.path( "ref" ).asText().isEmpty() );
        assertEquals( "mymacro", macrosAsJson.path( "name" ).asText() );
    }
}
