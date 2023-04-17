package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import static com.enonic.app.guillotine.graphql.FormItemTypesHelper.getFilteredFormItems;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;

public class MacroTypesFactory
{
    private final GuillotineContext guillotineContext;

    private final ServiceFacade serviceFacade;

    private final FormItemTypesFactory formItemTypesFactory;

    public MacroTypesFactory( final GuillotineContext guillotineContext, final ServiceFacade serviceFacade )
    {
        this.guillotineContext = guillotineContext;
        this.serviceFacade = serviceFacade;
        this.formItemTypesFactory = new FormItemTypesFactory( guillotineContext, serviceFacade );
    }

    public void create()
    {
        createMacroConfigType();
        createMacroType();
    }

    private void createMacroConfigType()
    {
        List<GraphQLFieldDefinition> macroConfigTypeFields = new ArrayList<>();

        serviceFacade.getComponentDescriptorService().getMacroDescriptors( guillotineContext.getApplications() ).forEach(
            macroDescriptor -> {
                String descriptorName = StringSanitizer.create( macroDescriptor.getName() );

                String macroTypeName =
                    "Macro_" + StringSanitizer.create( macroDescriptor.getKey().getApplicationKey().getName() ) + "_" + descriptorName;

                String macroDataConfigTypeName = macroTypeName + "_DataConfig";

                List<GraphQLFieldDefinition> macroDataConfigFields = new ArrayList<>();
                macroDataConfigFields.add( outputField( "body", Scalars.GraphQLString ) );

                getFilteredFormItems( macroDescriptor.getForm().getFormItems() ).forEach( formItem -> {
                    String fieldName = StringSanitizer.create( formItem.getName() );

                    GraphQLOutputType formItemObject =
                        (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( macroDataConfigTypeName, formItem );

                    GraphQLFieldDefinition field =
                        outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

                    guillotineContext.registerDataFetcher( macroDataConfigTypeName, fieldName,
                                                           new FormItemDataFetcher( formItem, serviceFacade ) );

                    macroDataConfigFields.add( field );
                } );

                GraphQLObjectType macroDataConfigType = newObject( macroDataConfigTypeName,
                                                                   "Macro descriptor data config for application ['" +
                                                                       macroDescriptor.getKey().getApplicationKey() +
                                                                       "'] and descriptor ['" + descriptorName + "']",
                                                                   macroDataConfigFields );

                guillotineContext.registerType( macroDataConfigType.getName(), macroDataConfigType );
                macroConfigTypeFields.add( outputField( descriptorName, macroDataConfigType ) );
            } );

        GraphQLObjectType macroConfigType = newObject( "MacroConfig", "Macro config type.", macroConfigTypeFields );
        guillotineContext.registerType( macroConfigType.getName(), macroConfigType );
    }

    private void createMacroType()
    {
        List<GraphQLFieldDefinition> macroTypeFields = new ArrayList<>();

        macroTypeFields.add( outputField( "ref", Scalars.GraphQLString ) );
        macroTypeFields.add( outputField( "name", Scalars.GraphQLString ) );
        macroTypeFields.add( outputField( "descriptor", Scalars.GraphQLString ) );
        macroTypeFields.add( outputField( "config", guillotineContext.getOutputType( "MacroConfig" ) ) );

        GraphQLObjectType macroType = newObject( "Macro", "Macro type.", macroTypeFields );

        guillotineContext.registerType( macroType.getName(), macroType );
    }
}
