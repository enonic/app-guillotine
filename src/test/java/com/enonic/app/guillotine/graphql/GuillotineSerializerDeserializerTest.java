package com.enonic.app.guillotine.graphql;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.ContentDeserializer;
import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.attachment.Attachments;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentInheritType;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentPublishInfo;
import com.enonic.xp.content.ExtraData;
import com.enonic.xp.content.WorkflowCheckState;
import com.enonic.xp.content.WorkflowInfo;
import com.enonic.xp.content.WorkflowState;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.page.DescriptorKey;
import com.enonic.xp.page.Page;
import com.enonic.xp.page.PageRegions;
import com.enonic.xp.region.FragmentComponent;
import com.enonic.xp.region.Region;
import com.enonic.xp.region.TextComponent;
import com.enonic.xp.schema.xdata.XDataName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GuillotineSerializerDeserializerTest
{
    @Test
    public void testSerializePermissions()
    {
        assertNull( GuillotineSerializer.serializePermissions( null ) );

        Content content = mock( Content.class );
        when( content.getPermissions() ).thenReturn( AccessControlList.create().add(
            AccessControlEntry.create().allowAll().principal( RoleKeys.CONTENT_MANAGER_ADMIN ).build() ).build() );

        Map<String, Object> mappedPermissions = CastHelper.cast( GuillotineSerializer.serializePermissions( content ) );
        assertNotNull( mappedPermissions );
        assertNotNull( mappedPermissions.get( "permissions" ) );
    }

    @Test
    void test()
    {
        Content source = newContent();

        Map<String, Object> serialized = GuillotineSerializer.serialize( source );
        Content deserialized = ContentDeserializer.deserialize( serialized );

        assertEquals( source.getId(), deserialized.getId() );
    }

    private static Content newContent()
    {
        final Content.Builder<?> builder = Content.create();

        builder.id( ContentId.from( "123456" ) );
        builder.name( "mycontent" );
        builder.displayName( "My Content" );
        builder.parentPath( ContentPath.from( "/a/b" ) );
        builder.creator( PrincipalKey.from( "user:system:admin" ) );
        builder.modifier( PrincipalKey.from( "user:system:admin" ) );
        builder.owner( PrincipalKey.from( "user:system:admin" ) );
        builder.modifiedTime( Instant.ofEpochSecond( 0 ) );
        builder.createdTime( Instant.ofEpochSecond( 0 ) );
        PropertyTree data = new PropertyTree();
        data.setString( "key1", "value1" );
        builder.data( data );
        builder.addExtraData( new ExtraData( XDataName.from( "myapplication:myschema" ), ContentFixtures.newTinyPropertyTree() ) );
        builder.page( newPage() );
        builder.attachments( Attachments.from(
            Attachment.create().name( "image.jpeg" ).label( "source" ).mimeType( "image/jpeg" ).size( 12345 ).sha512(
                "sha512" ).build() ) );
        builder.workflowInfo( WorkflowInfo.create().state( WorkflowState.PENDING_APPROVAL ).checks(
            Map.of( "check1", WorkflowCheckState.APPROVED ) ).build() );
        builder.publishInfo( ContentPublishInfo.create().from( Instant.parse( "2016-11-03T10:00:00Z" ) ).to(
            Instant.parse( "2016-11-23T10:00:00Z" ) ).build() );
        builder.setInherit( Set.of( ContentInheritType.CONTENT ) );

        return builder.build();
    }

    private static Page newPage()
    {
        final Page.Builder builder = Page.create();

        builder.config( ContentFixtures.newTinyPropertyTree() );
        builder.descriptor( DescriptorKey.from( "my-app-key:mycontroller" ) );
        builder.regions( newPageRegions() );

        return builder.build();
    }

    public static PageRegions newPageRegions()
    {
        final Region region = Region.create( ContentFixtures.newTopRegion() ).add( TextComponent.create().text( "Text" ).build() ).add(
            FragmentComponent.create().fragment( ContentId.from( "contentId" ) ).build() ).build();
        return PageRegions.create().add( region ).build();
    }
}
