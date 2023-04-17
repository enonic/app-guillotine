package com.enonic.app.guillotine.helper;

import com.enonic.app.guillotine.graphql.CreateSchemaParams;

public class ParamsHelper
{
    public CreateSchemaParams.Builder newInstance()
    {
        return CreateSchemaParams.create();
    }
}
