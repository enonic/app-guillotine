package com.enonic.app.guillotine.graphql.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;

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

    public static Map<String, Object> filterForbiddenContent( Map<String, Object> content, GuillotineContext context )
    {
        if ( content == null )
        {
            return null;
        }
        if ( context.isGlobalMode() )
        {
            return content;
        }

        for ( String allowedContentType : getAllowedContentPaths( context ) )
        {
            String contentPath = content.get( "_path" ).toString();
            if ( Objects.equals( contentPath, allowedContentType ) || contentPath.startsWith( allowedContentType + "/" ) )
            {
                return content;
            }
        }

        return null;
    }

    public static List<String> getAllowedContentPaths( GuillotineContext context )
    {
        PortalRequest portalRequest = PortalRequestAccessor.get();
        List<String> allowPaths = new ArrayList<>( context.getAllowPaths() );
        if ( portalRequest != null && portalRequest.getSite() != null )
        {
            allowPaths.add( portalRequest.getSite().getPath().toString() );
        }

        return allowPaths;
    }

}
