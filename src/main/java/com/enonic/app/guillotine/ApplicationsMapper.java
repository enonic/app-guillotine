package com.enonic.app.guillotine;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationKeys;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class ApplicationsMapper
    implements MapSerializable
{
    private final ApplicationKeys applicationKeys;

    public ApplicationsMapper( final ApplicationKeys applicationKeys )
    {
        this.applicationKeys = applicationKeys;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.array("applications");
        for ( ApplicationKey applicationKey : applicationKeys )
        {
            gen.value( applicationKey.getName() );
        }
        gen.end();
    }
}
