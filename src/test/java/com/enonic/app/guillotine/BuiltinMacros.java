package com.enonic.app.guillotine;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.macro.MacroDescriptors;
import com.enonic.xp.macro.MacroKey;

public class BuiltinMacros
{
    public static MacroDescriptors getSystemMacroDescriptors()
    {
        return MacroDescriptors.from( generateDisableMacroDescriptor(), generateEmbedIFrameMacroDescriptor() );
    }

    private static MacroDescriptor generateDisableMacroDescriptor()
    {
        final MacroKey macroKey = MacroKey.from( ApplicationKey.SYSTEM, "disable" );

        final Form form =
            Form.create().addFormItem( createTextAreaInput( "body", "Contents", macroKey ).occurrences( 1, 1 ).build() ).build();

        return create( macroKey, "Disable macros", "Contents of this macro will not be formatted", form );
    }

    private static MacroDescriptor generateEmbedIFrameMacroDescriptor()
    {
        final MacroKey macroKey = MacroKey.from( ApplicationKey.SYSTEM, "embed" );

        final Form form =
            Form.create().addFormItem( createTextAreaInput( "body", "IFrame HTML", macroKey ).occurrences( 1, 1 ).build() ).build();

        return create( macroKey, "Embed IFrame", "Generic iframe embedder", form );
    }

    private static MacroDescriptor create( final MacroKey macroKey, final String displayName, final String description, final Form form )
    {
        return MacroDescriptor.create().key( macroKey ).displayName( displayName ).displayNameI18nKey(
            macroKey.getApplicationKey().getName() + "." + macroKey.getName() + ".displayName" ).description(
            description ).descriptionI18nKey( macroKey.getApplicationKey().getName() + "." + macroKey.getName() + ".description" ).form(
            form ).build();
    }

    private static Input.Builder createTextAreaInput( final String name, final String label, final MacroKey macroKey )
    {
        return Input.create().inputType( InputTypeName.TEXT_AREA ).label( label ).labelI18nKey(
            macroKey.getApplicationKey().getName() + "." + macroKey.getName() + "." + name + ".label" ).name( name ).immutable( true );
    }
}
