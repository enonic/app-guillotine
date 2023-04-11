package com.enonic.app.guillotine.headless;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = GraphQLSchemaProvider.class)
public class GraphQLSchemaProviderImpl
    implements GraphQLSchemaProvider
{
    private final ConcurrentMap<String, Object> schemasMap = new ConcurrentHashMap<>();

    public void registerSchema( String schemaId, Object graphQLSchema )
    {
        schemasMap.put( schemaId, graphQLSchema );
    }

    @SuppressWarnings("unchecked")
    public <T> T getSchema( String schemaId, Class<T> cls )
    {
        return (T) schemasMap.get( schemaId );

    }
}
