package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.FormItemDataFetcher;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.app.guillotine.graphql.helper.NamingHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.app.guillotine.graphql.scalars.CustomScalars;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeConfig;
import com.enonic.xp.inputtype.InputTypeName;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newArgument;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newEnum;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class FormItemTypesFactory
{
    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    public FormItemTypesFactory( final GuillotineContext context, final ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;
    }

    public GraphQLType generateFormItemObject( String parentTypeName, FormItem formItem )
    {
        GraphQLType formItemObject;

        if ( formItem instanceof FormItemSet )
        {
            formItemObject = generateItemSetObjectType( parentTypeName, (FormItemSet) formItem );
        }
        else if ( formItem instanceof FormOptionSet )
        {
            formItemObject = generateOptionSetObjectType( parentTypeName, (FormOptionSet) formItem );
        }
        else if ( formItem instanceof Input )
        {
            formItemObject = getTypeForFormInputType( (Input) formItem );
        }
        else
        {
            formItemObject = Scalars.GraphQLString;
        }

        Occurrences occurrences = FormItemTypesHelper.getOccurrences( formItem );

        return occurrences.getMaximum() == 1 ? formItemObject : new GraphQLList( formItemObject );
    }

    private GraphQLType generateItemSetObjectType( String parentTypeName, FormItemSet formItemSet )
    {
        String typeName =
            context.uniqueName( parentTypeName + "_" + NamingHelper.camelCase( StringNormalizer.create( formItemSet.getName() ) ) );

        String description = formItemSet.getLabel();

        List<GraphQLFieldDefinition> fields = FormItemTypesHelper.getFilteredFormItems( formItemSet ).stream().map( formItem -> {
            String fieldName = StringNormalizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) generateFormItemObject( parentTypeName, formItem );

            GraphQLFieldDefinition field = outputField( fieldName, formItemObject, generateFormItemArguments( formItem ) );

            context.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade, context ) );

            return field;
        } ).collect( Collectors.toList() );

        GraphQLObjectType objectType = newObject( typeName, description, fields );
        context.registerType( objectType.getName(), objectType );
        return objectType;
    }

    private GraphQLObjectType generateOptionSetObjectType( String parentTypeName, FormOptionSet formOptionSet )
    {
        String typeName = parentTypeName + "_" + NamingHelper.camelCase( StringNormalizer.create( formOptionSet.getName() ) );
        String uniqueTypeName = context.uniqueName( typeName );
        String description = formOptionSet.getLabel();

        GraphQLEnumType enumType = generateOptionSetEnum( formOptionSet, typeName );

        GraphQLFieldDefinition selectedField =
            outputField( "_selected", formOptionSet.getMultiselection().getMaximum() == 1 ? enumType : new GraphQLList( enumType ) );

        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( selectedField );

        context.registerDataFetcher( uniqueTypeName, selectedField.getName(), environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return formOptionSet.getMultiselection().getMaximum() == 1
                ? sourceAsMap.get( "_selected" )
                : ArrayHelper.forceArray( sourceAsMap.get( "_selected" ) );
        } );

        formOptionSet.forEach( option -> {
            String optionName = StringNormalizer.create( option.getName() );
            GraphQLType type = generateOptionObjectType( parentTypeName, option );

            fields.add( outputField( optionName, type ) );

            context.registerDataFetcher( uniqueTypeName, optionName, new FormItemDataFetcher( option, serviceFacade, context ) );
        } );

        GraphQLObjectType objectType = newObject( uniqueTypeName, description, fields );
        context.registerType( objectType.getName(), objectType );
        return objectType;
    }

    private GraphQLType getTypeForFormInputType( Input formItem )
    {
        if ( InputTypeName.ATTACHMENT_UPLOADER.equals( formItem.getInputType() ) )
        {
            return GraphQLTypeReference.typeRef( "Attachment" );
        }
        if ( InputTypeName.CHECK_BOX.equals( formItem.getInputType() ) )
        {
            return Scalars.GraphQLBoolean;
        }
        if ( InputTypeName.DATE.equals( formItem.getInputType() ) )
        {
            return ExtendedScalars.Date;
        }
        if ( InputTypeName.TIME.equals( formItem.getInputType() ) )
        {
            return CustomScalars.LocalTime;
        }
        if ( InputTypeName.DATE_TIME.equals( formItem.getInputType() ) )
        {
            InputTypeConfig config = formItem.getInputTypeConfig();
            if ( config != null && config.getProperty( "timezone" ) != null &&
                "true".equals( config.getProperty( "timezone" ).getValue() ) )
            {
                return ExtendedScalars.DateTime;
            }
            return CustomScalars.LocalDateTime;
        }
        if ( InputTypeName.DOUBLE.equals( formItem.getInputType() ) )
        {
            return Scalars.GraphQLFloat;
        }
        if ( InputTypeName.GEO_POINT.equals( formItem.getInputType() ) )
        {
            return GraphQLTypeReference.typeRef( "GeoPoint" );
        }
        if ( InputTypeName.HTML_AREA.equals( formItem.getInputType() ) )
        {
            return GraphQLTypeReference.typeRef( "RichText" );
        }
        if ( InputTypeName.CONTENT_SELECTOR.equals( formItem.getInputType() ) ||
            InputTypeName.IMAGE_SELECTOR.equals( formItem.getInputType() ) ||
            InputTypeName.MEDIA_UPLOADER.equals( formItem.getInputType() ) ||
            InputTypeName.MEDIA_SELECTOR.equals( formItem.getInputType() ) )
        {
            return GraphQLTypeReference.typeRef( "Content" );
        }
        if ( InputTypeName.IMAGE_UPLOADER.equals( formItem.getInputType() ) )
        {
            return GraphQLTypeReference.typeRef( "MediaUploader" );
        }
        if ( InputTypeName.RADIO_BUTTON.equals( formItem.getInputType() ) )
        {
            return Scalars.GraphQLString;
        }
        if ( InputTypeName.SITE_CONFIGURATOR.equals( formItem.getInputType() ) )
        {
            return GraphQLTypeReference.typeRef( "SiteConfigurator" );
        }

        return Scalars.GraphQLString;
    }

    public List<GraphQLArgument> generateFormItemArguments( FormItem formItem )
    {
        List<GraphQLArgument> result = new ArrayList<>();

        generateFormItemOccurrencesArgument( FormItemTypesHelper.getOccurrences( formItem ), result );

        if ( formItem instanceof Input && ( (Input) formItem ).getInputType().equals( InputTypeName.HTML_AREA ) )
        {
            result.add( newArgument( "processHtml", GraphQLTypeReference.typeRef( "ProcessHtmlInput" ) ) );
        }

        return result;
    }

    private void generateFormItemOccurrencesArgument( Occurrences occurrences, List<GraphQLArgument> container )
    {
        if ( occurrences != null && occurrences.getMaximum() != 1 )
        {
            container.add( newArgument( "offset", Scalars.GraphQLInt ) );
            container.add( newArgument( "first", Scalars.GraphQLInt ) );
        }
    }

    private GraphQLEnumType generateOptionSetEnum( FormOptionSet formOptionSet, String optionSetName )
    {
        String enumName = optionSetName + "_OptionEnum";
        String description = formOptionSet.getLabel() + " option enum.";

        Map<String, Object> enumValues = new LinkedHashMap<>();

        formOptionSet.forEach( option -> enumValues.put( StringNormalizer.create( option.getName() ), option.getName() ) );

        GraphQLEnumType enumType = newEnum( context.uniqueName( enumName ), description, enumValues );
        context.registerType( enumType.getName(), enumType );
        return enumType;
    }

    private GraphQLObjectType generateOptionSetObjectType( String parentTypeName, FormOptionSetOption formOptionSet )
    {
        String typeName =
            context.uniqueName( parentTypeName + "_" + NamingHelper.camelCase( StringNormalizer.create( formOptionSet.getName() ) ) );
        String description = formOptionSet.getLabel();

        List<FormItem> formItems = FormItemTypesHelper.getFilteredFormItems( formOptionSet );
        List<GraphQLFieldDefinition> fields = formItems.stream().map( formItem -> {
            String fieldName = StringNormalizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) generateFormItemObject( parentTypeName, formItem );

            GraphQLFieldDefinition field = outputField( fieldName, formItemObject, generateFormItemArguments( formItem ) );

            context.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade, context ) );

            return field;
        } ).collect( Collectors.toList() );

        GraphQLObjectType objectType = newObject( typeName, description, fields );
        context.registerType( objectType.getName(), objectType );
        return objectType;
    }

    private GraphQLType generateOptionObjectType( String parentTypeName, FormOptionSetOption option )
    {
        return option.iterator().hasNext() ? generateOptionSetObjectType( parentTypeName, option ) : Scalars.GraphQLString;
    }
}
