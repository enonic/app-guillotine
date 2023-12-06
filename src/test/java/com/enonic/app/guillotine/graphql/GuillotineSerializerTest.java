package com.enonic.app.guillotine.graphql;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GuillotineSerializerTest
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
        assertNotNull( mappedPermissions.get( "inheritsPermissions" ) );
    }
}
