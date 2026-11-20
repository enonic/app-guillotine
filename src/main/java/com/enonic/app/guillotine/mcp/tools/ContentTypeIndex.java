package com.enonic.app.guillotine.mcp.tools;

import java.util.HashMap;
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

/**
 * The GraphQL object type of each content type in a Guillotine schema. Guillotine describes the type it generates for a content type
 * as {@code "<title> - <app:name>"}; content of a type without one resolves to {@link #UNTYPED_CONTENT}.
 */
final class ContentTypeIndex
{
    static final String CONTENT = "Content";

    static final String UNTYPED_CONTENT = "UntypedContent";

    private static final Pattern CONTENT_TYPE_NAME = Pattern.compile( "[^\\s:]+:[^\\s:]+" );

    private final GraphQLSchema schema;

    private final Map<String, GraphQLObjectType> byContentType = new TreeMap<>();

    private final Map<String, String> byLowerCaseName = new HashMap<>();

    ContentTypeIndex( final GraphQLSchema schema )
    {
        this.schema = schema;
        if ( schema.getType( CONTENT ) instanceof GraphQLInterfaceType contentInterface )
        {
            for ( final GraphQLObjectType type : schema.getImplementations( contentInterface ) )
            {
                final String name = contentTypeName( type );
                if ( name != null )
                {
                    byContentType.putIfAbsent( name, type );
                    byLowerCaseName.putIfAbsent( name.toLowerCase( Locale.ROOT ), name );
                }
            }
        }
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
        if ( schema.getType( graphQlType ) instanceof GraphQLObjectType type )
        {
            final String name = contentTypeName( type );
            return name != null && byContentType.get( name ) == type ? name : null;
        }
        return null;
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
