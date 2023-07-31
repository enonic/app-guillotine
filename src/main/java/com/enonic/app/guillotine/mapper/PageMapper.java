package com.enonic.app.guillotine.mapper;

import com.enonic.xp.content.ContentId;
import com.enonic.xp.page.Page;
import com.enonic.xp.page.PageRegions;
import com.enonic.xp.region.Component;
import com.enonic.xp.region.ComponentPath;
import com.enonic.xp.region.Region;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public final class PageMapper
    implements MapSerializable
{
    static final String PAGE = "page";

    static final String TEMPLATE = "template";

    private final Page value;

    private final ContentId contentId;

    public PageMapper( final Page value, final ContentId contentId )
    {
        this.value = value;
        this.contentId = contentId;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        serialize( gen, this.value );
    }

    private void serialize( final MapGenerator gen, final Page value )
    {
        if ( value.isFragment() )
        {
            serializeFragment( gen, value.getFragment() );
        }
        else
        {
            serializePage( gen, value );
        }
    }

    private void serializeFragment( final MapGenerator gen, final Component value )
    {
        gen.map( "fragment" );

        new ComponentMapper( value, contentId ).serialize( gen );

        gen.end();
    }

    private void serializePage( final MapGenerator gen, final Page value )
    {
        gen.map( PAGE );

        gen.value( "type", PAGE );
        gen.value( "path", ComponentPath.DIVIDER );
        gen.value( TEMPLATE, value.getTemplate() );
        gen.value( "descriptor", value.getDescriptor() );

        if ( value.hasConfig() )
        {
            gen.map( "config" );
            new PropertyTreeMapper( value.getConfig(), contentId.toString() ).serialize( gen );
            gen.end();
        }
        if ( value.hasRegions() )
        {
            serializeRegions( gen, value.getRegions() );
        }

        gen.end();
    }

    private void serializeRegions( final MapGenerator gen, final PageRegions values )
    {
        gen.map( "regions" );

        if ( values != null )
        {
            for ( final Region region : values )
            {
                new RegionMapper( region, contentId ).serialize( gen );
            }
        }

        gen.end();
    }
}
