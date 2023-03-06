package com.enonic.app.guillotine.url;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.UrlTypeConstants;

public class PageUrlParams
{
    private PortalRequest portalRequest;

    private String type = UrlTypeConstants.SERVER_RELATIVE;

    private String id;

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

    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }
}
