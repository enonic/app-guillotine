package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.FormItemDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetFieldAsJsonDataFetcher;
import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.app.guillotine.graphql.helper.NamingHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.schema.mixin.MixinDescriptors;

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
        String mixinTypeName = context.uniqueName( "Mixin" );

        List<GraphQLFieldDefinition> mixinTypeFields = new ArrayList<>();

        getApplicationsKeys().forEach( applicationKey -> {
            String mixinApplicationConfigTypeName =
                context.uniqueName( "Mixin_" + StringNormalizer.create( applicationKey ) + "_ApplicationConfig" );

            MixinDescriptors mixinDescriptors = serviceFacade.getComponentDescriptorService().getMixins( applicationKey );

            if ( mixinDescriptors.isNotEmpty() )
            {
                List<GraphQLFieldDefinition> mixinApplicationTypeFields = new ArrayList<>();

                mixinDescriptors.forEach( mixinDescriptor -> {
                    String descriptorName = StringNormalizer.create( mixinDescriptor.getName().getLocalName() );

                    String mixinDescriptorTypeName = "Mixin_" + StringNormalizer.create( applicationKey ) + "_" + descriptorName;

                    String mixinConfigTypeName = context.uniqueName( mixinDescriptorTypeName + "_DataConfig" );

                    List<GraphQLFieldDefinition> mixinConfigFields =
                        createFormItemFields( resolveForm( mixinDescriptor.getForm() ), mixinConfigTypeName );

                    if ( !mixinConfigFields.isEmpty() )
                    {
                        GraphQLObjectType mixinConfigType = newObject( mixinConfigTypeName,
                                                                       "Mixin data config for application ['" + applicationKey +
                                                                           "}'] and descriptor ['" +
                                                                           mixinDescriptor.getName().getLocalName() + "']",
                                                                       mixinConfigFields );

                        context.registerType( mixinConfigType.getName(), mixinConfigType );

                        GraphQLFieldDefinition mixinApplicationField = outputField( descriptorName, mixinConfigType );
                        mixinApplicationTypeFields.add( mixinApplicationField );

                        context.registerDataFetcher( mixinApplicationConfigTypeName, mixinApplicationField.getName(),
                                                     new GetFieldAsJsonDataFetcher( mixinDescriptor.getName().getLocalName() ) );
                    }
                } );

                if ( !mixinApplicationTypeFields.isEmpty() )
                {
                    GraphQLObjectType applicationConfigType =
                        newObject( mixinApplicationConfigTypeName, "MixinApplicationConfig for application ['" + applicationKey + "']",
                                   mixinApplicationTypeFields );

                    GraphQLFieldDefinition xDataTypeField = outputField( StringNormalizer.create( applicationKey ), applicationConfigType );
                    mixinTypeFields.add( xDataTypeField );

                    context.registerDataFetcher( mixinTypeName, xDataTypeField.getName(),
                                                 new GetFieldAsJsonDataFetcher( NamingHelper.applicationConfigKey( applicationKey ) ) );
                }
            }
        } );

        if ( !mixinTypeFields.isEmpty() )
        {
            GraphQLObjectType extraDataType = newObject( mixinTypeName, "Mixin data.", mixinTypeFields );

            context.registerType( extraDataType.getName(), extraDataType );
        }
    }

    private List<GraphQLFieldDefinition> createFormItemFields( final Iterable<? extends FormItem> formItems, String typeName )
    {
        List<GraphQLFieldDefinition> mixinConfigFields = new ArrayList<>();

        FormItemTypesHelper.getFilteredFormItems( formItems ).forEach( formItem -> {
            String fieldName = StringNormalizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( typeName, formItem );

            GraphQLFieldDefinition field =
                outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

            context.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade, context ) );

            mixinConfigFields.add( field );
        } );

        return mixinConfigFields;
    }

    private Set<String> getApplicationsKeys()
    {
        Set<String> applicationKeys = new HashSet<>( context.getApplications() );
        applicationKeys.add( ApplicationKey.BASE.getName() );
        applicationKeys.add( ApplicationKey.MEDIA_MOD.getName() );
        return applicationKeys;
    }

    private Form resolveForm( Form originalForm )
    {
        Form inlineForm = serviceFacade.getCmsFormFragmentService().inlineFormItems( originalForm );
        return inlineForm != null ? inlineForm : originalForm;
    }
}
