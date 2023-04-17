package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.fetchers.FormInputDefaultValueDataFetcher;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.newInterface;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;

public class FormTypesFactory
{
    private final GuillotineContext guillotineContext;

    public FormTypesFactory( final GuillotineContext guillotineContext )
    {
        this.guillotineContext = guillotineContext;
    }

    public void create()
    {
        createFormItemTypeInterface();
        createOccurrencesType();
        createDefaultValueType();
        createFormItemSetType();
        createFormLayoutType();
        createFormOptionSetOptionType();
        createFormOptionSetType();
        createFormInputType();
    }

    private void createFormItemTypeInterface()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "formItemType", guillotineContext.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );

        GraphQLInterfaceType interfaceType = newInterface( "FormItem", "FormItem.", fields );
        guillotineContext.registerType( interfaceType.getName(), interfaceType );

        guillotineContext.registerTypeResolver( interfaceType.getName(), env -> {
            Object source = env.getObject();
            if ( source instanceof Map )
            {
                Map<String, Object> sourceAsMap = CastHelper.cast( source );
                switch ( sourceAsMap.get( "formItemType" ).toString() )
                {
                    case "ItemSet":
                        return guillotineContext.getOutputType( "FormItemSet" );
                    case "Layout":
                        return guillotineContext.getOutputType( "FormLayout" );
                    case "Input":
                        return guillotineContext.getOutputType( "FormInput" );
                    case "OptionSet":
                        return guillotineContext.getOutputType( "FormOptionSet" );
                }
            }
            return null;
        } );
    }

    private void createOccurrencesType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "maximum", Scalars.GraphQLInt ) );
        fields.add( outputField( "minimum", Scalars.GraphQLInt ) );

        GraphQLObjectType objectType = newObject( "Occurrences", "Occurrences.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );
    }

    private void createDefaultValueType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "value", Scalars.GraphQLString ) );
        fields.add( outputField( "type", Scalars.GraphQLString ) );

        GraphQLObjectType objectType = newObject( "DefaultValue", "Default value.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.registerDataFetcher( objectType.getName(), "value", new FormInputDefaultValueDataFetcher() );
    }

    private void createFormItemSetType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "formItemType", guillotineContext.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "customText", Scalars.GraphQLString ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "occurrences", guillotineContext.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "items", new GraphQLList( new GraphQLTypeReference( "FormItem" ) ) ) );

        GraphQLObjectType objectType =
            newObject( "FormItemSet", "Form item set.", List.of( guillotineContext.getInterfaceType( "FormItem" ) ), fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.addDictionaryType( objectType );
    }

    private void createFormLayoutType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "formItemType", guillotineContext.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "items", new GraphQLList( new GraphQLTypeReference( "FormItem" ) ) ) );

        GraphQLObjectType objectType =
            newObject( "FormLayout", "Form layout.", List.of( guillotineContext.getInterfaceType( "FormItem" ) ), fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.addDictionaryType( objectType );
    }

    private void createFormOptionSetOptionType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "default", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "items", new GraphQLList( new GraphQLTypeReference( "FormItem" ) ) ) );

        GraphQLObjectType objectType = newObject( "FormOptionSetOption", "Form layout.", fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.addDictionaryType( objectType );
    }

    private void createFormOptionSetType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "formItemType", guillotineContext.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "expanded", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "occurrences", guillotineContext.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "selection", guillotineContext.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "options", new GraphQLList( guillotineContext.getOutputType( "FormOptionSetOption" ) ) ) );

        GraphQLObjectType objectType =
            newObject( "FormOptionSet", "Form option set.", List.of( guillotineContext.getInterfaceType( "FormItem" ) ), fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.addDictionaryType( objectType );
    }

    private void createFormInputType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "formItemType", guillotineContext.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "customText", Scalars.GraphQLString ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "validationRegexp", Scalars.GraphQLString ) );
        fields.add( outputField( "maximize", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "inputType", Scalars.GraphQLString ) );
        fields.add( outputField( "occurrences", guillotineContext.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "defaultValue", guillotineContext.getOutputType( "DefaultValue" ) ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType objectType =
            newObject( "FormInput", "Form input.", List.of( guillotineContext.getInterfaceType( "FormItem" ) ), fields );
        guillotineContext.registerType( objectType.getName(), objectType );

        guillotineContext.addDictionaryType( objectType );

        guillotineContext.registerDataFetcher( objectType.getName(), "configAsJson", environment -> {
            Map<String, Object> source = environment.getSource();
            return source.get( "config" );
        } );

        guillotineContext.registerDataFetcher( objectType.getName(), "defaultValue", environment -> {
            Map<String, Object> source = environment.getSource();
            return source.get( "default" );
        } );
    }


}
