package com.enonic.app.guillotine.graphql.helper;

import java.util.Map;

import com.enonic.xp.context.ContextAccessor;

public class SecurityHelper
{
    public static boolean isAdmin()
    {
        return ContextAccessor.current().getAuthInfo().hasRole( "system.admin" );
    }

    public static boolean isCmsAdmin()
    {
        return ContextAccessor.current().getAuthInfo().hasRole( "cms.admin" );
    }

    public static boolean isCmsUser()
    {
        return ContextAccessor.current().getAuthInfo().hasRole( "cms.cm.app" );
    }

    public static boolean canAccessCmsData()
    {
        return isAdmin() || isCmsAdmin() || isCmsUser();
    }

    public static Map<String, Object> filterForbiddenContent( Map<String, Object> content )
    {
        return content;
    }

}
