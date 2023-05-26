package com.enonic.app.guillotine.graphql;

import java.util.List;
import java.util.Objects;
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
    private final GraphQLCodeRegistry.Builder codeRegisterBuilder;

    private final ConcurrentMap<String, GraphQLType> types = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, String> contentTypesDictionary = new ConcurrentHashMap<>();

    private final CopyOnWriteArraySet<GraphQLType> dictionary = new CopyOnWriteArraySet<>();

    private final ConcurrentMap<String, Object> options = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Integer> nameCountMap = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<String> applications;

    private final CopyOnWriteArrayList<String> allowPaths;

    private GuillotineContext( final Builder builder )
    {
        this.codeRegisterBuilder = builder.codeRegisterBuilder;
        this.applications = builder.applications;
        this.allowPaths = builder.allowPaths;
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

    public void registerContentType( String rawContentType, GraphQLObjectType objectType )
    {
        types.put( objectType.getName(), objectType );
        contentTypesDictionary.put( rawContentType, objectType.getName() );
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

    public String getContentType( String name )
    {
        return contentTypesDictionary.get( name );
    }

    public void registerDataFetcher( String typeName, String fieldName, DataFetcher<?> dataFetcher )
    {
        codeRegisterBuilder.dataFetcher( FieldCoordinates.coordinates( typeName, fieldName ), dataFetcher );
    }

    public void registerTypeResolver( String typeName, TypeResolver typeResolver )
    {
        codeRegisterBuilder.typeResolver( typeName, typeResolver );
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

    public static Builder create( final GraphQLCodeRegistry.Builder codeRegisterBuilder )
    {
        return new Builder( codeRegisterBuilder );
    }

    public static final class Builder
    {
        private final GraphQLCodeRegistry.Builder codeRegisterBuilder;

        private final CopyOnWriteArrayList<String> applications = new CopyOnWriteArrayList<>();

        private final CopyOnWriteArrayList<String> allowPaths = new CopyOnWriteArrayList<>();

        public Builder( final GraphQLCodeRegistry.Builder codeRegisterBuilder )
        {
            this.codeRegisterBuilder = Objects.requireNonNull( codeRegisterBuilder );
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

        public GuillotineContext build()
        {
            return new GuillotineContext( this );
        }
    }

}
