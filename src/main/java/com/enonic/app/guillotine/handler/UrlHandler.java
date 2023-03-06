package com.enonic.app.guillotine.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.enonic.app.guillotine.url.AttachmentUrlParams;
import com.enonic.app.guillotine.url.ImageUrlParams;
import com.enonic.app.guillotine.url.PageUrlParams;
import com.enonic.app.guillotine.url.UrlService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.UrlTypeConstants;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class UrlHandler
    implements ScriptBean
{
    private Supplier<PortalRequest> portalRequestSupplier;

    private Supplier<UrlService> urlServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.portalRequestSupplier = context.getBinding( PortalRequest.class );
        this.urlServiceSupplier = context.getService( UrlService.class );
    }


    public String imageUrl( final ScriptValue scriptValue )
    {
        final Map<String, Object> paramsAsMap = getParametersAsMap( scriptValue );

        final ImageUrlParams params = new ImageUrlParams();

        params.setId( paramsAsMap.get( "id" ).toString() );
        params.setScale( paramsAsMap.get( "scale" ).toString() );
        params.setType( Objects.toString( paramsAsMap.get( "type" ), UrlTypeConstants.SERVER_RELATIVE ) );
        params.setPortalRequest( portalRequestSupplier.get() );

        return urlServiceSupplier.get().imageUrl( params );
    }

    public String attachmentUrl( final ScriptValue scriptValue )
    {
        final Map<String, Object> paramsAsMap = getParametersAsMap( scriptValue );

        final AttachmentUrlParams params = new AttachmentUrlParams();

        params.setId( paramsAsMap.get( "id" ).toString() );
        params.setName( Objects.toString( params.getName(), null ) );
        params.setDownload( "true".equals( Objects.toString( paramsAsMap.get( "download" ), "false" ) ) );
        params.setType( Objects.toString( paramsAsMap.get( "type" ), UrlTypeConstants.SERVER_RELATIVE ) );
        params.setPortalRequest( portalRequestSupplier.get() );

        return urlServiceSupplier.get().attachmentUrl( params );
    }

    public String pageUrl( final ScriptValue scriptValue )
    {
        final Map<String, Object> paramsAsMap = getParametersAsMap( scriptValue );

        final PageUrlParams params = new PageUrlParams();

        params.setId( paramsAsMap.get( "id" ).toString() );
        params.setType( Objects.toString( paramsAsMap.get( "type" ), UrlTypeConstants.SERVER_RELATIVE ) );
        params.setPortalRequest( portalRequestSupplier.get() );

        return urlServiceSupplier.get().pageUrl( params );
    }

    private Map<String, Object> getParametersAsMap( ScriptValue scriptValue )
    {
        return scriptValue != null ? scriptValue.getMap() : new HashMap<>();
    }
}
