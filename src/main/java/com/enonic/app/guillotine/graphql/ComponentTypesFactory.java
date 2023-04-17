package com.enonic.app.guillotine.graphql;

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

import com.enonic.xp.form.FormItems;
import com.enonic.xp.region.ComponentDescriptor;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;

public class ComponentTypesFactory
{
    private final GuillotineContext guillotineContext;

    private final ServiceFacade serviceFacade;

    private final FormItemTypesFactory formItemTypesFactory;

    public ComponentTypesFactory( GuillotineContext guillotineContext, ServiceFacade serviceFacade )
    {
        this.guillotineContext = guillotineContext;
        this.serviceFacade = serviceFacade;
        this.formItemTypesFactory = new FormItemTypesFactory( guillotineContext, serviceFacade );
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

        GraphQLObjectType configType = guillotineContext.getOutputType( "PartComponentDataConfig" );
        if ( configType != null )
        {
            fields.add( outputField( "config", configType ) );
        }

        GraphQLObjectType objectType = newObject( "PartComponentData", "Part component data.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.registerDataFetcher( objectType.getName(), "configAsJson", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "config" );
        } );
    }

    private void createPageComponentData()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "descriptor", Scalars.GraphQLString ) );
        fields.add( outputField( "customized", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "template", new GraphQLTypeReference( "Content" ) ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType configType = guillotineContext.getOutputType( "PageComponentDataConfig" );
        if ( configType != null )
        {
            fields.add( outputField( "config", configType ) );
        }

        GraphQLObjectType objectType = newObject( "PageComponentData", "Page component data.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.registerDataFetcher( objectType.getName(), "configAsJson", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "config" );
        } );
    }

    private void createLayoutComponentData()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "descriptor", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType configType = guillotineContext.getOutputType( "LayoutComponentDataConfig" );
        if ( configType != null )
        {
            fields.add( outputField( "config", configType ) );
        }

        GraphQLObjectType objectType = newObject( "LayoutComponentData", "Layout component data.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.registerDataFetcher( objectType.getName(), "configAsJson", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "config" );
        } );
    }

    private void createImageComponentData()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "id", new GraphQLNonNull( Scalars.GraphQLID ) ) );
        fields.add( outputField( "caption", Scalars.GraphQLString ) );
        fields.add( outputField( "image", new GraphQLTypeReference( "media_Image" ) ) );

        GraphQLObjectType objectType = newObject( "ImageComponentData", "Image component data.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );
    }

    private void createTextComponentDataType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "value", new GraphQLNonNull( guillotineContext.getOutputType( "RichText" ) ) ) );

        GraphQLObjectType objectType = newObject( "TextComponentData", "Text component data.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );
    }

    private void createFragmentComponentDataType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "id", new GraphQLNonNull( Scalars.GraphQLID ) ) );
        fields.add( outputField( "fragment", new GraphQLTypeReference( "Content" ) ) );

        GraphQLObjectType objectType = newObject( "FragmentComponentData", "Fragment component data.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );
    }

    private void createComponentType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "type", new GraphQLNonNull( guillotineContext.getEnumType( "ComponentType" ) ) ) );
        fields.add( outputField( "path", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        fields.add( outputField( "part", guillotineContext.getOutputType( "PartComponentData" ) ) );
        fields.add( outputField( "page", guillotineContext.getOutputType( "PageComponentData" ) ) );
        fields.add( outputField( "layout", guillotineContext.getOutputType( "LayoutComponentData" ) ) );
        fields.add( outputField( "image", guillotineContext.getOutputType( "ImageComponentData" ) ) );
        fields.add( outputField( "text", guillotineContext.getOutputType( "TextComponentData" ) ) );
        fields.add( outputField( "fragment", guillotineContext.getOutputType( "FragmentComponentData" ) ) );

        GraphQLObjectType objectType = newObject( "Component", "Component.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );
    }

    private void createComponentDataConfigType( String componentType )
    {
        String componentDataConfigTypeName = componentType + "ComponentDataConfig";

        List<GraphQLFieldDefinition> componentFields = new ArrayList<>();

        guillotineContext.getApplications().forEach( applicationKey -> {
            String componentApplicationConfigTypeName =
                componentType + "_" + StringSanitizer.create( applicationKey ) + "_ComponentDataApplicationConfig";

            List<ComponentDescriptor> descriptors =
                serviceFacade.getComponentDescriptorService().getComponentDescriptors( componentType, applicationKey );

            List<GraphQLFieldDefinition> componentApplicationTypeFields = new ArrayList<>();

            descriptors.forEach( descriptor -> {
                String descriptorName = StringSanitizer.create( descriptor.getName() );

                String componentApplicationDescriptorTypeName =
                    componentType + "_" + StringSanitizer.create( applicationKey ) + "_" + descriptorName;

                List<GraphQLFieldDefinition> descriptorConfigTypeFields =
                    createFormItemFields( descriptor.getConfig().getFormItems(), componentApplicationDescriptorTypeName );

                if ( !descriptorConfigTypeFields.isEmpty() )
                {
                    GraphQLObjectType descriptorConfigType = newObject( componentApplicationDescriptorTypeName,
                                                                        componentType + " component application config for application ['" +
                                                                            applicationKey + "'] and descriptor ['" + descriptor.getName() +
                                                                            "']", descriptorConfigTypeFields );

                    guillotineContext.registerType( descriptorConfigType.getName(), descriptorConfigType );

                    GraphQLFieldDefinition componentApplicationField = outputField( descriptorName, descriptorConfigType );
                    componentApplicationTypeFields.add( componentApplicationField );

                    guillotineContext.registerDataFetcher( componentApplicationConfigTypeName, componentApplicationField.getName(),
                                                           environment -> {
                                                               Map<String, Object> sourceAsMap = environment.getSource();
                                                               return sourceAsMap.get( descriptor.getName() );
                                                           } );
                }
            } );

            if ( !componentApplicationTypeFields.isEmpty() )
            {
                GraphQLObjectType componentApplicationConfigType = newObject( componentApplicationConfigTypeName, componentType +
                    " component application config for application ['" + applicationKey + "']", componentApplicationTypeFields );

                GraphQLFieldDefinition componentTypeField =
                    outputField( StringSanitizer.create( applicationKey ), componentApplicationConfigType );
                componentFields.add( componentTypeField );

                guillotineContext.registerDataFetcher( componentDataConfigTypeName, componentTypeField.getName(), environment -> {
                    Map<String, Object> sourceAsMap = environment.getSource();
                    return sourceAsMap.get( NamingHelper.applicationConfigKey( applicationKey ) );
                } );
            }
        } );

        if ( !componentFields.isEmpty() )
        {
            GraphQLObjectType componentDataType =
                newObject( componentDataConfigTypeName, componentType + " component config.", componentFields );

            guillotineContext.registerType( componentDataType.getName(), componentDataType );
        }
    }

    private List<GraphQLFieldDefinition> createFormItemFields( FormItems formItems, String typeName )
    {
        List<GraphQLFieldDefinition> resultFields = new ArrayList<>();

        FormItemTypesHelper.getFilteredFormItems( formItems ).forEach( formItem -> {
            String fieldName = StringSanitizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( typeName, formItem );

            GraphQLFieldDefinition field =
                outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

            guillotineContext.registerDataFetcher( typeName, fieldName,
                                                   new FormItemDataFetcher( formItem, serviceFacade ) ); // TODO contentAsMap

            resultFields.add( field );
        } );

        return resultFields;
    }
}
