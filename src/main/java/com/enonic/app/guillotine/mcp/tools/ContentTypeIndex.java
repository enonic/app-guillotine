package com.enonic.app.guillotine.mcp.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.fetchers.ContentAwareDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.DynamicDataFetcher;
import com.enonic.app.guillotine.graphql.helper.NamingHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;

/**
 * The GraphQL object type of each content type in a Guillotine schema. Guillotine describes the type it generates for a content type
 * as {@code "<title> - <app:name>"} and names it after the content type; content of a type without one resolves to
 * {@link #UNTYPED_CONTENT}.
 */
final class ContentTypeIndex
{
    static final String CONTENT = "Content";

    static final String UNTYPED_CONTENT = "UntypedContent";

    private static final Pattern CONTENT_TYPE_NAME = Pattern.compile( "[^\\s:]+:[^\\s:]+" );

    private final GraphQLSchema schema;

    private final Map<String, GraphQLObjectType> byContentType = new TreeMap<>();

    private final Map<String, String> byLowerCaseName = new HashMap<>();

    /**
     * The index of {@code schema} for the content types in {@code registered}. A type whose description an extension replaced is found
     * by the name Guillotine generates for it; a registered type without a GraphQL type of its own maps to {@link #UNTYPED_CONTENT}.
     */
    ContentTypeIndex( final GraphQLSchema schema, final Iterable<ContentTypeName> registered )
    {
        this.schema = schema;
        final Set<GraphQLObjectType> implementations = new HashSet<>();
        if ( schema.getType( CONTENT ) instanceof GraphQLInterfaceType contentInterface )
        {
            for ( final GraphQLObjectType type : schema.getImplementations( contentInterface ) )
            {
                implementations.add( type );
                final String name = contentTypeName( type );
                if ( name != null )
                {
                    put( name, type );
                }
            }
        }
        final Set<GraphQLObjectType> mapped = new HashSet<>( byContentType.values() );
        final GraphQLObjectType untyped = schema.getType( UNTYPED_CONTENT ) instanceof GraphQLObjectType type ? type : null;
        for ( final ContentTypeName name : registered != null ? registered : List.<ContentTypeName>of() )
        {
            if ( byContentType.containsKey( name.toString() ) )
            {
                continue;
            }
            final GraphQLObjectType generated = schema.getType( generatedName( name ) ) instanceof GraphQLObjectType type &&
                implementations.contains( type ) && !mapped.contains( type ) ? type : untyped;
            if ( generated != null )
            {
                put( name.toString(), generated );
            }
        }
    }

    private void put( final String name, final GraphQLObjectType type )
    {
        if ( byContentType.putIfAbsent( name, type ) == null )
        {
            byLowerCaseName.putIfAbsent( name.toLowerCase( Locale.ROOT ), name );
        }
    }

    private static String generatedName( final ContentTypeName name )
    {
        return StringNormalizer.create( name.getApplicationKey().toString() ) + "_" +
            NamingHelper.camelCase( StringNormalizer.create( name.getLocalName() ) );
    }

    static List<ContentTypeName> registeredNames( final ContentTypeService contentTypeService )
    {
        final ContentTypes all = contentTypeService.getAll();
        return all == null ? List.of() : all.stream().map( ContentType::getName ).toList();
    }

    GraphQLSchema schema()
    {
        return schema;
    }

    Set<String> contentTypes()
    {
        return byContentType.keySet();
    }

    GraphQLObjectType byContentType( final String contentType )
    {
        return byContentType.get( contentType );
    }

    /**
     * The registered spelling of {@code contentType}, which the search index returns lowercased, or {@code contentType} itself when no
     * GraphQL type has that name.
     */
    String spelling( final String contentType )
    {
        return byLowerCaseName.getOrDefault( contentType.toLowerCase( Locale.ROOT ), contentType );
    }

    /**
     * The GraphQL type that content of {@code contentType} has in query results.
     */
    String graphQlTypeName( final String contentType )
    {
        final GraphQLObjectType type = byContentType.get( spelling( contentType ) );
        return type != null ? type.getName() : UNTYPED_CONTENT;
    }

    /**
     * The content type whose GraphQL type is {@code graphQlType}, or {@code null}.
     */
    String contentTypeOf( final String graphQlType )
    {
        if ( UNTYPED_CONTENT.equals( graphQlType ) )
        {
            return null;
        }
        return byContentType.entrySet()
            .stream()
            .filter( entry -> entry.getValue().getName().equals( graphQlType ) )
            .map( Map.Entry::getKey )
            .findFirst()
            .orElse( null );
    }

    /**
     * Whether {@code field} of {@code parent} resolves through a Guillotine extension, that is runs code of another application.
     */
    boolean runsAppCode( final GraphQLFieldsContainer parent, final GraphQLFieldDefinition field )
    {
        DataFetcher<?> fetcher = schema.getCodeRegistry().getDataFetcher( FieldCoordinates.coordinates( parent, field ), field );
        if ( fetcher instanceof ContentAwareDataFetcher wrapped )
        {
            fetcher = wrapped.getDelegate();
        }
        return fetcher instanceof DynamicDataFetcher;
    }

    private static String contentTypeName( final GraphQLObjectType type )
    {
        final String description = type.getDescription();
        final int separator = description != null ? description.lastIndexOf( " - " ) : -1;
        if ( separator < 0 )
        {
            return null;
        }
        final String name = description.substring( separator + 3 ).trim();
        return CONTENT_TYPE_NAME.matcher( name ).matches() ? name : null;
    }
}
