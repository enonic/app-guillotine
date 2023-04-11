package com.enonic.app.guillotine.headless;

import com.enonic.xp.annotation.PublicApi;

@PublicApi
public interface HeadlessService
{

    HeadlessSchema createSchema( HeadlessSchemaParams params );

    HeadlessObject createOutputObject( HeadlessObjectParams params );
}
