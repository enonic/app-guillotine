package com.enonic.app.guillotine.graphql.factory;

import java.util.Objects;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLObjectType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getNameForGraphQLTypeReference;
import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getOriginalTypeFromGraphQLList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FormTypesVerify
{
    private final GuillotineContext context;

    public FormTypesVerify( final GuillotineContext context )
    {
        this.context = context;
    }

    public void verify()
    {
        verifyOccurrences();
        verifyDefaultValue();
        verifyFormItemSet();
        verifyFormLayout();
        verifyFormOptionSetOption();
        verifyFormOptionSet();
        verifyFormInput();
    }

    private void verifyFormInput()
    {
        GraphQLObjectType type = context.getOutputType( "FormInput" );

        assertEquals( "Form input.", type.getDescription() );

        assertTrue( type.getInterfaces().stream().allMatch( i -> Objects.equals( "FormItem", i.getName() ) ) );

        assertEquals( 11, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "label" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "helpText" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "customText" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "validationRegexp" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getFieldDefinition( "maximize" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "inputType" ).getType() );
        assertEquals( ExtendedScalars.Json, type.getFieldDefinition( "configAsJson" ).getType() );
        assertEquals( "FormItemType", getNameForGraphQLTypeReference( type.getFieldDefinition( "formItemType" ).getType() ) );
        assertEquals( "DefaultValue", getNameForGraphQLTypeReference( type.getFieldDefinition( "defaultValue" ).getType() ) );
        assertEquals( "Occurrences", getNameForGraphQLTypeReference( type.getFieldDefinition( "occurrences" ).getType() ) );
    }

    private void verifyFormOptionSet()
    {
        GraphQLObjectType type = context.getOutputType( "FormOptionSet" );

        assertEquals( "Form option set.", type.getDescription() );

        assertTrue( type.getInterfaces().stream().allMatch( i -> Objects.equals( "FormItem", i.getName() ) ) );

        assertEquals( 8, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "label" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "helpText" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getFieldDefinition( "expanded" ).getType() );
        assertEquals( "FormItemType", getNameForGraphQLTypeReference( type.getFieldDefinition( "formItemType" ).getType() ) );
        assertEquals( "Occurrences", getNameForGraphQLTypeReference( type.getFieldDefinition( "selection" ).getType() ) );
        assertEquals( "Occurrences", getNameForGraphQLTypeReference( type.getFieldDefinition( "occurrences" ).getType() ) );
        assertEquals( "FormOptionSetOption", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "options" ) ) );
    }

    private void verifyFormOptionSetOption()
    {
        GraphQLObjectType type = context.getOutputType( "FormOptionSetOption" );

        assertEquals( "Form option set option.", type.getDescription() );

        assertEquals( 5, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "label" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "helpText" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getFieldDefinition( "default" ).getType() );
        assertEquals( "FormItem", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "items" ) ) );
    }

    private void verifyFormLayout()
    {
        GraphQLObjectType type = context.getOutputType( "FormLayout" );

        assertEquals( "Form layout.", type.getDescription() );

        assertTrue( type.getInterfaces().stream().allMatch( i -> Objects.equals( "FormItem", i.getName() ) ) );

        assertEquals( 4, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "label" ).getType() );
        assertEquals( "FormItemType", getNameForGraphQLTypeReference( type.getFieldDefinition( "formItemType" ).getType() ) );
        assertEquals( "FormItem", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "items" ) ) );
    }

    private void verifyFormItemSet()
    {
        GraphQLObjectType type = context.getOutputType( "FormItemSet" );

        assertEquals( "Form item set.", type.getDescription() );

        assertTrue( type.getInterfaces().stream().allMatch( i -> Objects.equals( "FormItem", i.getName() ) ) );

        assertEquals( 7, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "label" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "customText" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "helpText" ).getType() );
        assertEquals( "FormItemType", getNameForGraphQLTypeReference( type.getFieldDefinition( "formItemType" ).getType() ) );
        assertEquals( "Occurrences", getNameForGraphQLTypeReference( type.getFieldDefinition( "occurrences" ).getType() ) );
        assertEquals( "FormItem", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "items" ) ) );
    }

    private void verifyDefaultValue()
    {
        GraphQLObjectType type = context.getOutputType( "DefaultValue" );

        assertEquals( "Default value.", type.getDescription() );

        assertEquals( 2, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "value" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "type" ).getType() );
    }

    private void verifyOccurrences()
    {
        GraphQLObjectType type = context.getOutputType( "Occurrences" );

        assertEquals( "Occurrences.", type.getDescription() );

        assertEquals( 2, type.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLInt, type.getFieldDefinition( "maximum" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getFieldDefinition( "minimum" ).getType() );
    }
}
