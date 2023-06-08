package com.enonic.app.guillotine.graphql.transformer;

import java.util.Objects;

import graphql.schema.GraphQLType;

import com.enonic.xp.script.ScriptValue;

public final class GraphQLExtensionValidator
{
    public static void validateTypeDefs( final ScriptValue types )
    {
        if ( !types.isObject() )
        {
            throw new IllegalArgumentException( "The \"types\" field must be object" );
        }
        types.getKeys().forEach( typeName -> {
            ScriptValue typeDef = types.getMember( typeName );
            validateTypeDef( typeName, typeDef );
        } );
    }

    public static void validateInputTypeDefs( final ScriptValue types )
    {
        if ( !types.isObject() )
        {
            throw new IllegalArgumentException( "The \"types\" field must be object" );
        }

        types.getKeys().forEach( typeName -> {
            ScriptValue typeDef = types.getMember( typeName );

            ScriptValue fields = Objects.requireNonNull( typeDef.getMember( "fields" ),
                                                         String.format( "The fields must be set for type \"%s\"", typeName ) );

            if ( !fields.isObject() )
            {
                throw new IllegalArgumentException( String.format( "The \"fields\" field must be object for type \"%s\"", typeName ) );
            }

            fields.getKeys().forEach( fieldName -> {
                ScriptValue fieldType = fields.getMember( fieldName );
                if ( !( fieldType.getValue() instanceof GraphQLType ) )
                {
                    throw new IllegalArgumentException(
                        String.format( "The \"type\" must be specified for field \"%s\" of the type \"%s\"", fieldName, typeName ) );
                }
            } );
        } );
    }

    public static void validateEnumTypes( ScriptValue types )
    {
        if ( !types.isObject() )
        {
            throw new IllegalArgumentException( "The \"enums\" field must be object" );
        }
        types.getKeys().forEach( typeName -> {
            ScriptValue typeDef = types.getMember( typeName );

            ScriptValue values = Objects.requireNonNull( typeDef.getMember( "values" ),
                                                         String.format( "The values must be set for type \"%s\"", typeName ) );

            if ( !values.isObject() )
            {
                throw new IllegalArgumentException( String.format( "The \"values\" field must be object for type \"%s\"", typeName ) );
            }
        } );
    }

    public static void validateInterfaceTypes( ScriptValue types )
    {
        if ( !types.isObject() )
        {
            throw new IllegalArgumentException( "The \"interfaces\" field must be object" );
        }
        types.getKeys().forEach( typeName -> {
            ScriptValue typeDef = types.getMember( typeName );
            validateTypeDef( typeName, typeDef );
        } );
    }

    private static void validateFields( ScriptValue typeDef, String typeName )
    {
        ScriptValue fields = typeDef.getMember( "fields" );
        if ( fields == null )
        {
            throw new IllegalArgumentException( String.format( "The fields must be set for type \"%s\"", typeName ) );
        }

        if ( !fields.isObject() )
        {
            throw new IllegalArgumentException( String.format( "The \"fields\" field must be object for type \"%s\"", typeName ) );
        }

        fields.getKeys().forEach( fieldName -> {
            ScriptValue field = fields.getMember( fieldName );
            validateFieldDef( field, typeName, fieldName );
        } );
    }

    private static void validateTypeDef( String typeName, ScriptValue typeDef )
    {
        validateDescription( typeDef, typeName );
        validateFields( typeDef, typeName );
    }

    private static void validateDescription( ScriptValue typeDef, String typeName )
    {
        ScriptValue description = typeDef.getMember( "description" );
        if ( description != null && !description.isValue() || !( description.getValue() instanceof String ) )
        {
            throw new IllegalArgumentException( String.format( "The \"description\" field must be string for the \"%s\" type", typeName ) );
        }
    }

    private static void validateFieldDef( ScriptValue fieldDef, String typeName, String fieldName )
    {
        if ( fieldDef == null || !fieldDef.isObject() )
        {
            throw new IllegalArgumentException( String.format( "The field \"%s\" must be object for type \"%s\"", fieldName, typeName ) );
        }

        ScriptValue typeDef = fieldDef.getMember( "type" );
        if ( typeDef == null )
        {
            throw new IllegalArgumentException(
                String.format( "The \"type\" must be specified for field \"%s\" of the type \"%s\"", fieldName, typeName ) );
        }
        if ( !( typeDef.getValue() instanceof GraphQLType ) )
        {
            throw new IllegalArgumentException(
                String.format( "Incorrect type for the \"type\" field for field \"%s\" of the type \"%s\"", fieldName, typeName ) );
        }

        ScriptValue argsDef = fieldDef.getMember( "args" );
        if ( argsDef != null )
        {
            if ( !argsDef.isObject() )
            {
                throw new IllegalArgumentException(
                    String.format( "The arguments must be object for field \"%s\" of the type \"%s\"", fieldName, typeName ) );
            }
            argsDef.getKeys().forEach( argName -> {
                ScriptValue argDef = argsDef.getMember( argName );
                if ( !( argDef.getValue() instanceof GraphQLType ) )
                {
                    throw new IllegalArgumentException(
                        String.format( "Incorrect type for the \"%s\" argument for the field \"%s\" of the type \"%s\"", argName, fieldName,
                                       typeName ) );
                }
            } );
        }
    }

    public static void validateUnionTypes( ScriptValue types )
    {
        if ( !types.isObject() )
        {
            throw new IllegalArgumentException( "The \"unions\" field must be object" );
        }
        types.getKeys().forEach( typeName -> {
            ScriptValue typeDef = types.getMember( typeName );
            validateDescription( typeDef, typeName );

            ScriptValue typesMember = typeDef.getMember( "types" );
            if ( typesMember == null || !typesMember.isArray() )
            {
                throw new IllegalArgumentException(
                    String.format( "The \"types\" field for the type \"%s\" must be non null and array", typeName ) );
            }
            typesMember.getList().forEach( type -> {
                if ( !( type instanceof GraphQLType ) )
                {
                    throw new IllegalArgumentException(
                        String.format( "Incorrect type for the \"types\" field for the type \"%s\"", typeName ) );
                }
            } );
        } );
    }

    public static void validateCreationCallbacks( ScriptValue callbacks )
    {
        if ( !callbacks.isObject() )
        {
            throw new IllegalArgumentException( "The \"creationCallbacks\" field must be object" );
        }
        callbacks.getKeys().forEach( callbackName -> {
            ScriptValue callback = callbacks.getMember( callbackName );
            if ( callback == null || !callback.isFunction() )
            {
                throw new IllegalArgumentException( String.format( "Callback for the \"%s\" type must be function", callbackName ) );
            }
        } );
    }

    public static void validateResolvers( ScriptValue resolvers )
    {
        if ( !resolvers.isObject() )
        {
            throw new IllegalArgumentException( "The \"resolvers\" field must be object" );
        }
        resolvers.getKeys().forEach( typeName -> {
            ScriptValue typeFieldsResolver = resolvers.getMember( typeName );
            if ( typeFieldsResolver == null || !typeFieldsResolver.isObject() )
            {
                throw new IllegalArgumentException( String.format( "Field resolver for the \"%s\" type must be object", typeName ) );
            }
            typeFieldsResolver.getKeys().forEach( fieldName -> {
                ScriptValue fieldResolver = typeFieldsResolver.getMember( fieldName );
                if ( !fieldResolver.isFunction() )
                {
                    throw new IllegalArgumentException(
                        String.format( "Field resolver for the \"%s\" type and the \"%s\" field must be function", typeName, fieldName ) );
                }
            } );
        } );
    }

    public static void validateTypeResolvers( ScriptValue typeResolvers )
    {
        if ( !typeResolvers.isObject() )
        {
            throw new IllegalArgumentException( "The \"typeResolvers\" field must be object" );
        }
        typeResolvers.getKeys().forEach( typeName -> {
            ScriptValue typeResolver = typeResolvers.getMember( typeName );
            if ( typeResolver == null || !typeResolver.isFunction() )
            {
                throw new IllegalArgumentException( String.format( "TypeResolver for the \"%s\" type must be function", typeName ) );
            }
        } );
    }
}
