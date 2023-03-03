package com.enonic.app.guillotine.url;

import com.enonic.xp.portal.PortalRequest;

public class AttachmentUrlParams
{
    private PortalRequest portalRequest;

    private String id;

    private String name;

    private String type;

    private boolean download;

    public PortalRequest getPortalRequest()
    {
        return portalRequest;
    }

    public void setPortalRequest( final PortalRequest portalRequest )
    {
        this.portalRequest = portalRequest;
    }

    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType( final String type )
    {
        this.type = type;
    }

    public boolean isDownload()
    {
        return download;
    }

    public void setDownload( final boolean download )
    {
        this.download = download;
    }
}
