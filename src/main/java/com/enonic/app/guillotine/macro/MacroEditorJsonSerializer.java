package com.enonic.app.guillotine.macro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.enonic.xp.portal.url.ProcessedHtml;
import com.enonic.xp.util.GenericValue;

public class MacroEditorJsonSerializer
{
    private final ProcessedHtml.Macro macro;

    public MacroEditorJsonSerializer( final ProcessedHtml.Macro macro )
    {
        this.macro = macro;
    }

    public Map<String, Object> serialize()
    {
        final Map<String, Object> result = new LinkedHashMap<>();

        result.put( "ref", macro.ref() );
        result.put( "name", macro.descriptor().getName() );
        result.put( "descriptor", macro.descriptor().toString() );
        result.put( "config", Collections.singletonMap( macro.descriptor().getName(), createMacroData() ) );

        return result;
    }

    private Map<String, Object> createMacroData()
    {
        final Map<String, Object> macroData = new LinkedHashMap<>();

        macroData.put( "body", macro.body() );

        for ( Map.Entry<String, GenericValue> param : macro.config().properties() )
        {
            macroData.put( param.getKey(), param.getValue().toRawJava() );
        }

        return macroData;
    }
}
