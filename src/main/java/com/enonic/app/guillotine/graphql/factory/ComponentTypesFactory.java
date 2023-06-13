package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.FormItemDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetFieldAsJsonDataFetcher;
import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.app.guillotine.graphql.helper.NamingHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.xp.form.FormItems;
import com.enonic.xp.region.ComponentDescriptor;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class ComponentTypesFactory
{
    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    private final FormItemTypesFactory formItemTypesFactory;

    public ComponentTypesFactory( GuillotineContext context, ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;
        this.formItemTypesFactory = new FormItemTypesFactory( context, serviceFacade );
    }

    public void create()
    {
        createComponentDataConfigType( "Page" );
        createComponentDataConfigType( "Part" );
        createComponentDataConfigType( "Layout" );

        createPageComponentData();
        createPartComponentData();
        createLayoutComponentData();
        createImageComponentData();
        createTextComponentDataType();
        createFragmentComponentDataType();

        createComponentType();
    }

    private void createPartComponentData()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "descriptor", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType configType = context.getOutputType( "PartComponentDataConfig" );
        if ( configType != null )
        {
            fields.add( outputField( "config", configType ) );
        }

        GraphQLObjectType objectType = newObject( context.uniqueName( "PartComponentData" ), "Part component data.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "configAsJson", new GetFieldAsJsonDataFetcher( "config" ) );

    }

    private void createPageComponentData()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "descriptor", Scalars.GraphQLString ) );
        fields.add( outputField( "customized", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "template", GraphQLTypeReference.typeRef( "Content" ) ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType configType = context.getOutputType( "PageComponentDataConfig" );
        if ( configType != null )
        {
            fields.add( outputField( "config", configType ) );
        }

        GraphQLObjectType objectType = newObject( context.uniqueName( "PageComponentData" ), "Page component data.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "configAsJson", new GetFieldAsJsonDataFetcher( "config" ) );
    }

    private void createLayoutComponentData()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "descriptor", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType configType = context.getOutputType( "LayoutComponentDataConfig" );
        if ( configType != null )
        {
            fields.add( outputField( "config", configType ) );
        }

        GraphQLObjectType objectType = newObject( context.uniqueName( "LayoutComponentData" ), "Layout component data.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "configAsJson", new GetFieldAsJsonDataFetcher( "config" ) );
    }

    private void createImageComponentData()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "id", new GraphQLNonNull( Scalars.GraphQLID ) ) );
        fields.add( outputField( "caption", Scalars.GraphQLString ) );
        fields.add( outputField( "image", GraphQLTypeReference.typeRef( "media_Image" ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( "ImageComponentData" ), "Image component data.", fields );
        context.registerType( objectType.getName(), objectType );
    }

    private void createTextComponentDataType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "value", new GraphQLNonNull( GraphQLTypeReference.typeRef( "RichText" ) ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( "TextComponentData" ), "Text component data.", fields );
        context.registerType( objectType.getName(), objectType );
    }

    private void createFragmentComponentDataType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "id", new GraphQLNonNull( Scalars.GraphQLID ) ) );
        fields.add( outputField( "fragment", GraphQLTypeReference.typeRef( "Content" ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( "FragmentComponentData" ), "Fragment component data.", fields );
        context.registerType( objectType.getName(), objectType );
    }

    private void createComponentType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "type", new GraphQLNonNull( GraphQLTypeReference.typeRef( "ComponentType" ) ) ) );
        fields.add( outputField( "path", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( outputField( "part", GraphQLTypeReference.typeRef( "PartComponentData" ) ) );
        fields.add( outputField( "page", GraphQLTypeReference.typeRef( "PageComponentData" ) ) );
        fields.add( outputField( "layout", GraphQLTypeReference.typeRef( "LayoutComponentData" ) ) );
        fields.add( outputField( "image", GraphQLTypeReference.typeRef( "ImageComponentData" ) ) );
        fields.add( outputField( "text", GraphQLTypeReference.typeRef( "TextComponentData" ) ) );
        fields.add( outputField( "fragment", GraphQLTypeReference.typeRef( "FragmentComponentData" ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( "Component" ), "Component.", fields );
        context.registerType( objectType.getName(), objectType );
    }

    private void createComponentDataConfigType( String componentType )
    {
        String componentDataConfigTypeName = componentType + "ComponentDataConfig";

        List<GraphQLFieldDefinition> componentFields = new ArrayList<>();

        context.getApplications().forEach( applicationKey -> {
            String componentApplicationConfigTypeName =
                componentType + "_" + StringNormalizer.create( applicationKey ) + "_ComponentDataApplicationConfig";

            List<ComponentDescriptor> descriptors =
                serviceFacade.getComponentDescriptorService().getComponentDescriptors( componentType, applicationKey );

            List<GraphQLFieldDefinition> componentApplicationTypeFields = new ArrayList<>();

            descriptors.forEach( descriptor -> {
                String descriptorName = StringNormalizer.create( descriptor.getName() );

                String componentApplicationDescriptorTypeName =
                    componentType + "_" + StringNormalizer.create( applicationKey ) + "_" + descriptorName;

                List<GraphQLFieldDefinition> descriptorConfigTypeFields =
                    createFormItemFields( descriptor.getConfig().getFormItems(), componentApplicationDescriptorTypeName );

                if ( !descriptorConfigTypeFields.isEmpty() )
                {
                    GraphQLObjectType descriptorConfigType = newObject( context.uniqueName( componentApplicationDescriptorTypeName ),
                                                                        componentType + " component application config for application ['" +
                                                                            applicationKey + "'] and descriptor ['" + descriptor.getName() +
                                                                            "']", descriptorConfigTypeFields );

                    context.registerType( descriptorConfigType.getName(), descriptorConfigType );

                    GraphQLFieldDefinition componentApplicationField = outputField( descriptorName, descriptorConfigType );
                    componentApplicationTypeFields.add( componentApplicationField );

                    context.registerDataFetcher( componentApplicationConfigTypeName, componentApplicationField.getName(), environment -> {
                        Map<String, Object> sourceAsMap = environment.getSource();
                        return sourceAsMap.get( descriptor.getName() );
                    } );
                }
            } );

            if ( !componentApplicationTypeFields.isEmpty() )
            {
                GraphQLObjectType componentApplicationConfigType = newObject( context.uniqueName( componentApplicationConfigTypeName ),
                                                                              componentType +
                                                                                  " component application config for application ['" +
                                                                                  applicationKey + "']", componentApplicationTypeFields );

                GraphQLFieldDefinition componentTypeField =
                    outputField( StringNormalizer.create( applicationKey ), componentApplicationConfigType );
                componentFields.add( componentTypeField );

                context.registerDataFetcher( componentDataConfigTypeName, componentTypeField.getName(),
                                             new GetFieldAsJsonDataFetcher( NamingHelper.applicationConfigKey( applicationKey ) ) );
            }
        } );

        if ( !componentFields.isEmpty() )
        {
            GraphQLObjectType componentDataType =
                newObject( context.uniqueName( componentDataConfigTypeName ), componentType + " component config.", componentFields );

            context.registerType( componentDataType.getName(), componentDataType );
        }
    }

    private List<GraphQLFieldDefinition> createFormItemFields( FormItems formItems, String typeName )
    {
        List<GraphQLFieldDefinition> resultFields = new ArrayList<>();

        FormItemTypesHelper.getFilteredFormItems( formItems ).forEach( formItem -> {
            String fieldName = StringNormalizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( typeName, formItem );

            GraphQLFieldDefinition field =
                outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

            context.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade ) ); // TODO contentAsMap

            resultFields.add( field );
        } );

        return resultFields;
    }
}
