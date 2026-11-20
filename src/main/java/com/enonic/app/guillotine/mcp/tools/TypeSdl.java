package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeUtil;

/**
 * Compact SDL of one content type: its own fields (those of the {@code Content} interface are named once in a comment), the types
 * generated for it ({@code <Type>_Data}, item sets, option sets and their enums) in full, and the other object types its fields use
 * as one comment line of field names each. Fields that run another application's code end with {@code # app-defined}.
 */
final class TypeSdl
{
    static final String APP_DEFINED = "  # app-defined";

    private TypeSdl()
    {
    }

    static String print( final ContentTypeIndex index, final GraphQLObjectType root, final int maxChars )
    {
        final GraphQLInterfaceType content = (GraphQLInterfaceType) index.schema().getType( ContentTypeIndex.CONTENT );
        final String ownPrefix = root.getName() + "_";
        final StringBuilder text = new StringBuilder();
        final Set<String> seen = new HashSet<>( Set.of( root.getName() ) );
        final Set<GraphQLNamedType> shared = new LinkedHashSet<>();
        final Deque<GraphQLNamedType> queue = new ArrayDeque<>();
        queue.add( root );
        while ( !queue.isEmpty() )
        {
            final GraphQLNamedType type = queue.poll();
            if ( !text.isEmpty() )
            {
                text.append( '\n' );
            }
            if ( type instanceof GraphQLEnumType enumType )
            {
                text.append( "enum " )
                    .append( enumType.getName() )
                    .append( " { " )
                    .append( enumType.getValues().stream().map( GraphQLEnumValueDefinition::getName ).collect( Collectors.joining( " " ) ) )
                    .append( " }" );
                continue;
            }
            final GraphQLFieldsContainer container = (GraphQLFieldsContainer) type;
            text.append( "type " ).append( type.getName() );
            if ( type == root && content != null )
            {
                text.append( " implements Content" );
            }
            text.append( " {\n" );
            if ( type == root && content != null )
            {
                text.append( "  # Content fields: " )
                    .append( content.getFieldDefinitions().stream().map( GraphQLFieldDefinition::getName ).collect( Collectors.joining( " " ) ) )
                    .append( '\n' );
            }
            for ( final GraphQLFieldDefinition field : container.getFieldDefinitions() )
            {
                final boolean inherited = type == root && content != null && content.getFieldDefinition( field.getName() ) != null;
                final boolean appDefined = index.runsAppCode( container, field );
                if ( inherited && !appDefined )
                {
                    continue;
                }
                text.append( "  " ).append( field.getName() ).append( arguments( field.getArguments() ) ).append( ": " )
                    .append( GraphQLTypeUtil.simplePrint( field.getType() ) );
                if ( appDefined || ( type == root && isAppAdded( field ) ) )
                {
                    text.append( APP_DEFINED );
                }
                text.append( '\n' );
                if ( GraphQLTypeUtil.unwrapAll( field.getType() ) instanceof GraphQLNamedType next && seen.add( next.getName() ) )
                {
                    if ( next.getName().startsWith( ownPrefix ) &&
                        ( next instanceof GraphQLFieldsContainer || next instanceof GraphQLEnumType ) )
                    {
                        queue.add( next );
                    }
                    else if ( next instanceof GraphQLFieldsContainer && !TypeExamples.isContent( next ) )
                    {
                        shared.add( next );
                    }
                }
            }
            text.append( '}' );
        }
        for ( final GraphQLNamedType type : shared )
        {
            text.append( "\n# " )
                .append( type.getName() )
                .append( ": " )
                .append( ( (GraphQLFieldsContainer) type ).getFieldDefinitions()
                             .stream()
                             .map( GraphQLFieldDefinition::getName )
                             .collect( Collectors.joining( " " ) ) );
        }
        return cut( text.toString(), maxChars );
    }

    /**
     * Whether Guillotine itself does not generate {@code field} on a content type: it adds only {@code data}, and {@code mediaUrl} and
     * {@code imageUrl} on media types, to the fields of the {@code Content} interface.
     */
    static boolean isAppAdded( final GraphQLFieldDefinition field )
    {
        return !List.of( "data", "mediaUrl", "imageUrl" ).contains( field.getName() );
    }

    private static String arguments( final List<GraphQLArgument> arguments )
    {
        if ( arguments.isEmpty() )
        {
            return "";
        }
        final List<String> printed = new ArrayList<>();
        arguments.forEach( argument -> printed.add( argument.getName() + ": " + GraphQLTypeUtil.simplePrint( argument.getType() ) ) );
        return "(" + String.join( ", ", printed ) + ")";
    }

    private static String cut( final String text, final int maxChars )
    {
        if ( text.length() <= maxChars )
        {
            return text;
        }
        final String marker = "\n# cut to " + maxChars + " characters";
        final int end = text.lastIndexOf( '\n', Math.max( 0, maxChars - marker.length() ) );
        return text.substring( 0, Math.max( 0, end ) ) + marker;
    }
}
