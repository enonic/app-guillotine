package com.enonic.app.guillotine.graphql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.ImmutableMap;

import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;

import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;

public class GuillotineContext
{
    private final ConcurrentMap<String, GraphQLType> types = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> contentTypesDictionary = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Object> options = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Integer> nameCountMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<FieldCoordinates, DataFetcher<?>> dataFetchers = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, TypeResolver> typeResolvers = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<String> applications;

    private final ImmutableMap<String, MacroDescriptor> macroDecorators;

    private final CopyOnWriteArrayList<String> allowPaths;

    private GuillotineContext( final Builder builder )
    {
        this.applications = builder.applications;
        this.allowPaths = builder.allowPaths;
        this.macroDecorators = ImmutableMap.<String, MacroDescriptor>builder().putAll( builder.macroDecorators ).build();
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

    public List<String> getApplications()
    {
        return applications;
    }

    public List<String> getAllowPaths()
    {
        return allowPaths;
    }

    public Map<String, MacroDescriptor> getMacroDecorators()
    {
        return macroDecorators;
    }

    public void registerType( String name, GraphQLType type )
    {
        types.put( name, type );
    }

    public void registerContentType( String rawContentType, GraphQLObjectType objectType )
    {
        types.put( objectType.getName(), objectType );
        contentTypesDictionary.put( rawContentType, objectType.getName() );
    }

    public List<GraphQLType> getAllTypes()
    {
        return new CopyOnWriteArrayList<>( types.values() );
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

    public String getContentType( String name )
    {
        return contentTypesDictionary.get( name );
    }

    public ConcurrentMap<FieldCoordinates, DataFetcher<?>> getDataFetchers()
    {
        return dataFetchers;
    }

    public ConcurrentMap<String, TypeResolver> getTypeResolvers()
    {
        return typeResolvers;
    }

    public void registerDataFetcher( String typeName, String fieldName, DataFetcher<?> dataFetcher )
    {
        dataFetchers.put( FieldCoordinates.coordinates( typeName, fieldName ), dataFetcher );
    }

    public void registerTypeResolver( String typeName, TypeResolver typeResolver )
    {
        typeResolvers.put( typeName, typeResolver );
    }

    public String uniqueName( String name )
    {
        if ( name == null || "".equals( name ) )
        {
            throw new IllegalArgumentException( "Name can not be empty or null" );
        }

        nameCountMap.compute( name, ( k, v ) -> v == null ? 1 : v + 1 );

        return nameCountMap.get( name ) == 1 ? name : name + "_" + nameCountMap.get( name );
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private final CopyOnWriteArrayList<String> applications = new CopyOnWriteArrayList<>();

        private final CopyOnWriteArrayList<String> allowPaths = new CopyOnWriteArrayList<>();

        private final Map<String, MacroDescriptor> macroDecorators = new HashMap<>();

        public Builder()
        {

        }

        public Builder addApplications( final List<String> applications )
        {
            if ( applications != null )
            {
                this.applications.addAll( applications );
            }
            return this;
        }

        public Builder addAllowPaths( final List<String> allowPaths )
        {
            if ( allowPaths != null )
            {
                this.allowPaths.addAll( allowPaths );
            }
            return this;
        }

        public Builder addMacroDecorators( final Map<String, MacroDescriptor> macroDecorators )
        {
            if ( macroDecorators != null )
            {
                this.macroDecorators.putAll( macroDecorators );
            }
            return this;
        }

        public GuillotineContext build()
        {
            return new GuillotineContext( this );
        }
    }

}
