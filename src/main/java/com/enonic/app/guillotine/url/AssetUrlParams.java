package com.enonic.app.guillotine.url;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.UrlTypeConstants;

public class AssetUrlParams
{
    private PortalRequest portalRequest;

    private String type = UrlTypeConstants.SERVER_RELATIVE;

    private String application;

    private String path;

    public PortalRequest getPortalRequest()
    {
        return portalRequest;
    }

    public void setPortalRequest( final PortalRequest portalRequest )
    {
        this.portalRequest = portalRequest;
    }

    public String getType()
    {
        return type;
    }

    public void setType( final String type )
    {
        this.type = type;
    }

    public String getApplication()
    {
        return application;
    }

    public void setApplication( final String application )
    {
        this.application = application;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( final String path )
    {
        this.path = path;
    }
}
