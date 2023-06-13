package com.enonic.app.guillotine.graphql.factory;

import com.enonic.xp.form.Form;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.xdata.XData;

import static com.enonic.xp.media.MediaInfo.GPS_INFO_GEO_POINT;
import static com.enonic.xp.media.MediaInfo.GPS_INFO_METADATA_NAME;

public class TestFixtures
{
    public static XData createGpsInfo()
    {
        return XData.create().name( GPS_INFO_METADATA_NAME ).displayName( "Gps Info" ).form( createGpsInfoMixinForm() ).build();
    }

    private static Form createGpsInfoMixinForm()
    {
        final Form.Builder form = Form.create();
        form.addFormItem(
            Input.create().inputType( InputTypeName.GEO_POINT ).label( "Geo Point" ).name( GPS_INFO_GEO_POINT ).immutable( true ).build() );
        form.addFormItem( createTextLine( "altitude", "Altitude" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "direction", "Direction" ).occurrences( 0, 1 ).build() );

        return form.build();
    }

    private static Input.Builder createTextLine( final String name, final String label )
    {
        return Input.create().inputType( InputTypeName.TEXT_LINE ).label( label ).name( name ).immutable( true );
    }
}
