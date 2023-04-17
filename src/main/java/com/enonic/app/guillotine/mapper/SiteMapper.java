package com.enonic.app.guillotine.mapper;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;
import com.enonic.xp.site.Site;

public class SiteMapper
    implements MapSerializable
{
    private final Site site;

    public SiteMapper( final Site site )
    {
        this.site = site;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        new ContentMapper( site ).serialize( gen );
        gen.value( "description", site.getDescription() );
    }
}
