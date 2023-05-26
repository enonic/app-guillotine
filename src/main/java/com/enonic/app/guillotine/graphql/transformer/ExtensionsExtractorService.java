package com.enonic.app.guillotine.graphql.transformer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.app.guillotine.graphql.GraphQLApi;
import com.enonic.app.guillotine.mapper.GraphQLMapper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.ScriptExports;
import com.enonic.xp.script.ScriptValue;

@Component(service = ExtensionsExtractorService.class)
public class ExtensionsExtractorService
{
    private static final Logger LOG = LoggerFactory.getLogger( GraphQLApi.class );

    private static final String SCRIPT_PATH = "guillotine/guillotine.js";

    private static final String EXTENSIONS_METHOD_NAME = "extensions";

    private static final String INPUT_TYPES_NODE = "inputTypes";

    private static final String ENUMS_NODE = "enums";

    private static final String INTERFACES_NODE = "interfaces";

    private static final String UNIONS_NODE = "unions";

    private static final String TYPES_NODE = "types";

    private static final String CREATION_CALLBACKS_NODE = "creationCallbacks";

    private static final String RESOLVERS_NODE = "resolvers";

    private static final String TYPE_RESOLVERS_NODE = "typeResolvers";

    private final ApplicationService applicationService;

    private final ResourceService resourceService;

    private final PortalScriptService portalScriptService;

    @Activate
    public ExtensionsExtractorService( final @Reference ApplicationService applicationService,
                                       final @Reference ResourceService resourceService,
                                       final @Reference PortalScriptService portalScriptService )
    {
        this.applicationService = applicationService;
        this.resourceService = resourceService;
        this.portalScriptService = portalScriptService;
    }

    public SchemaExtensions extractSchemaExtensions()
    {
        SchemaExtensions.Builder schemaExtensionsBuilder = SchemaExtensions.create();

        applicationService.getInstalledApplications().forEach( application -> {
            ScriptValue extensions = executeMethod( application.getKey(), getGraphQLObject() );

            if ( extensions != null && extensions.isObject() )
            {
                ScriptValue types = extensions.getMember( TYPES_NODE );
                ScriptValue inputTypes = extensions.getMember( INPUT_TYPES_NODE );
                ScriptValue resolvers = extensions.getMember( RESOLVERS_NODE );
                ScriptValue enums = extensions.getMember( ENUMS_NODE );
                ScriptValue unions = extensions.getMember( UNIONS_NODE );
                ScriptValue interfaces = extensions.getMember( INTERFACES_NODE );
                ScriptValue creationCallbacks = extensions.getMember( CREATION_CALLBACKS_NODE );
                ScriptValue typeResolvers = extensions.getMember( TYPE_RESOLVERS_NODE );

                extractTypes( schemaExtensionsBuilder, types );
                extractInputTypes( schemaExtensionsBuilder, inputTypes );
                extractEnums( schemaExtensionsBuilder, enums );
                extractUnions( schemaExtensionsBuilder, unions );
                extractInterfaces( schemaExtensionsBuilder, interfaces );
                extractResolvers( schemaExtensionsBuilder, resolvers );
                extractTypeResolvers( schemaExtensionsBuilder, typeResolvers );
                extractCreationCallbacks( schemaExtensionsBuilder, creationCallbacks );
            }
        } );

        return schemaExtensionsBuilder.build();
    }

    private static void extractCreationCallbacks( final SchemaExtensions.Builder schemaExtensionsBuilder,
                                                  final ScriptValue creationCallbacks )
    {
        if ( creationCallbacks != null && creationCallbacks.isObject() )
        {
            creationCallbacks.getKeys().forEach( typeName -> {
                ScriptValue creationCallbackFn = creationCallbacks.getMember( typeName );
                if ( creationCallbackFn != null && creationCallbackFn.isFunction() )
                {
                    schemaExtensionsBuilder.addCreationCallback( typeName, creationCallbackFn );
                }
            } );
        }
    }

    private static void extractTypeResolvers( final SchemaExtensions.Builder schemaExtensionsBuilder, final ScriptValue typeResolvers )
    {
        if ( typeResolvers != null && typeResolvers.isObject() )
        {
            typeResolvers.getKeys().forEach( typeName -> {
                ScriptValue typeResolverDef = typeResolvers.getMember( typeName );
                schemaExtensionsBuilder.addTypeResolver( typeName, typeResolverDef );
            } );
        }
    }

    private static void extractResolvers( final SchemaExtensions.Builder schemaExtensionsBuilder, final ScriptValue resolvers )
    {
        if ( resolvers != null && resolvers.isObject() )
        {
            resolvers.getKeys().forEach( typeName -> {
                ScriptValue typeResolverDef = resolvers.getMember( typeName );
                if ( typeResolverDef != null && typeResolverDef.isObject() )
                {
                    typeResolverDef.getKeys().forEach( fieldName -> {
                        ScriptValue resolverDef = typeResolverDef.getMember( fieldName );
                        schemaExtensionsBuilder.addResolver( typeName, fieldName, resolverDef );
                    } );
                }
            } );
        }
    }

    private static void extractInterfaces( final SchemaExtensions.Builder schemaExtensionsBuilder, final ScriptValue interfaces )
    {
        if ( interfaces != null && interfaces.isObject() )
        {
            interfaces.getKeys().forEach( typeName -> schemaExtensionsBuilder.addInterface( typeName, interfaces.getMember( typeName ) ) );
        }
    }

    private static void extractUnions( final SchemaExtensions.Builder schemaExtensionsBuilder, final ScriptValue unions )
    {
        if ( unions != null && unions.isObject() )
        {
            unions.getKeys().forEach( typeName -> schemaExtensionsBuilder.addUnion( typeName, unions.getMember( typeName ) ) );
        }
    }

    private static void extractEnums( final SchemaExtensions.Builder schemaExtensionsBuilder, final ScriptValue enums )
    {
        if ( enums != null && enums.isObject() )
        {
            enums.getKeys().forEach( typeName -> schemaExtensionsBuilder.addEnum( typeName, enums.getMember( typeName ) ) );
        }
    }

    private static void extractInputTypes( final SchemaExtensions.Builder schemaExtensionsBuilder, final ScriptValue inputTypes )
    {
        if ( inputTypes != null && inputTypes.isObject() )
        {
            inputTypes.getKeys().forEach( typeName -> schemaExtensionsBuilder.addInputType( typeName, inputTypes.getMember( typeName ) ) );
        }
    }

    private static void extractTypes( final SchemaExtensions.Builder schemaExtensionsBuilder, final ScriptValue types )
    {
        if ( types != null && types.isObject() )
        {
            types.getKeys().forEach( typeName -> schemaExtensionsBuilder.addType( typeName, types.getMember( typeName ) ) );
        }
    }

    private Object getGraphQLObject()
    {
        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new GraphQLMapper().serialize( generator );
        return generator.getRoot();
    }

    private ScriptValue executeMethod( ApplicationKey applicationKey, Object graphQL )
    {
        ResourceKey resourceKey = ResourceKey.from( applicationKey, SCRIPT_PATH );
        if ( resourceService.getResource( resourceKey ).exists() )
        {
            ScriptExports scriptExports = portalScriptService.execute( resourceKey );
            if ( scriptExports != null )
            {
                if ( scriptExports.hasMethod( EXTENSIONS_METHOD_NAME ) )
                {
                    try
                    {
                        return scriptExports.executeMethod( EXTENSIONS_METHOD_NAME, graphQL );
                    }
                    catch ( Exception e )
                    {
                        LOG.warn( "{} function can not be extracted from {}", EXTENSIONS_METHOD_NAME, SCRIPT_PATH, e );
                    }
                }
            }
        }
        return null;
    }
}
