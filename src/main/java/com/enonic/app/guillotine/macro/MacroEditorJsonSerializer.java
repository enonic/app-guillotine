package com.enonic.app.guillotine.macro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.xp.form.FormItemPath;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.portal.url.ProcessedHtml;

public class MacroEditorJsonSerializer
{
    private final ProcessedHtml.Macro macro;

    private final MacroDescriptor descriptor;

    public MacroEditorJsonSerializer( final ProcessedHtml.Macro macro )
    {
        this.macro = macro;
        this.descriptor = macro.descriptor();
    }

    public Map<String, Object> serialize()
    {
        final Map<String, Object> result = new LinkedHashMap<>();

        result.put( "ref", macro.ref() );
        result.put( "name", descriptor.getName() );
        result.put( "descriptor", descriptor.getKey().toString() );
        result.put( "config", Collections.singletonMap( descriptor.getName(), createMacroData() ) );

        return result;
    }

    private Map<String, Object> createMacroData()
    {
        final Map<String, Object> macroData = new LinkedHashMap<>();

        macroData.put( "body", macro.body() );

        for ( Map.Entry<String, List<String>> param : macro.params().entrySet() )
        {
            final List<String> values = param.getValue();

            final Occurrences occurrences =
                FormItemTypesHelper.getOccurrences( descriptor.getForm().getFormItem( FormItemPath.from( param.getKey() ) ) );

            if ( occurrences != null && occurrences.isMultiple() )
            {
                macroData.put( param.getKey(), values );
            }
            else
            {
                macroData.put( param.getKey(), values.isEmpty() ? null : values.get( 0 ) );
            }
        }

        return macroData;
    }
}
