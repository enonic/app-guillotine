package com.enonic.app.guillotine.graphql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.SchemaTransformer;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.factory.HeadlessCmsTypeFactory;
import com.enonic.app.guillotine.graphql.factory.TypeFactory;
import com.enonic.app.guillotine.graphql.helper.GraphQLHelper;
import com.enonic.app.guillotine.graphql.transformer.ExtensionGraphQLTypeVisitor;
import com.enonic.app.guillotine.graphql.transformer.ExtensionsExtractorService;
import com.enonic.app.guillotine.graphql.transformer.SchemaExtensions;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class GraphQLApi
    implements ScriptBean
{
    private Supplier<ServiceFacade> serviceFacadeSupplier;

    private Supplier<ApplicationService> applicationServiceSupplier;

    private Supplier<ExtensionsExtractorService> extensionsExtractorServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.serviceFacadeSupplier = context.getService( ServiceFacade.class );
        this.applicationServiceSupplier = context.getService( ApplicationService.class );
        this.extensionsExtractorServiceSupplier = context.getService( ExtensionsExtractorService.class );
    }

    public GraphQLSchema createSchema()
    {
        // Extract the extensions from the applications
        SchemaExtensions schemaExtensions = extensionsExtractorServiceSupplier.get().extractSchemaExtensions();

        // Temporary solution to get the extensions from the applications
        GraphQLSchema graphQLSchema = createBaseGraphQLSchema();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();

        // Generate the Guillotine types
        generateGuillotineApi( typesRegister );

        ExtensionsProcessor extensionsProcessor = new ExtensionsProcessor( typesRegister );
        extensionsProcessor.process( schemaExtensions );

        OutputObjectCreationCallbackParams queryCreationCallback = new OutputObjectCreationCallbackParams();
        queryCreationCallback.removeFields( List.of( "_TEMP_FIELD_" ) );
        typesRegister.addCreationCallback( "Query", queryCreationCallback );

        final GraphQLCodeRegistry transformedCodeRegistry = registerTypeResolvers( graphQLSchema, typesRegister );

        graphQLSchema = graphQLSchema.transform( builder -> {
            builder.additionalTypes( typesRegister.getAdditionalTypes() );
            builder.codeRegistry( transformedCodeRegistry );
        } );

        graphQLSchema =
            SchemaTransformer.transformSchema( graphQLSchema, new ExtensionGraphQLTypeVisitor( typesRegister.getCreationCallbacks() ) );

        final GraphQLCodeRegistry codeRegistry = registerDataFetchers( graphQLSchema, typesRegister );

        return graphQLSchema.transform( builder -> builder.codeRegistry( codeRegistry ) );
    }

    private GraphQLCodeRegistry registerTypeResolvers( GraphQLSchema graphQLSchema, GraphQLTypesRegister typesRegister )
    {
        return graphQLSchema.getCodeRegistry().transform( builder -> typesRegister.getTypeResolvers().forEach( builder::typeResolver ) );
    }

    private GraphQLCodeRegistry registerDataFetchers( GraphQLSchema graphQLSchema, GraphQLTypesRegister typesRegister )
    {
        return graphQLSchema.getCodeRegistry().transform( builder -> {

            Map<String, GraphQLInterfaceType> interfaces = new HashMap<>();
            for ( GraphQLType type : typesRegister.getAdditionalTypes() )
            {
                if ( type instanceof GraphQLInterfaceType )
                {
                    GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) type;
                    interfaces.put( interfaceType.getName(), interfaceType );
                }
            }

            Map<String, List<GraphQLObjectType>> implementations = new LinkedHashMap<>();
            for ( GraphQLInterfaceType interfaceType : interfaces.values() )
            {
                implementations.put( interfaceType.getName(), graphQLSchema.getImplementations( interfaceType ) );
            }

            Set<String> processedTypes = new HashSet<>();

            for ( String interfaceTypeName : interfaces.keySet() )
            {
                Map<String, DataFetcher<?>> fieldResolvers = typesRegister.getResolvers().get( interfaceTypeName );
                if ( fieldResolvers != null )
                {
                    fieldResolvers.forEach(
                        ( fieldName, dataFetcher ) -> builder.dataFetcher( FieldCoordinates.coordinates( interfaceTypeName, fieldName ),
                                                                           dataFetcher ) );

                    if ( implementations.get( interfaceTypeName ) != null )
                    {
                        implementations.get( interfaceTypeName ).forEach( implementation -> fieldResolvers.forEach(
                            ( fieldName, fieldResolver ) -> builder.dataFetcher(
                                FieldCoordinates.coordinates( implementation.getName(), fieldName ), fieldResolvers.get( fieldName ) ) ) );
                    }
                }

                processedTypes.add( interfaceTypeName );
            }

            for ( String typeName : typesRegister.getResolvers().keySet() )
            {
                if ( !processedTypes.contains( typeName ) )
                {
                    Map<String, DataFetcher<?>> fieldResolvers = typesRegister.getResolvers().get( typeName );
                    if ( fieldResolvers != null )
                    {
                        fieldResolvers.forEach(
                            ( fieldName, dataFetcher ) -> builder.dataFetcher( FieldCoordinates.coordinates( typeName, fieldName ),
                                                                               dataFetcher ) );
                    }
                }
            }
        } );

    }

    private GraphQLSchema createBaseGraphQLSchema()
    {
        GraphQLObjectType queryType =
            GraphQLHelper.newObject( "Query", "Query", List.of( outputField( "_TEMP_FIELD_", Scalars.GraphQLString ) ) );

        GraphQLSchema.Builder graphQLSchema = GraphQLSchema.newSchema();
        graphQLSchema.query( queryType );

        return graphQLSchema.build();
    }

    private void generateGuillotineApi( GraphQLTypesRegister typesRegister )
    {
        GuillotineContext context =
            GuillotineContext.create().addApplications( getApplicationNames() ).addMacroDecorators( getRegisteredMacrosInSystem() ).build();
        new TypeFactory( context, serviceFacadeSupplier.get() ).createTypes();
        GraphQLObjectType guillotineApi = new HeadlessCmsTypeFactory( context, serviceFacadeSupplier.get() ).create();

        Map<String, Object> guillotineFieldArguments = new HashMap<>();
        guillotineFieldArguments.put( "repo", Scalars.GraphQLString );
        guillotineFieldArguments.put( "branch", Scalars.GraphQLString );
        guillotineFieldArguments.put( "siteKey", Scalars.GraphQLString );

        Map<String, Object> guillotineFieldOptions = new HashMap<>();
        guillotineFieldOptions.put( "type", guillotineApi );
        guillotineFieldOptions.put( "args", guillotineFieldArguments );

        OutputObjectCreationCallbackParams guillotineQueryCreationCallback = new OutputObjectCreationCallbackParams();
        guillotineQueryCreationCallback.addFields( Map.of( "guillotine", guillotineFieldOptions ) );

        typesRegister.addCreationCallback( "Query", guillotineQueryCreationCallback );

        typesRegister.addResolver( "Query", "guillotine", environment -> {
            final Map<String, Object> localContext = environment.getLocalContext();

            localContext.put( Constants.GUILLOTINE_TARGET_REPO_CTX, environment.getArgument( "repo" ) );
            localContext.put( Constants.GUILLOTINE_TARGET_BRANCH_CTX, environment.getArgument( "branch" ) );
            localContext.put( Constants.GUILLOTINE_TARGET_SITE_CTX, environment.getArgument( "siteKey" ) );

            return new Object();
        } );

        typesRegister.addAdditionalType( context.getAllTypes() );

        context.getDataFetchers().forEach(
            ( fieldCoordinates, dataFetcher ) -> typesRegister.addResolver( fieldCoordinates.getTypeName(), fieldCoordinates.getFieldName(),
                                                                            dataFetcher ) );

        context.getTypeResolvers().forEach( typesRegister::addTypeResolver );
    }

    public Object execute( GraphQLSchema graphQLSchema, String query, ScriptValue variables )
    {
        GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).build();

        ExecutionInput executionInput =
            ExecutionInput.newExecutionInput().query( query ).variables( extractValue( variables ) ).localContext(
                new HashMap<String, Object>() ).build();

        return new ExecutionResultMapper( graphQL.execute( executionInput ) );
    }

    private Map<String, Object> extractValue( ScriptValue scriptValue )
    {
        return scriptValue == null ? new HashMap<>() : scriptValue.getMap();
    }

    private List<String> getApplicationNames()
    {
        return applicationServiceSupplier.get().getInstalledApplications().stream().map(
            application -> application.getKey().getName() ).collect( Collectors.toList() );
    }

    private Map<String, MacroDescriptor> getRegisteredMacrosInSystem()
    {
        return serviceFacadeSupplier.get().getMacroDescriptorService().getAll().stream().collect(
            Collectors.toMap( MacroDescriptor::getName, Function.identity() ) );
    }

}
