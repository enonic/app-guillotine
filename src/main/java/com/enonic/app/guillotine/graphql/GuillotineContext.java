package com.enonic.app.guillotine.graphql;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;

public class GuillotineContext
{
    private final GraphQLCodeRegistry.Builder codeRegisterBuilder = GraphQLCodeRegistry.newCodeRegistry();

    private final ConcurrentMap<String, GraphQLType> types = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, GraphQLObjectType> contentTypes = new ConcurrentHashMap<>();

    private final CopyOnWriteArraySet<GraphQLType> dictionary = new CopyOnWriteArraySet<>();

    private final CopyOnWriteArrayList<String> applications = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<String> allowPaths = new CopyOnWriteArrayList<>();

    private final ConcurrentMap<String, Object> options = new ConcurrentHashMap<>();

    public GuillotineContext( final CreateSchemaParams params )
    {
        if ( params != null )
        {
            if ( params.getApplications() != null )
            {
                this.applications.addAll( params.getApplications() );
            }
            if ( params.getAllowPaths() != null )
            {
                this.allowPaths.addAll( params.getAllowPaths() );
            }
        }
    }

    public boolean isGlobalMode()
    {
        if ( options.containsKey( "__globalModeOn" ) )
        {
            return options.get( "__globalModeOn" ) == null || (boolean) options.get( "__globalModeOn" );
        }

        PortalRequest portalRequest = PortalRequestAccessor.get();
        boolean globalModeOn = portalRequest.getSite() == null;
        options.put( "__globalModeOn", globalModeOn );
        return globalModeOn;
    }

    public Set<GraphQLType> getDictionary()
    {
        return dictionary;
    }

    public List<String> getApplications()
    {
        return applications;
    }

    public List<String> getAllowPaths()
    {
        return allowPaths;
    }

    public void registerType( String name, GraphQLType type )
    {
        types.put( name, type );
    }

    public void addDictionaryType( GraphQLType objectType )
    {
        dictionary.add( objectType );
    }

    public void registerContentType( String contentTypeName, GraphQLObjectType objectType )
    {
        types.put( objectType.getName(), objectType );
        contentTypes.put( contentTypeName, objectType );
        addDictionaryType( objectType );
    }

    public GraphQLEnumType getEnumType( String name )
    {
        return (GraphQLEnumType) types.get( name );
    }

    public GraphQLInterfaceType getInterfaceType( String name )
    {
        return (GraphQLInterfaceType) types.get( name );
    }

    public GraphQLInputObjectType getInputType( String name )
    {
        return (GraphQLInputObjectType) types.get( name );
    }

    public GraphQLObjectType getOutputType( String name )
    {
        return (GraphQLObjectType) types.get( name );
    }

    public GraphQLObjectType getContentType( String name )
    {
        return contentTypes.get( name );
    }

    public void registerDataFetcher( String typeName, String fieldName, DataFetcher<?> dataFetcher )
    {
        codeRegisterBuilder.dataFetcher( FieldCoordinates.coordinates( typeName, fieldName ), dataFetcher );
    }

    public void registerTypeResolver( String typeName, TypeResolver typeResolver )
    {
        codeRegisterBuilder.typeResolver( typeName, typeResolver );
    }

    public GraphQLCodeRegistry getCodeRegistry()
    {
        return codeRegisterBuilder.build();
    }

}
