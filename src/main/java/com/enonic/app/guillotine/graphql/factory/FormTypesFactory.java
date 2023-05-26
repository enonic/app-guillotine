package com.enonic.app.guillotine.graphql.factory;

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

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.FormInputDefaultValueDataFetcher;
import com.enonic.app.guillotine.graphql.helper.CastHelper;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newInterface;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class FormTypesFactory
{
    private final GuillotineContext context;

    public FormTypesFactory( final GuillotineContext context )
    {
        this.context = context;
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

        fields.add( outputField( "formItemType", context.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );

        GraphQLInterfaceType interfaceType = newInterface( context.uniqueName( "FormItem" ), "FormItem.", fields );
        context.registerType( interfaceType.getName(), interfaceType );

        context.registerTypeResolver( interfaceType.getName(), env -> {
            Object source = env.getObject();
            if ( source instanceof Map )
            {
                Map<String, Object> sourceAsMap = CastHelper.cast( source );
                switch ( sourceAsMap.get( "formItemType" ).toString() )
                {
                    case "ItemSet":
                        return context.getOutputType( "FormItemSet" );
                    case "Layout":
                        return context.getOutputType( "FormLayout" );
                    case "Input":
                        return context.getOutputType( "FormInput" );
                    case "OptionSet":
                        return context.getOutputType( "FormOptionSet" );
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

        GraphQLObjectType objectType = newObject( context.uniqueName( "Occurrences" ), "Occurrences.", fields );
        context.registerType( objectType.getName(), objectType );
    }

    private void createDefaultValueType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "value", Scalars.GraphQLString ) );
        fields.add( outputField( "type", Scalars.GraphQLString ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( "DefaultValue" ), "Default value.", fields );
        context.registerType( objectType.getName(), objectType );

        context.registerDataFetcher( objectType.getName(), "value", new FormInputDefaultValueDataFetcher() );
    }

    private void createFormItemSetType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "formItemType", context.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "customText", Scalars.GraphQLString ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "occurrences", context.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "items", new GraphQLList( new GraphQLTypeReference( "FormItem" ) ) ) );

        GraphQLObjectType objectType =
            newObject( context.uniqueName( "FormItemSet" ), "Form item set.", List.of( context.getInterfaceType( "FormItem" ) ), fields );
        context.registerType( objectType.getName(), objectType );

        context.addDictionaryType( objectType );
    }

    private void createFormLayoutType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "formItemType", context.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "items", new GraphQLList( new GraphQLTypeReference( "FormItem" ) ) ) );

        GraphQLObjectType objectType =
            newObject( context.uniqueName( "FormLayout" ), "Form layout.", List.of( context.getInterfaceType( "FormItem" ) ), fields );
        context.registerType( objectType.getName(), objectType );

        context.addDictionaryType( objectType );
    }

    private void createFormOptionSetOptionType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "default", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "items", new GraphQLList( new GraphQLTypeReference( "FormItem" ) ) ) );

        GraphQLObjectType objectType = newObject( context.uniqueName( "FormOptionSetOption" ), "Form layout.", fields );
        context.registerType( objectType.getName(), objectType );

        context.addDictionaryType( objectType );
    }

    private void createFormOptionSetType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "formItemType", context.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "expanded", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "occurrences", context.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "selection", context.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "options", new GraphQLList( context.getOutputType( "FormOptionSetOption" ) ) ) );

        GraphQLObjectType objectType =
            newObject( context.uniqueName( "FormOptionSet" ), "Form option set.", List.of( context.getInterfaceType( "FormItem" ) ),
                       fields );
        context.registerType( objectType.getName(), objectType );

        context.addDictionaryType( objectType );
    }

    private void createFormInputType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "formItemType", context.getEnumType( "FormItemType" ) ) );
        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "customText", Scalars.GraphQLString ) );
        fields.add( outputField( "helpText", Scalars.GraphQLString ) );
        fields.add( outputField( "validationRegexp", Scalars.GraphQLString ) );
        fields.add( outputField( "maximize", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "inputType", Scalars.GraphQLString ) );
        fields.add( outputField( "occurrences", context.getOutputType( "Occurrences" ) ) );
        fields.add( outputField( "defaultValue", context.getOutputType( "DefaultValue" ) ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType objectType =
            newObject( context.uniqueName( "FormInput" ), "Form input.", List.of( context.getInterfaceType( "FormItem" ) ), fields );
        context.registerType( objectType.getName(), objectType );

        context.addDictionaryType( objectType );

        context.registerDataFetcher( objectType.getName(), "configAsJson", environment -> {
            Map<String, Object> source = environment.getSource();
            return source.get( "config" );
        } );

        context.registerDataFetcher( objectType.getName(), "defaultValue", environment -> {
            Map<String, Object> source = environment.getSource();
            return source.get( "default" );
        } );
    }

}
