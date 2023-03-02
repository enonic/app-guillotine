package com.enonic.app.guillotine.url;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.UrlTypeConstants;

public class ImageUrlParams
{
    private PortalRequest portalRequest;

    private String type = UrlTypeConstants.SERVER_RELATIVE;

    private String id;

    private String path;

    private String background;

    private Integer quality;

    private String filter;

    private String format;

    private String scale;

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

    public String getPath()
    {
        return path;
    }

    public void setPath( final String path )
    {
        this.path = path;
    }

    public String getBackground()
    {
        return background;
    }

    public void setBackground( final String background )
    {
        this.background = background;
    }

    public Integer getQuality()
    {
        return quality;
    }

    public void setQuality( final Integer quality )
    {
        this.quality = quality;
    }

    public String getFilter()
    {
        return filter;
    }

    public void setFilter( final String filter )
    {
        this.filter = filter;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat( final String format )
    {
        this.format = format;
    }

    public String getScale()
    {
        return scale;
    }

    public void setScale( final String scale )
    {
        this.scale = scale;
    }

}
