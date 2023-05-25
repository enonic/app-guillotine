package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.fetchers.FormItemDataFetcher;
import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.helper.NamingHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.FormItems;
import com.enonic.xp.schema.xdata.XDatas;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class XDataTypesFactory
{
    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    private final FormItemTypesFactory formItemTypesFactory;

    public XDataTypesFactory( final GuillotineContext context, final ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;
        this.formItemTypesFactory = new FormItemTypesFactory( context, serviceFacade );
    }

    public void create()
    {
        String extraDataTypeName = "ExtraData";

        List<GraphQLFieldDefinition> xDataTypeFields = new ArrayList<>();

        getApplicationsKeys().forEach( applicationKey -> {
            String xDataApplicationConfigTypeName = "XData_" + StringNormalizer.create( applicationKey ) + "_ApplicationConfig";

            XDatas extraData = serviceFacade.getComponentDescriptorService().getExtraData( applicationKey );

            if ( extraData.isNotEmpty() )
            {
                List<GraphQLFieldDefinition> xDataApplicationTypeFields = new ArrayList<>();

                extraData.forEach( xData -> {
                    String descriptorName = StringNormalizer.create( xData.getName().getLocalName() );

                    String xDataTypeName = "XData_" + StringNormalizer.create( applicationKey ) + "_" + descriptorName;

                    String xDataConfigTypeName = xDataTypeName + "_DataConfig";

                    List<GraphQLFieldDefinition> xDataConfigFields =
                        createFormItemFields( xData.getForm().getFormItems(), xDataConfigTypeName );

                    if ( !xDataConfigFields.isEmpty() )
                    {
                        GraphQLObjectType xDataConfigType = newObject( context.uniqueName( xDataConfigTypeName ),
                                                                       "Extra data config for application ['" + applicationKey +
                                                                           "}'] and descriptor ['" + xData.getName().getLocalName() + "']",
                                                                       xDataConfigFields );

                        context.registerType( xDataConfigType.getName(), xDataConfigType );

                        GraphQLFieldDefinition xDataApplicationField = outputField( descriptorName, xDataConfigType );
                        xDataApplicationTypeFields.add( xDataApplicationField );

                        context.registerDataFetcher( xDataApplicationConfigTypeName, xDataApplicationField.getName(), environment -> {
                            Map<String, Object> sourceAsMap = environment.getSource();
                            return sourceAsMap.get( xData.getName().getLocalName() );
                        } );
                    }
                } );

                if ( !xDataApplicationTypeFields.isEmpty() )
                {
                    GraphQLObjectType applicationConfigType = newObject( context.uniqueName( xDataApplicationConfigTypeName ),
                                                                         "XDataApplicationConfig for application ['" + applicationKey +
                                                                             "']", xDataApplicationTypeFields );

                    GraphQLFieldDefinition xDataTypeField = outputField( StringNormalizer.create( applicationKey ), applicationConfigType );
                    xDataTypeFields.add( xDataTypeField );

                    context.registerDataFetcher( extraDataTypeName, xDataTypeField.getName(), environment -> {
                        Map<String, Object> sourceAsMap = environment.getSource();
                        return sourceAsMap.get( NamingHelper.applicationConfigKey( applicationKey ) );
                    } );
                }
            }
        } );

        if ( !xDataTypeFields.isEmpty() )
        {
            GraphQLObjectType extraDataType = newObject( context.uniqueName( extraDataTypeName ), "Extra data.", xDataTypeFields );

            context.registerType( extraDataType.getName(), extraDataType );
        }
    }

    private List<GraphQLFieldDefinition> createFormItemFields( FormItems formItems, String typeName )
    {
        List<GraphQLFieldDefinition> xDataConfigFields = new ArrayList<>();

        FormItemTypesHelper.getFilteredFormItems( formItems ).forEach( formItem -> {
            String fieldName = StringNormalizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( typeName, formItem );

            GraphQLFieldDefinition field =
                outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

            context.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade ) ); // TODO contentAsMap

            xDataConfigFields.add( field );
        } );

        return xDataConfigFields;
    }

    private Set<String> getApplicationsKeys()
    {
        Set<String> applicationKeys = new HashSet<>( context.getApplications() );
        applicationKeys.add( ApplicationKey.BASE.getName() );
        applicationKeys.add( ApplicationKey.MEDIA_MOD.getName() );
        return applicationKeys;
    }
}
