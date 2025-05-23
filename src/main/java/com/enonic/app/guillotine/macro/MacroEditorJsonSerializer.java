package com.enonic.app.guillotine.macro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ListMultimap;

import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.macro.MacroDescriptor;

public class MacroEditorJsonSerializer
{
    private final MacroDecorator macro;

    private final MacroDescriptor descriptor;

    public MacroEditorJsonSerializer( final MacroDecorator macro, final MacroDescriptor descriptor )
    {
        this.macro = macro;
        this.descriptor = descriptor;
    }

    public Map<String, Object> serialize()
    {
        final Map<String, Object> result = new LinkedHashMap<>();

        result.put( "ref", macro.getId() );
        result.put( "name", macro.getMacro().getName() );
        result.put( "descriptor", descriptor.getKey().toString() );
        result.put( "config", Collections.singletonMap( macro.getMacro().getName(), createMacroData() ) );

        return result;
    }

    private Map<String, Object> createMacroData()
    {
        final Map<String, Object> macroData = new LinkedHashMap<>();

        macroData.put( "body", macro.getMacro().getBody() );

        final ListMultimap<String, String> params = macro.getMacro().getParameters();

        for ( String key : params.keySet() )
        {
            List<String> values = macro.getMacro().getParameter( key );

            Occurrences occurrences = FormItemTypesHelper.getOccurrences( descriptor.getForm().getFormItem( key ) );

            if ( occurrences != null && occurrences.isMultiple() )
            {
                macroData.put( key, values );
            }
            else
            {
                macroData.put( key, Objects.requireNonNullElse( values, List.of() ).isEmpty() ? null : values.get( 0 ) );
            }
        }

        return macroData;
    }

}
