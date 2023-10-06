package com.enonic.app.guillotine.graphql;

import java.time.Instant;
import java.util.Locale;

import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.attachment.Attachments;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentPublishInfo;
import com.enonic.xp.content.ExtraData;
import com.enonic.xp.content.Media;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.page.DescriptorKey;
import com.enonic.xp.page.Page;
import com.enonic.xp.page.PageRegions;
import com.enonic.xp.region.FragmentComponent;
import com.enonic.xp.region.ImageComponent;
import com.enonic.xp.region.LayoutComponent;
import com.enonic.xp.region.LayoutRegions;
import com.enonic.xp.region.PartComponent;
import com.enonic.xp.region.Region;
import com.enonic.xp.region.TextComponent;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.xdata.XDataName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;
import com.enonic.xp.security.acl.Permission;

public class ContentFixtures
{
    public static Media createMediaContent()
    {
        final Media.Builder builder = Media.create();

        builder.id( ContentId.from( "contentId" ) );
        builder.name( "mycontent" );
        builder.displayName( "My Content" );
        builder.valid( true );
        builder.type( ContentTypeName.imageMedia() );
        builder.parentPath( ContentPath.from( "/a/b" ) );
        builder.modifier( PrincipalKey.from( "user:system:admin" ) );
        builder.modifiedTime( Instant.ofEpochSecond( 0 ) );
        builder.creator( PrincipalKey.from( "user:system:admin" ) );
        builder.owner( PrincipalKey.from( "user:system:admin" ) );
        builder.createdTime( Instant.ofEpochSecond( 0 ) );
        builder.language( Locale.ENGLISH );
        builder.data( dataMediaImage() );
        builder.publishInfo( ContentPublishInfo.create().from( Instant.parse( "2016-11-03T10:00:00Z" ) ).to(
            Instant.parse( "2016-11-23T10:00:00Z" ) ).build() );
        builder.addExtraData( new ExtraData( XDataName.from( "media" ), xMedia() ) );
        builder.page( newPage() );
        builder.attachments( mediaAttachments() );
        builder.permissions( AccessControlList.create().add(
            AccessControlEntry.create().allowAll().principal( RoleKeys.CONTENT_MANAGER_ADMIN ).build() ).add(
            AccessControlEntry.create().principal( PrincipalKey.ofAnonymous() ).allow( Permission.CREATE ).allow( Permission.DELETE ).allow(
                Permission.READ_PERMISSIONS ).deny( Permission.MODIFY ).deny( Permission.WRITE_PERMISSIONS ).build() ).build() );

        return builder.build();
    }

    public static PropertyTree newTinyPropertyTree()
    {
        final PropertyTree tree = new PropertyTree();
        tree.setString( "a", "1" );
        return tree;
    }

    public static Attachments mediaAttachments()
    {
        return Attachments.from(
            Attachment.create().name( "image.jpeg" ).label( "source" ).mimeType( "image/jpeg" ).size( 12345 ).sha512( "sha512" ).build() );
    }

    public static PropertyTree xMedia()
    {
        final PropertySet imageInfo = new PropertySet();
        imageInfo.setString( "colorSpace", "sRGB" );
        imageInfo.setString( "contentType", "image/jpeg" );
        imageInfo.setLong( "pixelSize", 16036032L );
        imageInfo.setLong( "imageHeight", 3468L );
        imageInfo.setLong( "imageWidth", 4624L );
        imageInfo.setLong( "byteSize", 3620112L );

        final PropertySet cameraInfo = new PropertySet();
        cameraInfo.setString( "exposureMode", "Auto exposure" );
        cameraInfo.setString( "make", "samsung" );
        cameraInfo.setString( "exposureBias", "0 EV" );
        cameraInfo.setString( "meteringMode", "Spot" );
        cameraInfo.setString( "whiteBalance", "Auto white balance" );
        cameraInfo.setString( "orientation", "Top, left side (Horizontal / normal)" );
        cameraInfo.setString( "flash", "Flash did not fire" );
        cameraInfo.setInstant( "date", Instant.ofEpochSecond( 0 ) );
        cameraInfo.setString( "shutterTime", "0.01 sec" );
        cameraInfo.setString( "focalLength", "5.2 mm" );
        cameraInfo.setString( "model", "SM-A715F" );
        cameraInfo.setString( "iso", "40" );
        cameraInfo.setString( "exposureProgram", "Program normal" );
        cameraInfo.setString( "aperture", "f/1.8" );

        final PropertySet mediaSet = new PropertySet();
        mediaSet.setSet( "imageInfo", imageInfo );
        mediaSet.setSet( "cameraInfo", cameraInfo );

        final PropertyTree tree = new PropertyTree();
        tree.setSet( "media", mediaSet );
        return tree;
    }

    public static Page newPage()
    {
        final Page.Builder builder = Page.create();

        builder.config( newTinyPropertyTree() );
        builder.descriptor( DescriptorKey.from( "my-app-key:mycontroller" ) );
        builder.regions( newPageRegions() );

        return builder.build();
    }

    public static PropertyTree dataMediaImage()
    {
        final PropertySet mediaSet = new PropertySet();

        final PropertySet focalPointSet = mediaSet.addSet( "focalPoint" );
        focalPointSet.setDouble( "x", 0.790625 );
        focalPointSet.setDouble( "y", 0.73125 );

        final PropertySet zoomPositionSet = mediaSet.addSet( "zoomPosition" );
        zoomPositionSet.setLong( "left", 0L );
        zoomPositionSet.setLong( "top", 0L );
        zoomPositionSet.setLong( "right", 0L );
        zoomPositionSet.setLong( "bottom", 0L );

        final PropertySet cropPositionSet = mediaSet.addSet( "cropPosition" );
        zoomPositionSet.setLong( "left", 0L );
        zoomPositionSet.setLong( "top", 0L );
        zoomPositionSet.setLong( "right", 1L );
        zoomPositionSet.setLong( "bottom", 1L );
        zoomPositionSet.setLong( "zoom", 1L );

        mediaSet.setString( "attachment", "image.jpeg" );
        mediaSet.setSet( "focalPoint", focalPointSet );
        mediaSet.setSet( "zoomPosition", zoomPositionSet );
        mediaSet.setSet( "cropPosition", cropPositionSet );

        final PropertyTree tree = new PropertyTree();
        tree.setSet( "media", mediaSet );
        return tree;
    }

    public static PageRegions newPageRegions()
    {
        return PageRegions.create().add( newTopRegion() ).add( newBottomRegion() ).build();
    }

    public static Region newTopRegion()
    {
        return Region.create().name( "top" ).add( createPartComponent( "app-descriptor-x:name-x", newTinyPropertyTree() ) ).add(
            createLayoutComponent() ).add( LayoutComponent.create().build() ).build();
    }

    public static PropertyTree newImageComponentPropertyTree()
    {
        final PropertyTree tree = new PropertyTree();
        tree.setString( "caption", "Caption" );
        return tree;
    }

    public static Region newBottomRegion()
    {
        return Region.create().name( "bottom" ).add( createPartComponent( "app-descriptor-y:name-y", newTinyPropertyTree() ) ).add(
            createImageComponent( "img-id-x", "Image Component", newImageComponentPropertyTree() ) ).add(
            ImageComponent.create().build() ).build();
    }

    public static Content createContent( String contentId, String name, String parentPath )
    {
        return Content.create().id( ContentId.from( contentId ) ).name( name ).parentPath( ContentPath.from( parentPath ) ).valid(
            false ).creator( PrincipalKey.ofAnonymous() ).createdTime( Instant.parse( "1975-01-08T00:00:00Z" ) ).build();
    }

    private static ImageComponent createImageComponent( final String imageId, final String imageDisplayName,
                                                        final PropertyTree imageConfig )
    {
        final ContentId id = ContentId.from( imageId );
        return ImageComponent.create().image( id ).config( imageConfig ).build();
    }

    private static FragmentComponent createFragmentComponent( final String fragmentId, final String fragmentDisplayName )
    {
        final ContentId id = ContentId.from( fragmentId );
        return FragmentComponent.create().fragment( id ).build();
    }

    private static PartComponent createPartComponent( final String descriptorKey, final PropertyTree partConfig )
    {
        final DescriptorKey descriptor = DescriptorKey.from( descriptorKey );

        return PartComponent.create().descriptor( descriptor ).config( partConfig ).build();
    }

    private static LayoutComponent createLayoutComponent()
    {
        final Region region1 = Region.create().name( "left" ).add( PartComponent.create().build() ).add(
            TextComponent.create().text( "text text text" ).build() ).add( TextComponent.create().build() ).build();

        final Region region2 = Region.create().name( "right" ).add( createImageComponent( "image-id", "Some Image", null ) ).add(
            createFragmentComponent( "213sda-ss222", "My Fragment" ) ).build();

        final LayoutRegions layoutRegions = LayoutRegions.create().add( region1 ).add( region2 ).build();

        return LayoutComponent.create().descriptor( "layoutDescriptor:name" ).regions( layoutRegions ).build();
    }
}
