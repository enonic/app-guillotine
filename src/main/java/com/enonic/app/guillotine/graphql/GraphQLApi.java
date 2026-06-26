package com.enonic.app.guillotine.graphql;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.ParseAndValidate;
import graphql.Scalars;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.language.Document;
import graphql.language.SourceLocation;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.SchemaTransformer;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.factory.HeadlessCmsTypeFactory;
import com.enonic.app.guillotine.graphql.factory.TypeFactory;
import com.enonic.app.guillotine.graphql.fetchers.ContentAwareDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.DynamicDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GuillotineDataFetcher;
import com.enonic.app.guillotine.graphql.helper.GraphQLHelper;
import com.enonic.app.guillotine.graphql.helper.GraphQLTypeChecker;
import com.enonic.app.guillotine.graphql.transformer.ExtensionGraphQLTypeVisitor;
import com.enonic.app.guillotine.graphql.transformer.ExtensionsExtractorService;
import com.enonic.app.guillotine.graphql.transformer.SchemaExtensions;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.script.ScriptValue;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class GraphQLApi
{
    private Supplier<ServiceFacade> serviceFacadeSupplier;

    private Supplier<ApplicationService> applicationServiceSupplier;

    private Supplier<ExtensionsExtractorService> extensionsExtractorServiceSupplier;

    private Supplier<GuillotineConfigService> guillotineConfigServiceSupplier;

    private static final Parser PARSER = new Parser();

    public void initialize( final Supplier<ServiceFacade> serviceFacadeSupplier,
                            final Supplier<ApplicationService> applicationServiceSupplier,
                            final Supplier<ExtensionsExtractorService> extensionsExtractorServiceSupplier,
                            final Supplier<GuillotineConfigService> guillotineConfigServiceSupplier )
    {
        this.serviceFacadeSupplier = serviceFacadeSupplier;
        this.applicationServiceSupplier = applicationServiceSupplier;
        this.extensionsExtractorServiceSupplier = extensionsExtractorServiceSupplier;
        this.guillotineConfigServiceSupplier = guillotineConfigServiceSupplier;
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

        graphQLSchema = SchemaTransformer.transformSchema( graphQLSchema,
                                                           new ExtensionGraphQLTypeVisitor( typesRegister.getCreationCallbacks(),
                                                                                            guillotineConfigServiceSupplier.get() ) );

        final GraphQLCodeRegistry codeRegistry = registerDataFetchers( graphQLSchema, typesRegister, schemaExtensions );

        graphQLSchema = graphQLSchema.transform( builder -> builder.codeRegistry( codeRegistry ) );

        return wrapContentDataFetchers( graphQLSchema );
    }

    private GraphQLCodeRegistry registerTypeResolvers( GraphQLSchema graphQLSchema, GraphQLTypesRegister typesRegister )
    {
        return graphQLSchema.getCodeRegistry().transform( builder -> typesRegister.getTypeResolvers().forEach( builder::typeResolver ) );
    }

    private GraphQLCodeRegistry registerDataFetchers( GraphQLSchema graphQLSchema, GraphQLTypesRegister typesRegister,
                                                      SchemaExtensions schemaExtensions )
    {
        return graphQLSchema.getCodeRegistry().transform( builder -> {
            // register the data fetchers for all defined types
            for ( String typeName : typesRegister.getResolvers().keySet() )
            {
                Map<String, DataFetcher<?>> fieldResolvers = typesRegister.getResolvers().get( typeName );
                if ( fieldResolvers != null )
                {
                    fieldResolvers.forEach(
                        ( fieldName, dataFetcher ) -> builder.dataFetcher( FieldCoordinates.coordinates( typeName, fieldName ),
                                                                           dataFetcher ) );
                }
            }

            // If the dataFetcher has been overridden for an interface, it should be overridden for all implementations,
            // except when it has also been overridden for a specific implementation.
            List<GraphQLInterfaceType> interfaces =
                typesRegister.getAdditionalTypes().stream().filter( type -> type instanceof GraphQLInterfaceType ).map(
                    type -> (GraphQLInterfaceType) type ).toList();

            for ( GraphQLInterfaceType interfaceType : interfaces )
            {
                Map<String, ScriptValue> interfaceDataFetchers = schemaExtensions.getResolvers().get( interfaceType.getName() );

                List<GraphQLObjectType> implementations = graphQLSchema.getImplementations( interfaceType );

                if ( interfaceDataFetchers != null && implementations != null )
                {
                    implementations.forEach( implementation -> interfaceDataFetchers.forEach( ( fieldName, resolverDef ) -> {
                        Map<String, ScriptValue> implementationDataFetchers =
                            schemaExtensions.getResolvers().get( implementation.getName() );

                        if ( ( implementationDataFetchers == null || !implementationDataFetchers.containsKey( fieldName ) ) &&
                            interfaceDataFetchers.containsKey( fieldName ) )
                        {
                            builder.dataFetcher( FieldCoordinates.coordinates( implementation.getName(), fieldName ),
                                                 new DynamicDataFetcher( resolverDef ) );
                        }
                    } ) );
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

    private GraphQLSchema wrapContentDataFetchers( final GraphQLSchema graphQLSchema )
    {
        final GraphQLCodeRegistry.Builder newCodeRegistry = GraphQLCodeRegistry.newCodeRegistry( graphQLSchema.getCodeRegistry() );

        final List<GraphQLObjectType> outputGraphQLTypes =
            graphQLSchema.getAllTypesAsList().stream().filter( t -> t instanceof GraphQLObjectType ).map(
                t -> (GraphQLObjectType) t ).toList();

        for ( GraphQLObjectType objectType : outputGraphQLTypes )
        {
            for ( GraphQLFieldDefinition fieldDefinition : objectType.getFieldDefinitions() )
            {
                final GraphQLOutputType outputType = fieldDefinition.getType();

                if ( GraphQLTypeChecker.isContentType( outputType ) )
                {
                    final DataFetcher<?> originalFetcher = graphQLSchema.getCodeRegistry().getDataFetcher( objectType, fieldDefinition );
                    final DataFetcher<?> wrappedFetcher = new ContentAwareDataFetcher( originalFetcher );
                    newCodeRegistry.dataFetcher( FieldCoordinates.coordinates( objectType.getName(), fieldDefinition.getName() ),
                                                 wrappedFetcher );
                }
            }
        }

        return GraphQLSchema.newSchema( graphQLSchema ).codeRegistry( newCodeRegistry.build() ).build();
    }

    private void generateGuillotineApi( GraphQLTypesRegister typesRegister )
    {
        GuillotineContext context =
            GuillotineContext.create().addApplications( getApplicationNames() ).addMacroDecorators( getRegisteredMacrosInSystem() ).build();
        new TypeFactory( context, serviceFacadeSupplier.get() ).createTypes();
        GraphQLObjectType guillotineApi = new HeadlessCmsTypeFactory( context, serviceFacadeSupplier.get() ).create();

        Map<String, Object> guillotineFieldArguments = new HashMap<>();
        guillotineFieldArguments.put( "project", Scalars.GraphQLString );
        guillotineFieldArguments.put( "branch", Scalars.GraphQLString );
        guillotineFieldArguments.put( "siteKey", Scalars.GraphQLString );
        guillotineFieldArguments.put( "pageBaseUrl", Scalars.GraphQLString );

        Map<String, Object> guillotineFieldOptions = new HashMap<>();
        guillotineFieldOptions.put( "type", guillotineApi );
        guillotineFieldOptions.put( "args", guillotineFieldArguments );

        OutputObjectCreationCallbackParams guillotineQueryCreationCallback = new OutputObjectCreationCallbackParams();
        guillotineQueryCreationCallback.addFields( Map.of( "guillotine", guillotineFieldOptions ) );

        typesRegister.addCreationCallback( "Query", guillotineQueryCreationCallback );

        typesRegister.addResolver( "Query", "guillotine",
                                   new GuillotineDataFetcher( serviceFacadeSupplier, guillotineConfigServiceSupplier ) );

        typesRegister.addAdditionalType( context.getAllTypes() );

        context.getDataFetchers().forEach(
            ( fieldCoordinates, dataFetcher ) -> typesRegister.addResolver( fieldCoordinates.getTypeName(), fieldCoordinates.getFieldName(),
                                                                            dataFetcher ) );

        context.getTypeResolvers().forEach( typesRegister::addTypeResolver );
    }

    public Object execute( GraphQLSchema graphQLSchema, String query, Map<String, Object> variables )
    {
        return new ExecutionResultMapper( executeInternal( graphQLSchema, query, variables == null ? Map.of() : variables ) );
    }

    public Map<String, Object> executeToSpecification( GraphQLSchema graphQLSchema, String query, Map<String, Object> variables )
    {
        return executeInternal( graphQLSchema, query, variables == null ? Map.of() : variables ).toSpecification();
    }

    private ExecutionResult executeInternal( GraphQLSchema graphQLSchema, String query, Map<String, Object> variables )
    {
        final PreparsedDocumentProvider preparsedProvider = ( executionInput, parseAndValidateFunction ) -> {
            PreparsedDocumentEntry entry;
            try
            {
                int maxQueryTokens = guillotineConfigServiceSupplier.get().getMaxQueryTokens();
                ParserOptions parserOptions = ParserOptions.newParserOptions().maxTokens( maxQueryTokens ).build();

                Document doc = PARSER.parseDocument(
                    ParserEnvironment.newParserEnvironment().document( executionInput.getQuery() ).parserOptions( parserOptions ).build() );

                List<ValidationError> errors = ParseAndValidate.validate( graphQLSchema, doc );
                entry = errors.isEmpty() ? new PreparsedDocumentEntry( doc ) : new PreparsedDocumentEntry( errors );
            }
            catch ( InvalidSyntaxException e )
            {
                entry = new PreparsedDocumentEntry( List.of(
                    ValidationError.newValidationError().validationErrorType( ValidationErrorType.InvalidSyntax ).sourceLocations(
                        List.of( new SourceLocation( e.getLocation().getLine(), e.getLocation().getColumn() ) ) ).description(
                        e.getMessage() ).build() ) );
            }

            return CompletableFuture.completedFuture( entry );
        };

        final GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).preparsedDocumentProvider( preparsedProvider ).build();

        final ExecutionInput executionInput = ExecutionInput.newExecutionInput().query( query ).variables( variables ).build();

        return graphQL.execute( executionInput );
    }

    private List<String> getApplicationNames()
    {
        return applicationServiceSupplier.get().getInstalledApplications().stream().map(
            application -> application.getKey().getName() ).collect( Collectors.toList() );
    }

    private Map<String, MacroDescriptor> getRegisteredMacrosInSystem()
    {
        Map<String, MacroDescriptor> result = new LinkedHashMap<>();
        serviceFacadeSupplier.get().getMacroDescriptorService().getAll().forEach( macroDescriptor -> {
            result.putIfAbsent( macroDescriptor.getName(), macroDescriptor );
        } );
        return result;
    }

}
