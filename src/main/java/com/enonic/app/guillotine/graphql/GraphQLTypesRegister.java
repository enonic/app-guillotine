package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;

public class GraphQLTypesRegister
{

    private final Set<GraphQLType> additionalTypes = new HashSet<>();

    private final Map<String, Map<String, DataFetcher<?>>> resolvers = new LinkedHashMap<>();

    private final Map<String, TypeResolver> typeResolvers = new LinkedHashMap<>();

    private final Map<String, List<OutputObjectCreationCallbackParams>> creationCallbacks = new LinkedHashMap<>();

    public GraphQLTypesRegister()
    {
        // do nothing
    }

    public void addAdditionalType( final GraphQLType type )
    {
        this.additionalTypes.add( type );
    }

    public void addAdditionalType( final List<GraphQLType> types )
    {
        this.additionalTypes.addAll( types );
    }

    public void addResolver( final String type, final String field, final DataFetcher<?> dataFetcher )
    {
        this.resolvers.computeIfAbsent( type, k -> new LinkedHashMap<>() ).put( field, dataFetcher );
    }

    public void addTypeResolver( final String type, final TypeResolver typeResolver )
    {
        this.typeResolvers.put( type, typeResolver );
    }

    public void addCreationCallback( final String type, OutputObjectCreationCallbackParams creationCallbacks )
    {
        this.creationCallbacks.computeIfAbsent( type, k -> new ArrayList<>() ).add( creationCallbacks );
    }

    public Set<GraphQLType> getAdditionalTypes()
    {
        return additionalTypes;
    }

    public Map<String, Map<String, DataFetcher<?>>> getResolvers()
    {
        return resolvers;
    }

    public Map<String, TypeResolver> getTypeResolvers()
    {
        return typeResolvers;
    }

    public Map<String, List<OutputObjectCreationCallbackParams>> getCreationCallbacks()
    {
        return creationCallbacks;
    }
}
