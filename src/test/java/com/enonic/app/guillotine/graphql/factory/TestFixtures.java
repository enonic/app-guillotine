package com.enonic.app.guillotine.graphql.factory;

import java.util.Locale;

import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentName;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.xdata.XData;
import com.enonic.xp.site.Site;

import static com.enonic.xp.media.MediaInfo.CAMERA_INFO_METADATA_NAME;
import static com.enonic.xp.media.MediaInfo.GPS_INFO_GEO_POINT;
import static com.enonic.xp.media.MediaInfo.GPS_INFO_METADATA_NAME;
import static com.enonic.xp.media.MediaInfo.IMAGE_INFO_IMAGE_HEIGHT;
import static com.enonic.xp.media.MediaInfo.IMAGE_INFO_IMAGE_WIDTH;
import static com.enonic.xp.media.MediaInfo.IMAGE_INFO_METADATA_NAME;
import static com.enonic.xp.media.MediaInfo.IMAGE_INFO_PIXEL_SIZE;
import static com.enonic.xp.media.MediaInfo.MEDIA_INFO_BYTE_SIZE;

public class TestFixtures
{
    public static final XData IMAGE_METADATA =
        XData.create().name( IMAGE_INFO_METADATA_NAME ).displayName( "Image Info" ).displayNameI18nKey(
            "media.imageInfo.displayName" ).form( createImageInfoXDataForm() ).build();

    public static final XData CAMERA_METADATA =
        XData.create().name( CAMERA_INFO_METADATA_NAME ).displayName( "Photo Info" ).displayNameI18nKey(
            "media.cameraInfo.displayName" ).form( createPhotoInfoXDataForm() ).build();

    public static final XData GPS_METADATA =
        XData.create().name( GPS_INFO_METADATA_NAME ).displayName( "Gps Info" ).displayNameI18nKey( "base.gpsInfo.displayName" ).form(
            createGpsInfoMixinForm() ).build();

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

    private static Form createImageInfoXDataForm()
    {
        final Form.Builder form = Form.create();
        form.addFormItem( createLong( IMAGE_INFO_PIXEL_SIZE, "Size (px)" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createLong( IMAGE_INFO_IMAGE_HEIGHT, "Height (px)" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createLong( IMAGE_INFO_IMAGE_WIDTH, "Width (px)" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "contentType", "Content Type" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "description", "Description" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createLong( MEDIA_INFO_BYTE_SIZE, "Size (bytes)" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "colorSpace", "Color Space" ).occurrences( 0, 0 ).build() );
        form.addFormItem( createTextLine( "fileSource", "File Source" ).occurrences( 0, 1 ).build() );

        return form.build();
    }

    private static Input.Builder createLong( final String name, final String label )
    {
        return Input.create().inputType( InputTypeName.LONG ).label( label ).name( name ).immutable( true );
    }

    private static Form createPhotoInfoXDataForm()
    {
        final Form.Builder form = Form.create();
        form.addFormItem( createDate( "date", "Date" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "make", "Make" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "model", "Model" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "lens", "Lens" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "iso", "ISO" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "focalLength", "Focal Length" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "focalLength35", "Focal Length 35mm" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "exposureBias", "Exposure Bias" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "aperture", "Aperture" ).occurrences( 0, 0 ).build() );
        form.addFormItem( createTextLine( "shutterTime", "Shutter Time" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "flash", "Flash" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "autoFlashCompensation", "Auto Flash Compensation" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "whiteBalance", "White Balance" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "exposureProgram", "Exposure Program" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "shootingMode", "Shooting Mode" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "meteringMode", "Metering Mode" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "exposureMode", "Exposure Mode" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "focusDistance", "Focus Distance" ).occurrences( 0, 1 ).build() );
        form.addFormItem( createTextLine( "orientation", "Orientation" ).occurrences( 0, 1 ).build() );

        return form.build();
    }

    public static Site createSite( final String name, final String description )
    {
        return Site.create().id( ContentId.from( name ) ).name( ContentName.from( name ) ).description( description ).parentPath(
            ContentPath.ROOT ).language( Locale.ENGLISH ).build();
    }

    private static Input.Builder createDate( final String name, final String label )
    {
        return Input.create().inputType( InputTypeName.DATE_TIME ).label( label ).name( name ).immutable( true );
    }
}
