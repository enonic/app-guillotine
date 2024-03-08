package com.enonic.app.guillotine.graphql;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import graphql.schema.SchemaTransformer;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.factory.HeadlessCmsTypeFactory;
import com.enonic.app.guillotine.graphql.factory.TypeFactory;
import com.enonic.app.guillotine.graphql.fetchers.DynamicDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GuillotineDataFetcher;
import com.enonic.app.guillotine.graphql.helper.GraphQLHelper;
import com.enonic.app.guillotine.graphql.transformer.ContextualFieldResolver;
import com.enonic.app.guillotine.graphql.transformer.ExtensionGraphQLTypeVisitor;
import com.enonic.app.guillotine.graphql.transformer.ExtensionsExtractorService;
import com.enonic.app.guillotine.graphql.transformer.SchemaExtensions;
import com.enonic.app.guillotine.mapper.ExecutionResultMapper;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.portal.PortalRequest;
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

    private Supplier<PortalRequest> portalRequestSupplier;

	private Supplier<GuillotineConfigService> guillotineConfigServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        this.serviceFacadeSupplier = context.getService( ServiceFacade.class );
        this.applicationServiceSupplier = context.getService( ApplicationService.class );
        this.extensionsExtractorServiceSupplier = context.getService( ExtensionsExtractorService.class );
        this.portalRequestSupplier = context.getBinding( PortalRequest.class );
		this.guillotineConfigServiceSupplier = context.getService( GuillotineConfigService.class );
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
            SchemaTransformer.transformSchema( graphQLSchema, new ExtensionGraphQLTypeVisitor( typesRegister.getCreationCallbacks(), guillotineConfigServiceSupplier.get() ) );

        final GraphQLCodeRegistry codeRegistry = registerDataFetchers( graphQLSchema, typesRegister, schemaExtensions );

        return graphQLSchema.transform( builder -> builder.codeRegistry( codeRegistry ) );
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
                    type -> (GraphQLInterfaceType) type ).collect( Collectors.toList() );

            for ( GraphQLInterfaceType interfaceType : interfaces )
            {
                Map<String, ContextualFieldResolver> interfaceDataFetchers = schemaExtensions.getResolvers().get( interfaceType.getName() );

                List<GraphQLObjectType> implementations = graphQLSchema.getImplementations( interfaceType );

                if ( interfaceDataFetchers != null && implementations != null )
                {
                    implementations.forEach( implementation -> interfaceDataFetchers.forEach( ( fieldName, fieldResolver ) -> {
                        Map<String, ContextualFieldResolver> implementationDataFetchers =
                            schemaExtensions.getResolvers().get( implementation.getName() );

                        if ( ( implementationDataFetchers == null || !implementationDataFetchers.containsKey( fieldName ) ) &&
                            interfaceDataFetchers.containsKey( fieldName ) )
                        {
                            builder.dataFetcher( FieldCoordinates.coordinates( implementation.getName(), fieldName ),
                                                 new DynamicDataFetcher( fieldResolver ) );
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

    private void generateGuillotineApi( GraphQLTypesRegister typesRegister )
    {
        GuillotineContext context =
            GuillotineContext.create().addApplications( getApplicationNames() ).addMacroDecorators( getRegisteredMacrosInSystem() ).build();
        new TypeFactory( context, serviceFacadeSupplier.get() ).createTypes();
        GraphQLObjectType guillotineApi = new HeadlessCmsTypeFactory( context, serviceFacadeSupplier.get() ).create();

		Map<String, Object> guillotineFieldArguments = new HashMap<>();
		guillotineFieldArguments.put( "siteKey", Scalars.GraphQLString );

		Map<String, Object> guillotineFieldOptions = new HashMap<>();
		guillotineFieldOptions.put( "type", guillotineApi );
		guillotineFieldOptions.put( "args", guillotineFieldArguments );

        OutputObjectCreationCallbackParams guillotineQueryCreationCallback = new OutputObjectCreationCallbackParams();
        guillotineQueryCreationCallback.addFields( Map.of( "guillotine", guillotineFieldOptions ) );

        typesRegister.addCreationCallback( "Query", guillotineQueryCreationCallback );

        typesRegister.addResolver( "Query", "guillotine", new GuillotineDataFetcher( portalRequestSupplier ) );

        typesRegister.addAdditionalType( context.getAllTypes() );

        context.getDataFetchers().forEach(
            ( fieldCoordinates, dataFetcher ) -> typesRegister.addResolver( fieldCoordinates.getTypeName(), fieldCoordinates.getFieldName(),
                                                                            dataFetcher ) );

        context.getTypeResolvers().forEach( typesRegister::addTypeResolver );
    }

    public Object execute( GraphQLSchema graphQLSchema, String query, ScriptValue variables )
    {
        GraphQL graphQL = GraphQL.newGraphQL( graphQLSchema ).build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query( query ).variables( extractValue( variables ) ).build();

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
        Map<String, MacroDescriptor> result = new LinkedHashMap<>();
        serviceFacadeSupplier.get().getMacroDescriptorService().getAll().forEach( macroDescriptor -> {
            result.putIfAbsent( macroDescriptor.getName(), macroDescriptor );
        } );
        return result;
    }

}
