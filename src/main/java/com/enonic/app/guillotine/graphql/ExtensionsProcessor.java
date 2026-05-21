package com.enonic.app.guillotine.graphql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLType;

import com.enonic.app.guillotine.graphql.fetchers.DynamicDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.DynamicTypeResolver;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GraphQLHelper;
import com.enonic.app.guillotine.graphql.transformer.ContextualFieldResolver;
import com.enonic.app.guillotine.graphql.transformer.SchemaExtensions;
import com.enonic.xp.script.ScriptValue;

public class ExtensionsProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger( ExtensionsProcessor.class );

    private final GraphQLTypesRegister typesRegister;

    public ExtensionsProcessor( final GraphQLTypesRegister typesRegister )
    {
        this.typesRegister = typesRegister;
    }

    public void process( final SchemaExtensions extensions )
    {
        processEnumTypes( extensions.getEnums() );
        processInputTypes( extensions.getInputTypes() );
        processInterfacesTypes( extensions.getInterfaces() );
        processOutputTypes( extensions.getTypes() );
        processUnionsTypes( extensions.getUnions() );
        processTypeResolvers( extensions.getTypeResolvers() );
        processResolvers( extensions.getResolvers() );
        processCreationCallbacks( extensions.getCreationCallbacks() );
    }

    private void processEnumTypes( Map<String, ScriptValue> enums )
    {
        try
        {
            enums.forEach( ( typeName, typeDef ) -> {
                try
                {
                    String description = typeDef.getMember( "description" ).getValue( String.class );
                    ScriptValue valuesDefs = typeDef.getMember( "values" );

                    Map<String, Object> values = new LinkedHashMap<>();
                    valuesDefs.getKeys().forEach( key -> values.put( key, valuesDefs.getMember( key ).getValue() ) );

                    typesRegister.addAdditionalType( GraphQLHelper.newEnum( typeName, description, values ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to process GraphQL enum type '{}'", typeName, e );
                }
            } );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "enums", e );
        }
    }

    private void processInputTypes( Map<String, ScriptValue> inputTypes )
    {
        try
        {
            inputTypes.forEach( ( typeName, typeDef ) -> {
                try
                {
                    String description = typeDef.getMember( "description" ).getValue( String.class );
                    ScriptValue fieldDefs = typeDef.getMember( "fields" );

                    List<GraphQLInputObjectField> fieldDefinitions = fieldDefs.getKeys().stream().map(
                        fieldName -> GraphQLHelper.inputField( fieldName,
                                                               CastHelper.cast( fieldDefs.getMember( fieldName ).getValue() ) ) ).collect(
                        Collectors.toList() );

                    typesRegister.addAdditionalType( GraphQLHelper.newInputObject( typeName, description, fieldDefinitions ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to process GraphQL input type '{}'", typeName, e );
                }
            } );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "inputTypes", e );
        }
    }

    private void processOutputTypes( Map<String, ScriptValue> types )
    {
        try
        {
            types.forEach( ( typeName, typeDef ) -> {
                try
                {
                    String description = typeDef.getMember( "description" ).getValue( String.class );
                    ScriptValue interfacesDefs = typeDef.getMember( "interfaces" );
                    ScriptValue fieldDefs = typeDef.getMember( "fields" );

                    List<GraphQLFieldDefinition> fieldDefinitions = extractFields( fieldDefs );
                    List<GraphQLType> interfaces = extractInterfaces( interfacesDefs );

                    typesRegister.addAdditionalType( GraphQLHelper.newObject( typeName, description, interfaces, fieldDefinitions ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to process GraphQL output type '{}'", typeName, e );
                }
            } );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "types", e );
        }
    }

    private void processInterfacesTypes( final Map<String, ScriptValue> interfaces )
    {
        try
        {
            interfaces.forEach( ( typeName, typeDef ) -> {
                try
                {
                    String description = typeDef.getMember( "description" ).getValue( String.class );
                    ScriptValue fieldsDefs = typeDef.getMember( "fields" );
                    List<GraphQLFieldDefinition> fieldDefinitions = extractFields( fieldsDefs );

                    typesRegister.addAdditionalType( GraphQLHelper.newInterface( typeName, description, fieldDefinitions ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to process GraphQL interface type '{}'", typeName, e );
                }
            } );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "interfaces", e );
        }
    }

    private void processUnionsTypes( final Map<String, ScriptValue> unions )
    {
        try
        {
            unions.forEach( ( typeName, typeDef ) -> {
                try
                {
                    String description = typeDef.getMember( "description" ).getValue( String.class );
                    ScriptValue typesDefs = typeDef.getMember( "types" );

                    typesRegister.addAdditionalType(
                        GraphQLHelper.newUnion( typeName, description, CastHelper.cast( typesDefs.getList() ) ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to process GraphQL union type '{}'", typeName, e );
                }
            } );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "unions", e );
        }
    }

    private void processTypeResolvers( final Map<String, ScriptValue> typeResolvers )
    {
        try
        {
            typeResolvers.forEach( ( typeName, typeResolver ) -> {
                try
                {
                    typesRegister.addTypeResolver( typeName, new DynamicTypeResolver( typeResolver ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to register type resolver for GraphQL type '{}'", typeName, e );
                }
            } );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "typeResolvers", e );
        }
    }

    private void processResolvers( final Map<String, Map<String, ContextualFieldResolver>> resolvers )
    {
        try
        {
            resolvers.forEach( ( typeName, typeResolver ) -> {
                Map<String, ContextualFieldResolver> fieldResolvers = resolvers.get( typeName );
                fieldResolvers.forEach( ( fieldName, fieldResolver ) -> {
                    try
                    {
                        typesRegister.addResolver( typeName, fieldName, new DynamicDataFetcher( fieldResolver ) );
                    }
                    catch ( Exception e )
                    {
                        LOG.warn( "Failed to register resolver for GraphQL type '{}' field '{}'", typeName, fieldName, e );
                    }
                } );
            } );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "resolvers", e );
        }
    }

    private void processCreationCallbacks( final Map<String, List<ScriptValue>> creationCallbacksMap )
    {
        try
        {
            creationCallbacksMap.forEach( ( typeName, creationCallbacks ) -> creationCallbacks.forEach( creationCallback -> {
                try
                {
                    OutputObjectCreationCallbackParams params = new OutputObjectCreationCallbackParams();
                    creationCallback.call( params );
                    typesRegister.addCreationCallback( typeName, params );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to apply creation callback for GraphQL type '{}'", typeName, e );
                }
            } ) );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to process GraphQL schema extensions category '{}'", "creationCallbacks", e );
        }
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
}
