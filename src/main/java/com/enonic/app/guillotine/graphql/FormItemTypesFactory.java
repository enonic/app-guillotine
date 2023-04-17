package com.enonic.app.guillotine.graphql;

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

import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeConfig;
import com.enonic.xp.inputtype.InputTypeName;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.newArgument;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.newEnum;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;

public class FormItemTypesFactory
{
    private final GuillotineContext guillotineContext;

    private final ServiceFacade serviceFacade;

    public FormItemTypesFactory( final GuillotineContext guillotineContext, final ServiceFacade serviceFacade )
    {
        this.guillotineContext = guillotineContext;
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

    private GraphQLOutputType generateItemSetObjectType( String parentTypeName, FormItemSet formItemSet )
    {
        String typeName = parentTypeName + "_" + NamingHelper.camelCase( StringSanitizer.create( formItemSet.getName() ) );
        String description = formItemSet.getLabel();

        List<GraphQLFieldDefinition> fields =
            FormItemTypesHelper.getFilteredFormItems( formItemSet.getFormItems() ).stream().map( formItem -> {
                String fieldName = StringSanitizer.create( formItem.getName() );

                GraphQLOutputType formItemObject = (GraphQLOutputType) generateFormItemObject( parentTypeName, formItem );

                GraphQLFieldDefinition field = outputField( fieldName, formItemObject, generateFormItemArguments( formItem ) );

                guillotineContext.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade ) );

                return field;
            } ).collect( Collectors.toList() );

        return newObject( typeName, description, fields );
    }

    private GraphQLObjectType generateOptionSetObjectType( String parentTypeName, FormOptionSet formOptionSet )
    {
        String typeName = parentTypeName + "_" + NamingHelper.camelCase( StringSanitizer.create( formOptionSet.getName() ) );
        String description = formOptionSet.getLabel();

        GraphQLEnumType enumType = generateOptionSetEnum( formOptionSet, typeName );

        GraphQLFieldDefinition selectedField =
            outputField( "_selected", formOptionSet.getMultiselection().getMaximum() == 1 ? enumType : new GraphQLList( enumType ) );

        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( selectedField );

        guillotineContext.registerDataFetcher( typeName, selectedField.getName(), environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return formOptionSet.getMultiselection().getMaximum() == 1
                ? sourceAsMap.get( "_selected" )
                : ArrayHelper.forceArray( sourceAsMap.get( "_selected" ) );
        } );

        formOptionSet.forEach( option -> {
            String optionName = StringSanitizer.create( option.getName() );
            GraphQLType type = generateOptionObjectType( parentTypeName, option );

            fields.add( outputField( optionName, (GraphQLOutputType) type ) );

            guillotineContext.registerDataFetcher( typeName, optionName, new FormItemDataFetcher( option, serviceFacade ) );
        } );

        return newObject( typeName, description, fields );
    }

    private GraphQLType getTypeForFormInputType( Input formItem )
    {
        if ( InputTypeName.ATTACHMENT_UPLOADER.equals( formItem.getInputType() ) )
        {
            return guillotineContext.getOutputType( "Attachment" );
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
            return ExtendedScalars.LocalTime;
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
            return guillotineContext.getOutputType( "GeoPoint" );
        }
        if ( InputTypeName.HTML_AREA.equals( formItem.getInputType() ) )
        {
            return guillotineContext.getOutputType( "RichText" );
        }
        if ( InputTypeName.CONTENT_SELECTOR.equals( formItem.getInputType() ) ||
            InputTypeName.IMAGE_SELECTOR.equals( formItem.getInputType() ) ||
            InputTypeName.MEDIA_UPLOADER.equals( formItem.getInputType() ) ||
            InputTypeName.MEDIA_SELECTOR.equals( formItem.getInputType() ) )
        {
            return new GraphQLTypeReference( "Content" );
        }
        if ( InputTypeName.IMAGE_UPLOADER.equals( formItem.getInputType() ) )
        {
            return guillotineContext.getOutputType( "MediaUploader" );
        }
        if ( InputTypeName.RADIO_BUTTON.equals( formItem.getInputType() ) )
        {
            return Scalars.GraphQLString;
        }
        if ( InputTypeName.SITE_CONFIGURATOR.equals( formItem.getInputType() ) )
        {
            return guillotineContext.getOutputType( "SiteConfigurator" );
        }

        return Scalars.GraphQLString;
    }

    public List<GraphQLArgument> generateFormItemArguments( FormItem formItem )
    {
        List<GraphQLArgument> result = new ArrayList<>();

        generateFormItemOccurrencesArgument( FormItemTypesHelper.getOccurrences( formItem ), result );

        if ( formItem instanceof Input && ( (Input) formItem ).getInputType().equals( InputTypeName.HTML_AREA ) )
        {
            result.add( newArgument( "processHtml", guillotineContext.getInputType( "ProcessHtmlInput" ) ) );
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

        formOptionSet.forEach( option -> enumValues.put( StringSanitizer.create( option.getName() ), option.getName() ) );

        return newEnum( enumName, description, enumValues );
    }

    private GraphQLObjectType generateOptionSetObjectType( String parentTypeName, FormOptionSetOption formOptionSet )
    {
        String typeName = parentTypeName + "_" + NamingHelper.camelCase( StringSanitizer.create( formOptionSet.getName() ) );
        String description = formOptionSet.getLabel();

        List<FormItem> formItems = FormItemTypesHelper.getFilteredFormItems( formOptionSet.getFormItems() );
        List<GraphQLFieldDefinition> fields = formItems.stream().map( formItem -> {
            String fieldName = StringSanitizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) generateFormItemObject( parentTypeName, formItem );

            GraphQLFieldDefinition field = outputField( fieldName, formItemObject, generateFormItemArguments( formItem ) );

            guillotineContext.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade ) );

            return field;
        } ).collect( Collectors.toList() );

        return newObject( typeName, description, fields );
    }

    private GraphQLType generateOptionObjectType( String parentTypeName, FormOptionSetOption option )
    {
        return option.getFormItems().size() > 0 ? generateOptionSetObjectType( parentTypeName, option ) : Scalars.GraphQLString;
    }
}
