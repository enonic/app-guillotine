package com.enonic.app.guillotine.url;

import java.util.concurrent.Callable;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.auth.AuthenticationInfo;

@Component(immediate = true)
public class UrlServiceImpl
    implements UrlService
{

    private final ContentService contentService;

    @Activate
    public UrlServiceImpl( final @Reference ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public String imageUrl( final ImageUrlParams params )
    {
        return runWithAdminRole( () -> new ImageUrlBuilder( params, contentService ).buildUrl() );
    }

    @Override
    public String attachmentUrl( final AttachmentUrlParams params )
    {
        return runWithAdminRole( () -> new AttachmentUrlBuilder( params, contentService ).buildUrl() );
    }

    private <T> T runWithAdminRole( final Callable<T> callable )
    {
        final Context context = ContextAccessor.current();
        final AuthenticationInfo authenticationInfo =
            AuthenticationInfo.copyOf( context.getAuthInfo() ).principals( RoleKeys.ADMIN ).build();
        return ContextBuilder.from( context ).authInfo( authenticationInfo ).build().callWith( callable );
    }
}
