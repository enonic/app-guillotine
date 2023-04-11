package com.enonic.app.guillotine.headless;

public interface GraphQLSchemaProvider
{
    void registerSchema( String schemaId, Object graphQLSchema );

    <T> T getSchema( String schemaId, Class<T> cls );
}
