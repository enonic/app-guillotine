package com.enonic.app.guillotine.graphql.factory;

import graphql.schema.GraphQLEnumType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumTypesVerifier
{
    private final GuillotineContext context;

    public EnumTypesVerifier( final GuillotineContext context )
    {
        this.context = context;
    }

    public void verify()
    {
        verifyUrlType();
        verifyContentPathType();
        verifyMediaIntentType();
        verifyDslOperatorType();
        verifyDslSortDirectionType();
        verifyHighlightEncoderType();
        verifyHighlightTagsSchemaType();
        verifyHighlightFragmenterType();
        verifyHighlightOrderType();
        verifyDslGeoPointDistanceType();
        verifyPrincipalType();
        verifyPermission();
        verifyFormItemType();
        verifyComponentType();
    }

    private void verifyUrlType()
    {
        GraphQLEnumType type = context.getEnumType( "UrlType" );

        assertEquals( "UrlType", type.getName() );
        assertEquals( "URL type.", type.getDescription() );
        assertEquals( 2, type.getValues().size() );
        verifyTypeValue( type, "server" );
        verifyTypeValue( type, "absolute" );
    }

    private void verifyContentPathType()
    {
        GraphQLEnumType type = context.getEnumType( "ContentPathType" );

        assertEquals( "ContentPathType", type.getName() );
        assertEquals( "Content path type.", type.getDescription() );
        assertEquals( 1, type.getValues().size() );
        verifyTypeValue( type, "siteRelative" );
    }

    private void verifyMediaIntentType()
    {
        GraphQLEnumType type = context.getEnumType( "MediaIntentType" );

        assertEquals( "MediaIntentType", type.getName() );
        assertEquals( "Media intent type.", type.getDescription() );

        assertEquals( 2, type.getValues().size() );
        verifyTypeValue( type, "download" );
        verifyTypeValue( type, "inline" );
    }

    private void verifyDslOperatorType()
    {
        GraphQLEnumType type = context.getEnumType( "DslOperatorType" );
        assertEquals( "DSL Operator type.", type.getDescription() );

        assertEquals( 2, type.getValues().size() );
        verifyTypeValue( type, "OR" );
        verifyTypeValue( type, "AND" );
    }

    private void verifyDslSortDirectionType()
    {
        GraphQLEnumType type = context.getEnumType( "DslSortDirectionType" );
        assertEquals( "DSL sort direction type.", type.getDescription() );

        assertEquals( 2, type.getValues().size() );
        verifyTypeValue( type, "ASC" );
        verifyTypeValue( type, "DESC" );
    }

    private void verifyHighlightEncoderType()
    {
        GraphQLEnumType type = context.getEnumType( "HighlightEncoderType" );
        assertEquals( "Indicates if the snippet should be HTML encoded: default (no encoding) or html.", type.getDescription() );

        assertEquals( 2, type.getValues().size() );
        verifyTypeValue( type, "default" );
        verifyTypeValue( type, "html" );
    }

    private void verifyHighlightTagsSchemaType()
    {
        GraphQLEnumType type = context.getEnumType( "HighlightTagsSchemaType" );
        assertEquals( "Set to styled to use the built-in tag schema.", type.getDescription() );

        assertEquals( 1, type.getValues().size() );
        verifyTypeValue( type, "styled" );
    }

    private void verifyHighlightFragmenterType()
    {
        GraphQLEnumType type = context.getEnumType( "HighlightFragmenterType" );
        assertEquals( "Specifies how text should be broken up in highlight snippets: simple or span (default).", type.getDescription() );

        assertEquals( 2, type.getValues().size() );
        verifyTypeValue( type, "simple" );
        verifyTypeValue( type, "span" );
    }

    private void verifyHighlightOrderType()
    {
        GraphQLEnumType type = context.getEnumType( "HighlightOrderType" );
        assertEquals(
            "Sorts highlighted fragments by score when set to score. Defaults to none - will be displayed in the same order in which fragments appear in the property.",
            type.getDescription() );

        assertEquals( 2, type.getValues().size() );
        verifyTypeValue( type, "score" );
        verifyTypeValue( type, "none" );
    }

    private void verifyDslGeoPointDistanceType()
    {
        GraphQLEnumType type = context.getEnumType( "DslGeoPointDistanceType" );
        assertEquals( "DSL Geo Point Distance type.", type.getDescription() );

        assertEquals( 19, type.getValues().size() );
        verifyTypeValue( type, "m" );
        verifyTypeValue( type, "meters" );
        verifyTypeValue( type, "in" );
        verifyTypeValue( type, "inch" );
        verifyTypeValue( type, "yd" );
        verifyTypeValue( type, "yards" );
        verifyTypeValue( type, "ft" );
        verifyTypeValue( type, "feet" );
        verifyTypeValue( type, "km" );
        verifyTypeValue( type, "kilometers" );
        verifyTypeValue( type, "NM" );
        verifyTypeValue( type, "nmi" );
        verifyTypeValue( type, "nauticalmiles" );
        verifyTypeValue( type, "mm" );
        verifyTypeValue( type, "millimeters" );
        verifyTypeValue( type, "cm" );
        verifyTypeValue( type, "centimeters" );
        verifyTypeValue( type, "mi" );
        verifyTypeValue( type, "miles" );
    }

    private void verifyPrincipalType()
    {
        GraphQLEnumType type = context.getEnumType( "PrincipalType" );
        assertEquals( "Principal type.", type.getDescription() );

        assertEquals( 3, type.getValues().size() );
        verifyTypeValue( type, "user" );
        verifyTypeValue( type, "group" );
        verifyTypeValue( type, "role" );
    }

    private void verifyPermission()
    {
        GraphQLEnumType type = context.getEnumType( "Permission" );
        assertEquals( "Permission.", type.getDescription() );

        assertEquals( 7, type.getValues().size() );
        verifyTypeValue( type, "READ" );
        verifyTypeValue( type, "CREATE" );
        verifyTypeValue( type, "MODIFY" );
        verifyTypeValue( type, "DELETE" );
        verifyTypeValue( type, "PUBLISH" );
        verifyTypeValue( type, "READ_PERMISSIONS" );
        verifyTypeValue( type, "WRITE_PERMISSIONS" );
    }

    private void verifyFormItemType()
    {
        GraphQLEnumType type = context.getEnumType( "FormItemType" );
        assertEquals( "Form item type", type.getDescription() );

        assertEquals( 4, type.getValues().size() );
        verifyTypeValue( type, "ItemSet" );
        verifyTypeValue( type, "Layout" );
        verifyTypeValue( type, "Input" );
        verifyTypeValue( type, "OptionSet" );
    }

    private void verifyComponentType()
    {
        GraphQLEnumType type = context.getEnumType( "ComponentType" );
        assertEquals( "Component type.", type.getDescription() );

        assertEquals( 6, type.getValues().size() );
        verifyTypeValue( type, "page" );
        verifyTypeValue( type, "layout" );
        verifyTypeValue( type, "image" );
        verifyTypeValue( type, "part" );
        verifyTypeValue( type, "text" );
        verifyTypeValue( type, "fragment" );
    }

    private void verifyTypeValue( GraphQLEnumType type, String value )
    {
        assertEquals( value, type.getValue( value ).getName() );
        assertEquals( value, type.getValue( value ).getValue() );
    }

}
