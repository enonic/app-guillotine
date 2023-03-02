package com.enonic.app.guillotine.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.enonic.app.guillotine.url.AssetUrlParams;
import com.enonic.app.guillotine.url.ImageUrlParams;
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

    public String assetUrl( String path, String type )
    {
        final AssetUrlParams params = new AssetUrlParams();
        params.setPortalRequest( portalRequestSupplier.get() );
        params.setPath( path );
        params.setType( type );
        return urlServiceSupplier.get().assetUrl( params );
    }

    public String imageUrl( final ScriptValue scriptValue )
    {
        Map<String, Object> paramsAsMap;
        if ( scriptValue == null )
        {
            paramsAsMap = new HashMap<>();
        } else {
            paramsAsMap = scriptValue.getMap();
        }

        final ImageUrlParams params = new ImageUrlParams();

        params.setId( paramsAsMap.get( "id" ).toString() );
        params.setScale( paramsAsMap.get( "scale" ).toString() );
        params.setType( Objects.toString( paramsAsMap.get( "type" ), UrlTypeConstants.SERVER_RELATIVE ) );
        params.setPortalRequest( portalRequestSupplier.get() );

        return urlServiceSupplier.get().imageUrl( params );
    }
}
