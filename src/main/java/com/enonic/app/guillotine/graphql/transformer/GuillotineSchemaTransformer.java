package com.enonic.app.guillotine.graphql.transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.SchemaTransformer;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.DynamicDataFetcher;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GraphQLHelper;
import com.enonic.app.guillotine.mapper.MapMapper;
import com.enonic.xp.script.ScriptValue;

public class GuillotineSchemaTransformer
{
    private final GraphQLSchema schema;

    private final GuillotineContext context;

    private final SchemaExtensions schemaExtensions;

    private GuillotineSchemaTransformer( final Builder builder )
    {
        this.schema = builder.schema;
        this.context = builder.context;
        this.schemaExtensions = builder.schemaExtensions;
    }

    public GraphQLSchema transform()
    {
        GraphQLSchema transformedGraphQLSchema = schema;

        GraphQLCodeRegistry graphQLCodeRegistry = transformedGraphQLSchema.getCodeRegistry();

        transformedGraphQLSchema = transformedGraphQLSchema.transform( schemaBuilder -> {
            if ( schemaExtensions.getEnums() != null )
            {
                addAdditionalEnumTypes( schemaBuilder );
            }
            if ( schemaExtensions.getInputTypes() != null )
            {
                addAdditionalInputTypes( schemaBuilder );
            }
            if ( schemaExtensions.getUnions() != null )
            {
                addAdditionalUnionTypes( schemaBuilder );
            }
            if ( schemaExtensions.getInterfaces() != null )
            {
                addAdditionalInterfacesTypes( schemaBuilder );
            }
            if ( schemaExtensions.getTypes() != null )
            {
                addAdditionalOutputTypes( schemaBuilder );
            }
            addTypesResolvers( schemaBuilder, graphQLCodeRegistry );
        } );

        if ( schemaExtensions.getCreationCallbacks() != null )
        {
            transformedGraphQLSchema =
                SchemaTransformer.transformSchema( transformedGraphQLSchema, new ExtensionGraphQLTypeVisitor( schemaExtensions ) );
        }

        if ( schemaExtensions.getResolvers() != null )
        {
            transformedGraphQLSchema = transformCodeRegistry( transformedGraphQLSchema );
        }

        return transformedGraphQLSchema;
    }

    private void addAdditionalOutputTypes( GraphQLSchema.Builder schemaBuilder )
    {
        validateTypeDefs();

        schemaExtensions.getTypes().forEach( ( typeName, typeDef ) -> {
            if ( typeDef != null && typeDef.isObject() )
            {
                String description = typeDef.getMember( "description" ).getValue( String.class );
                ScriptValue interfacesDefs = typeDef.getMember( "interfaces" );
                ScriptValue fieldDefs = typeDef.getMember( "fields" );

                List<GraphQLFieldDefinition> fieldDefinitions = extractFields( fieldDefs );
                List<GraphQLType> interfaces = extractInterfaces( interfacesDefs );

                schemaBuilder.additionalType(
                    GraphQLHelper.newObject( context.uniqueName( typeName ), description, interfaces, fieldDefinitions ) );
            }
        } );
    }

    private void addAdditionalInputTypes( GraphQLSchema.Builder schemaBuilder )
    {
        schemaExtensions.getInputTypes().forEach( ( typeName, typeDef ) -> {
            if ( typeDef != null && typeDef.isObject() )
            {
                String description = typeDef.getMember( "description" ).getValue( String.class );
                ScriptValue fieldDefs = typeDef.getMember( "fields" );

                List<GraphQLInputObjectField> fieldDefinitions = fieldDefs.getKeys().stream().map(
                    fieldName -> GraphQLHelper.inputField( fieldName,
                                                           CastHelper.cast( fieldDefs.getMember( fieldName ).getValue() ) ) ).collect(
                    Collectors.toList() );

                schemaBuilder.additionalType(
                    GraphQLHelper.newInputObject( context.uniqueName( typeName ), description, fieldDefinitions ) );
            }
        } );
    }

    private void addAdditionalEnumTypes( GraphQLSchema.Builder schemaBuilder )
    {
        schemaExtensions.getEnums().forEach( ( typeName, typeDef ) -> {
            if ( typeDef != null && typeDef.isObject() )
            {
                String description = typeDef.getMember( "description" ).getValue( String.class );
                ScriptValue valuesDefs = typeDef.getMember( "values" );

                Map<String, Object> values = new LinkedHashMap<>();
                valuesDefs.getKeys().forEach( key -> values.put( key, valuesDefs.getMember( key ).getValue() ) );

                schemaBuilder.additionalType( GraphQLHelper.newEnum( typeName, description, values ) );
            }
        } );
    }

    private void addTypesResolvers( GraphQLSchema.Builder schemaBuilder, GraphQLCodeRegistry graphQLCodeRegistry )
    {
        if ( ( schemaExtensions.getInterfaces() != null && !schemaExtensions.getInterfaces().isEmpty() ) ||
            ( schemaExtensions.getUnions() != null && !schemaExtensions.getUnions().isEmpty() ) )
        {
            if ( schemaExtensions.getTypeResolvers() == null || schemaExtensions.getTypeResolvers().isEmpty() )
            {
                throw new IllegalArgumentException( "TypeResolver must be specified for Interface or Union" );
            }

            for ( String typeName : schemaExtensions.getTypeResolvers().keySet() )
            {
                ScriptValue typeResolver = schemaExtensions.getTypeResolvers().get( typeName );

                graphQLCodeRegistry = graphQLCodeRegistry.transform( builder -> builder.typeResolver( typeName, env -> {
                    ScriptValue value = typeResolver.call( new MapMapper( env.getObject() ) );
                    if ( value != null && value.isValue() )
                    {
                        return (GraphQLObjectType) env.getSchema().getType( value.getValue( String.class ) );
                    }
                    return null;
                } ) );

                schemaBuilder.codeRegistry( graphQLCodeRegistry );
            }
        }
    }

    private void addAdditionalUnionTypes( GraphQLSchema.Builder schemaBuilder )
    {
        schemaExtensions.getUnions().forEach( ( typeName, typeDef ) -> {
            if ( typeDef != null && typeDef.isObject() )
            {
                String description = typeDef.getMember( "description" ).getValue( String.class );
                ScriptValue typesDefs = typeDef.getMember( "types" );

                if ( typesDefs == null || !typesDefs.isArray() || typesDefs.getArray().isEmpty() )
                {
                    throw new IllegalArgumentException( "Value 'types' is required and cannot be empty" );
                }

                ScriptValue typeResolver = Objects.requireNonNull( schemaExtensions.getTypeResolvers() ).get( typeName );
                if ( typeResolver == null )
                {
                    throw new IllegalArgumentException( "TypeResolver for " + typeName + " must be set." );
                }

                schemaBuilder.additionalType( GraphQLHelper.newUnion( typeName, description, CastHelper.cast( typesDefs.getList() ) ) );
            }
        } );
    }

    private void addAdditionalInterfacesTypes( GraphQLSchema.Builder schemaBuilder )
    {
        schemaExtensions.getInterfaces().forEach( ( typeName, typeDef ) -> {
            if ( typeDef != null && typeDef.isObject() )
            {
                String description = typeDef.getMember( "description" ).getValue( String.class );
                ScriptValue fieldsDefs = typeDef.getMember( "fields" );

                List<GraphQLFieldDefinition> fieldDefinitions = extractFields( fieldsDefs );

                ScriptValue typeResolver = Objects.requireNonNull( schemaExtensions.getTypeResolvers() ).get( typeName );
                if ( typeResolver == null )
                {
                    throw new IllegalArgumentException( "TypeResolver for " + typeName + " must be set." );
                }

                schemaBuilder.additionalType( GraphQLHelper.newInterface( typeName, description, fieldDefinitions ) );
            }
        } );
    }

    private void validateTypeDefs()
    {
        schemaExtensions.getTypes().forEach( this::validateTypeDef );
    }

    private void validateTypeDef( String typeName, ScriptValue typeDef )
    {
        ScriptValue fields =
            Objects.requireNonNull( typeDef.getMember( "fields" ), String.format( "The fields must be set for type \"%s\"", typeName ) );

        if ( !fields.isObject() )
        {
            throw new IllegalArgumentException( String.format( "The \"fields\" field must be object for type \"%s\"", typeName ) );
        }

        fields.getKeys().forEach( fieldName -> {
            ScriptValue fieldDef = fields.getMember( fieldName );
            validateFieldDef( fieldDef, typeName, fieldName );
        } );

        if ( typeDef.getMember( "interfaces" ) != null && !typeDef.getMember( "interfaces" ).isArray() )
        {
            throw new IllegalArgumentException( String.format( "The \"interfaces\" field must be arrays for type \"%s\"", typeName ) );
        }
    }

    private void validateFieldDef( ScriptValue fieldDef, String typeName, String fieldName )
    {
        Objects.requireNonNull( fieldDef.getMember( "type" ),
                                String.format( "The \"type\" must be specified for field \"%s\" of the type \"%s\"", fieldName,
                                               typeName ) );
    }

    private List<GraphQLFieldDefinition> extractFields( ScriptValue fieldDefs )
    {
        return fieldDefs.getKeys().stream().map( fieldName -> {
            ScriptValue fieldDef = fieldDefs.getMember( fieldName );

            return GraphQLHelper.outputField( fieldName, CastHelper.cast( fieldDef.getMember( "type" ).getValue() ),
                                              extractArguments( fieldDef.getMember( "args" ) ) );
        } ).collect( Collectors.toList() );
    }

    private List<GraphQLType> extractInterfaces( ScriptValue interfaceDefs )
    {
        if ( interfaceDefs != null )
        {
            return interfaceDefs.getList().stream().map( type -> (GraphQLType) type ).collect( Collectors.toList() );
        }
        return null;
    }

    private List<GraphQLArgument> extractArguments( ScriptValue argsDefs )
    {
        if ( argsDefs != null && argsDefs.isObject() )
        {
            return argsDefs.getKeys().stream().map(
                argName -> GraphQLHelper.newArgument( argName, CastHelper.cast( argsDefs.getMember( argName ).getValue() ) ) ).collect(
                Collectors.toList() );
        }
        return null;
    }

    private GraphQLSchema transformCodeRegistry( GraphQLSchema transformedGraphQLSchema )
    {
        Set<String> interfaceTypes = new HashSet<>();
        interfaceTypes.addAll( schemaExtensions.getCreationCallbacks().keySet() );
        interfaceTypes.addAll( schemaExtensions.getInterfaces().keySet() );

        Map<String, GraphQLInterfaceType> interfaces = new HashMap<>();
        for ( String typeName : interfaceTypes )
        {
            GraphQLType type = transformedGraphQLSchema.getType( typeName );
            if ( type instanceof GraphQLInterfaceType )
            {
                interfaces.put( typeName, (GraphQLInterfaceType) type );
            }
        }

        Map<String, List<GraphQLObjectType>> implementations = new LinkedHashMap<>();
        for ( GraphQLInterfaceType interfaceType : interfaces.values() )
        {
            implementations.put( interfaceType.getName(), transformedGraphQLSchema.getImplementations( interfaceType ) );
        }

        Set<String> processedTypes = new HashSet<>();

        GraphQLCodeRegistry codeRegistry = transformedGraphQLSchema.getCodeRegistry().transform( builder -> {
            for ( String interfaceTypeName : interfaces.keySet() )
            {
                Map<String, ScriptValue> fieldResolvers = schemaExtensions.getResolvers().get( interfaceTypeName );
                if ( fieldResolvers != null )
                {
                    fieldResolvers.forEach(
                        ( fieldName, fieldResolver ) -> builder.dataFetcher( FieldCoordinates.coordinates( interfaceTypeName, fieldName ),
                                                                             new DynamicDataFetcher( fieldResolver ) ) );

                    if ( implementations.get( interfaceTypeName ) != null )
                    {
                        implementations.get( interfaceTypeName ).forEach( implementation -> fieldResolvers.forEach(
                            ( fieldName, fieldResolver ) -> builder.dataFetcher(
                                FieldCoordinates.coordinates( implementation.getName(), fieldName ),
                                new DynamicDataFetcher( fieldResolver ) ) ) );
                    }
                }

                processedTypes.add( interfaceTypeName );
            }

            for ( String typeName : schemaExtensions.getResolvers().keySet() )
            {
                if ( !processedTypes.contains( typeName ) )
                {
                    Map<String, ScriptValue> fieldResolvers = schemaExtensions.getResolvers().get( typeName );
                    if ( fieldResolvers != null )
                    {
                        fieldResolvers.forEach(
                            ( fieldName, fieldResolver ) -> builder.dataFetcher( FieldCoordinates.coordinates( typeName, fieldName ),
                                                                                 new DynamicDataFetcher( fieldResolver ) ) );
                    }
                }
            }
        } );

        return transformedGraphQLSchema.transform( builder -> builder.codeRegistry( codeRegistry ) );
    }

    public static Builder create( GraphQLSchema schema, GuillotineContext context )
    {
        return new Builder( schema, context );
    }

    public static class Builder
    {
        private final GraphQLSchema schema;

        private final GuillotineContext context;

        private SchemaExtensions schemaExtensions;


        private Builder( final GraphQLSchema schema, final GuillotineContext context )
        {
            this.schema = Objects.requireNonNull( schema );
            this.context = Objects.requireNonNull( context );
        }

        public Builder setSchemaExtensions( final SchemaExtensions schemaExtensions )
        {
            this.schemaExtensions = schemaExtensions;
            return this;
        }

        public GuillotineSchemaTransformer build()
        {
            return new GuillotineSchemaTransformer( this );
        }
    }
}
