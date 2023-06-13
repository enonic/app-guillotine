package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.FormItemDataFetcher;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;

import static com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper.getFilteredFormItems;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class MacroTypesFactory
{
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

        serviceFacade.getComponentDescriptorService().getMacroDescriptors( context.getApplications() ).forEach( macroDescriptor -> {
            String descriptorName = StringNormalizer.create( macroDescriptor.getName() );

            String macroTypeName =
                "Macro_" + StringNormalizer.create( macroDescriptor.getKey().getApplicationKey().getName() ) + "_" + descriptorName;

            String macroDataConfigTypeName = macroTypeName + "_DataConfig";

            List<GraphQLFieldDefinition> macroDataConfigFields = new ArrayList<>();
            macroDataConfigFields.add( outputField( "body", Scalars.GraphQLString ) );

            getFilteredFormItems( macroDescriptor.getForm().getFormItems() ).forEach( formItem -> {
                String fieldName = StringNormalizer.create( formItem.getName() );

                GraphQLOutputType formItemObject =
                    (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( macroDataConfigTypeName, formItem );

                GraphQLFieldDefinition field =
                    outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

                context.registerDataFetcher( macroDataConfigTypeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade ) );

                macroDataConfigFields.add( field );
            } );

            GraphQLObjectType macroDataConfigType = newObject( context.uniqueName( macroDataConfigTypeName ),
                                                               "Macro descriptor data config for application ['" +
                                                                   macroDescriptor.getKey().getApplicationKey() + "'] and descriptor ['" +
                                                                   descriptorName + "']", macroDataConfigFields );

            context.registerType( macroDataConfigType.getName(), macroDataConfigType );
            macroConfigTypeFields.add( outputField( descriptorName, macroDataConfigType ) );
        } );

        if ( !macroConfigTypeFields.isEmpty() )
        {
            GraphQLObjectType macroConfigType =
                newObject( context.uniqueName( "MacroConfig" ), "Macro config type.", macroConfigTypeFields );
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
    }
}
