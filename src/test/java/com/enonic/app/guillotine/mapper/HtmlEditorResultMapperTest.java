package com.enonic.app.guillotine.mapper;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import com.enonic.app.guillotine.macro.HtmlEditorProcessedResult;
import com.enonic.app.guillotine.macro.MacroDecorator;
import com.enonic.app.guillotine.macro.MacroEditorJsonSerializer;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.macro.Macro;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.macro.MacroKey;
import com.enonic.xp.testing.serializer.JsonMapGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlEditorResultMapperTest
{
    @Test
    void serialize()
    {
        MacroDescriptor macroDescriptor = MacroDescriptor.create().
            displayName( "TestMacro" ).
            key( MacroKey.from( "myapp:mymacro" ) ).
            form( Form.create().
                addFormItem( Input.create().
                    name( "attr1" ).
                    label( "Label" ).
                    inputType( InputTypeName.TEXT_LINE ).occurrences( Occurrences.create( 0, 2 ) ).build() ).
                addFormItem( Input.create().
                    name( "attr2" ).
                    label( "Label" ).
                    inputType( InputTypeName.TEXT_LINE ).build() ).
                build() ).
            build();

        Map<String, Object> macroResult = new MacroEditorJsonSerializer( MacroDecorator.from( Macro.create().
            name( "mymacro" ).
            param( "attr1", "val11" ).
            param( "attr1", "val12" ).
            param( "attr2", "val2" ).
            build()), macroDescriptor ).serialize();

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
