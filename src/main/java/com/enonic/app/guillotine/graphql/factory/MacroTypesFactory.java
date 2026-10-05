package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.FormItemDataFetcher;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.xp.form.Form;

import static com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper.getFilteredFormItems;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class MacroTypesFactory
{
    private static final Logger LOG = LoggerFactory.getLogger( MacroTypesFactory.class );

    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    private final FormItemTypesFactory formItemTypesFactory;

    public MacroTypesFactory( final GuillotineContext context, final ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;
        this.formItemTypesFactory = new FormItemTypesFactory( context, serviceFacade );
    }

    public void create()
    {
        createMacroConfigType();
        createMacroType();
    }

    private void createMacroConfigType()
    {
        List<GraphQLFieldDefinition> macroConfigTypeFields = new ArrayList<>();

        String macroConfigTypeName = context.uniqueName( "MacroConfig" );

        serviceFacade.getComponentDescriptorService().getMacroDescriptors( context.getApplications() ).forEach( macroDescriptor -> {
            context.registerMacroDescriptor( macroDescriptor );

            String descriptorName = StringNormalizer.create( macroDescriptor.getName() );

            String macroTypeName =
                "Macro_" + StringNormalizer.create( macroDescriptor.getKey().getApplicationKey().getName() ) + "_" + descriptorName;

            String macroDataConfigTypeName = context.uniqueName( macroTypeName + "_DataConfig" );

            List<GraphQLFieldDefinition> macroDataConfigFields = new ArrayList<>();
            macroDataConfigFields.add( outputField( "body", Scalars.GraphQLString ) );

            getFilteredFormItems( resolveForm( macroDescriptor.getForm() ) ).forEach( formItem -> {
                final String rawFieldName = formItem.getName();
                try
                {
                    final String fieldName = StringNormalizer.create( rawFieldName );

                    GraphQLOutputType formItemObject =
                        (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( macroDataConfigTypeName, formItem );

                    if ( formItemObject == null )
                    {
                        return;
                    }

                    GraphQLFieldDefinition field =
                        outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

                    context.registerDataFetcher( macroDataConfigTypeName, fieldName,
                                                 new FormItemDataFetcher( formItem, context, serviceFacade ) );

                    macroDataConfigFields.add( field );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to generate GraphQL field for GraphQL type '{}' on formItem with raw name '{}'",
                              macroDataConfigTypeName, rawFieldName, e );
                }
            } );

            GraphQLObjectType macroDataConfigType = newObject( macroDataConfigTypeName, "Macro descriptor data config for application ['" +
                macroDescriptor.getKey().getApplicationKey() + "'] and descriptor ['" + descriptorName + "']", macroDataConfigFields );

            context.registerType( macroDataConfigType.getName(), macroDataConfigType );

            final GraphQLFieldDefinition macroConfigField = outputField( descriptorName, macroDataConfigType );
            macroConfigTypeFields.add( macroConfigField );

            // several applications can define a macro of the same name, and the field is built for one of them:
            // a macro resolved to another descriptor has no config here
            final String descriptorKey = macroDescriptor.getKey().toString();
            context.registerDataFetcher( macroConfigTypeName, macroConfigField.getName(), environment -> {
                if ( !descriptorKey.equals( GuillotineLocalContextHelper.getLocalContext( environment ).get( Constants.MACRO_DESCRIPTOR_FIELD ) ) )
                {
                    return null;
                }
                Map<String, Object> sourceAsMap = CastHelper.cast( environment.getSource() );
                return sourceAsMap.get( macroDescriptor.getName() );
            } );
        } );

        if ( !macroConfigTypeFields.isEmpty() )
        {
            GraphQLObjectType macroConfigType = newObject( macroConfigTypeName, "Macro config type.", macroConfigTypeFields );
            context.registerType( macroConfigType.getName(), macroConfigType );
        }
    }

    private void createMacroType()
    {
        List<GraphQLFieldDefinition> macroTypeFields = new ArrayList<>();

        macroTypeFields.add( outputField( "ref", Scalars.GraphQLString ) );
        macroTypeFields.add( outputField( "name", Scalars.GraphQLString ) );
        macroTypeFields.add( outputField( "descriptor", Scalars.GraphQLString ) );
        if ( context.getOutputType( "MacroConfig" ) != null )
        {
            macroTypeFields.add( outputField( "config", GraphQLTypeReference.typeRef( "MacroConfig" ) ) );
        }

        GraphQLObjectType macroType = newObject( context.uniqueName( "Macro" ), "Macro type.", macroTypeFields );

        context.registerType( macroType.getName(), macroType );

        if ( context.getOutputType( "MacroConfig" ) != null )
        {
            context.registerDataFetcher( macroType.getName(), "config", environment -> {
                final Map<String, Object> sourceAsMap = CastHelper.cast( environment.getSource() );
                final Map<String, Object> localContext = GuillotineLocalContextHelper.newLocalContext( environment );
                localContext.put( Constants.MACRO_DESCRIPTOR_FIELD, sourceAsMap.get( "descriptor" ) );
                return DataFetcherResult.newResult()
                    .data( sourceAsMap.get( "config" ) )
                    .localContext( Collections.unmodifiableMap( localContext ) )
                    .build();
            } );
        }
    }

    private Form resolveForm( Form originalForm )
    {
        Form inlineForm = serviceFacade.getCmsFormFragmentService().inlineFormItems( originalForm );
        return inlineForm != null ? inlineForm : originalForm;
    }
}
