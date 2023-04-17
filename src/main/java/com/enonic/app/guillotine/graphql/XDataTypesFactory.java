package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.FormItems;
import com.enonic.xp.schema.xdata.XDatas;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;

public class XDataTypesFactory
{
    private final GuillotineContext guillotineContext;

    private final ServiceFacade serviceFacade;

    private final FormItemTypesFactory formItemTypesFactory;

    public XDataTypesFactory( final GuillotineContext guillotineContext, final ServiceFacade serviceFacade )
    {
        this.guillotineContext = guillotineContext;
        this.serviceFacade = serviceFacade;
        this.formItemTypesFactory = new FormItemTypesFactory( guillotineContext, serviceFacade );
    }

    public void create()
    {
        String extraDataTypeName = "ExtraData";

        List<GraphQLFieldDefinition> xDataTypeFields = new ArrayList<>();

        getApplicationsKeys().forEach( applicationKey -> {
            String xDataApplicationConfigTypeName = "XData_" + StringSanitizer.create( applicationKey ) + "_ApplicationConfig";

            XDatas extraData = serviceFacade.getComponentDescriptorService().getExtraData( applicationKey );

            if ( extraData.isNotEmpty() )
            {
                List<GraphQLFieldDefinition> xDataApplicationTypeFields = new ArrayList<>();

                extraData.forEach( xData -> {
                    String descriptorName = StringSanitizer.create( xData.getName().getLocalName() );

                    String xDataTypeName = "XData_" + StringSanitizer.create( applicationKey ) + "_" + descriptorName;

                    String xDataConfigTypeName = xDataTypeName + "_DataConfig";

                    List<GraphQLFieldDefinition> xDataConfigFields =
                        createFormItemFields( xData.getForm().getFormItems(), xDataConfigTypeName );

                    if ( !xDataConfigFields.isEmpty() )
                    {
                        GraphQLObjectType xDataConfigType = newObject( xDataConfigTypeName,
                                                                       "Extra data config for application ['" + applicationKey +
                                                                           "}'] and descriptor ['" + xData.getName().getLocalName() + "']",
                                                                       xDataConfigFields );

                        guillotineContext.registerType( xDataConfigType.getName(), xDataConfigType );

                        GraphQLFieldDefinition xDataApplicationField = outputField( descriptorName, xDataConfigType );
                        xDataApplicationTypeFields.add( xDataApplicationField );

                        guillotineContext.registerDataFetcher( xDataApplicationConfigTypeName, xDataApplicationField.getName(),
                                                               environment -> {
                                                                   Map<String, Object> sourceAsMap = environment.getSource();
                                                                   return sourceAsMap.get( xData.getName().getLocalName() );
                                                               } );
                    }
                } );

                if ( !xDataApplicationTypeFields.isEmpty() )
                {
                    GraphQLObjectType applicationConfigType =
                        newObject( xDataApplicationConfigTypeName, "XDataApplicationConfig for application ['" + applicationKey + "']",
                                   xDataApplicationTypeFields );

                    GraphQLFieldDefinition xDataTypeField = outputField( StringSanitizer.create( applicationKey ), applicationConfigType );
                    xDataTypeFields.add( xDataTypeField );

                    guillotineContext.registerDataFetcher( extraDataTypeName, xDataTypeField.getName(), environment -> {
                        Map<String, Object> sourceAsMap = environment.getSource();
                        return sourceAsMap.get( NamingHelper.applicationConfigKey( applicationKey ) );
                    } );
                }
            }
        } );

        if ( !xDataTypeFields.isEmpty() )
        {
            GraphQLObjectType extraDataType = newObject( extraDataTypeName, "Extra data.", xDataTypeFields );

            guillotineContext.registerType( extraDataType.getName(), extraDataType );
        }
    }

    private List<GraphQLFieldDefinition> createFormItemFields( FormItems formItems, String typeName )
    {
        List<GraphQLFieldDefinition> xDataConfigFields = new ArrayList<>();

        FormItemTypesHelper.getFilteredFormItems( formItems ).forEach( formItem -> {
            String fieldName = StringSanitizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( typeName, formItem );

            GraphQLFieldDefinition field =
                outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

            guillotineContext.registerDataFetcher( typeName, fieldName,
                                                   new FormItemDataFetcher( formItem, serviceFacade ) ); // TODO contentAsMap

            xDataConfigFields.add( field );
        } );

        return xDataConfigFields;
    }

    private Set<String> getApplicationsKeys()
    {
        Set<String> applicationKeys = new HashSet<>( guillotineContext.getApplications() );
        applicationKeys.add( ApplicationKey.BASE.getName() );
        applicationKeys.add( ApplicationKey.MEDIA_MOD.getName() );
        return applicationKeys;
    }
}
