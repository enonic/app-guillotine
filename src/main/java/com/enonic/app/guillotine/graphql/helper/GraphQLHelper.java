package com.enonic.app.guillotine.graphql.helper;

import java.util.List;
import java.util.Map;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;

public class GraphQLHelper
{

    public static GraphQLInterfaceType newInterface( String name, String description, List<GraphQLFieldDefinition> fields )
    {
        return GraphQLInterfaceType.newInterface().name( name ).description( description ).fields( fields ).build();
    }

    public static GraphQLObjectType newObject( String name, String description, List<GraphQLFieldDefinition> fields )
    {
        return GraphQLObjectType.newObject().name( name ).description( description ).fields( fields ).build();
    }

    public static GraphQLObjectType newObject( String name, String description, List<GraphQLType> interfaces,
                                               List<GraphQLFieldDefinition> fields )
    {
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name( name ).description( description ).fields( fields );

        if ( interfaces != null && !interfaces.isEmpty() )
        {
            interfaces.forEach( interfaceDef -> {
                if ( interfaceDef instanceof GraphQLInterfaceType )
                {
                    builder.withInterface( (GraphQLInterfaceType) interfaceDef );
                }
                if ( interfaceDef instanceof GraphQLTypeReference )
                {
                    builder.withInterface( (GraphQLTypeReference) interfaceDef );
                }
            } );
        }

        return builder.build();
    }

    public static GraphQLFieldDefinition outputField( String name, GraphQLOutputType type )
    {
        return GraphQLFieldDefinition.newFieldDefinition().name( name ).type( type ).build();
    }

    public static GraphQLFieldDefinition outputField( String name, GraphQLOutputType type, GraphQLArgument argument )
    {
        return outputField( name, type, List.of( argument ) );
    }

    public static GraphQLFieldDefinition outputField( String name, Object type, List<GraphQLArgument> arguments )
    {
        GraphQLFieldDefinition.Builder builder = GraphQLFieldDefinition.newFieldDefinition().name( name );

        if ( type instanceof GraphQLTypeReference )
        {
            builder.type( (GraphQLTypeReference) type );
        }
        else if ( type instanceof GraphQLObjectType.Builder )
        {
            builder.type( (GraphQLObjectType.Builder) type );
        }
        else if ( type instanceof GraphQLInterfaceType.Builder )
        {
            builder.type( (GraphQLInterfaceType.Builder) type );
        }
        else if ( type instanceof GraphQLUnionType.Builder )
        {
            builder.type( (GraphQLUnionType.Builder) type );
        }
        else if ( type instanceof GraphQLOutputType )
        {
            builder.type( (GraphQLOutputType) type );
        }

        if ( arguments != null && !arguments.isEmpty() )
        {
            builder.arguments( arguments );
        }

        return builder.build();
    }

    public static GraphQLArgument newArgument( String name, GraphQLInputType type )
    {
        return GraphQLArgument.newArgument().name( name ).type( type ).build();
    }

    public static GraphQLEnumType newEnum( String name, String description, Map<String, Object> values )
    {
        GraphQLEnumType.Builder enumType = GraphQLEnumType.newEnum().name( name ).description( description );
        values.forEach( enumType::value );
        return enumType.build();
    }

    public static GraphQLEnumType newEnum( String name, String description, List<String> values )
    {
        GraphQLEnumType.Builder enumType = GraphQLEnumType.newEnum().name( name ).description( description );
        values.forEach( enumType::value );
        return enumType.build();
    }

    public static GraphQLInputObjectType newInputObject( String name, String description, List<GraphQLInputObjectField> fields )
    {
        return GraphQLInputObjectType.newInputObject().name( name ).description( description ).fields( fields ).build();
    }

    public static GraphQLInputObjectField inputField( String name, String description, GraphQLInputType type )
    {
        return GraphQLInputObjectField.newInputObjectField().name( name ).description( description ).type( type ).build();
    }

    public static GraphQLInputObjectField inputField( String name, GraphQLInputType type )
    {
        return GraphQLInputObjectField.newInputObjectField().name( name ).type( type ).build();
    }

    public static GraphQLUnionType newUnion( String name, String description, List<GraphQLType> possibleTypes )
    {
        GraphQLUnionType.Builder unionType = GraphQLUnionType.newUnionType().name( name ).description( description );

        if ( possibleTypes != null )
        {
            possibleTypes.forEach( possibleType -> {
                if ( possibleType instanceof GraphQLObjectType )
                {
                    unionType.possibleType( (GraphQLObjectType) possibleType );
                }
                else if ( possibleType instanceof GraphQLTypeReference )
                {
                    unionType.possibleType( (GraphQLTypeReference) possibleType );
                }
            } );
        }
        return unionType.build();
    }
}
