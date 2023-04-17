package com.enonic.app.guillotine.graphql;

import java.util.List;

import graphql.schema.GraphQLEnumType;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.newEnum;

public class EnumTypesFactory
{
    private final GuillotineContext context;

    public EnumTypesFactory( GuillotineContext context )
    {
        this.context = context;
    }

    public void create()
    {
        registerEnum( "UrlType", "URL type.", List.of( "server", "absolute" ) );

        registerEnum( "ContentPathType", "Content path type.", List.of( "siteRelative" ) );

        registerEnum( "MediaIntentType", "Media intent type.", List.of( "download", "inline" ) );

        registerEnum( "DslOperatorType", "DSL Operator type.", List.of( "OR", "AND" ) );

        registerEnum( "DslSortDirectionType", "DSL sort direction type.", List.of( "ASC", "DESC" ) );

        registerEnum( "HighlightEncoderType", "Indicates if the snippet should be HTML encoded: default (no encoding) or html.",
                      List.of( "default", "html" ) );

        registerEnum( "HighlightTagsSchemaType", "Set to styled to use the built-in tag schema.", List.of( "styled" ) );

        registerEnum( "HighlightFragmenterType", "Specifies how text should be broken up in highlight snippets: simple or span (default).",
                      List.of( "simple", "span" ) );

        registerEnum( "HighlightOrderType",
                      "Sorts highlighted fragments by score when set to score. Defaults to none - will be displayed in the same order in which fragments appear in the property.",
                      List.of( "score", "none" ) );

        registerEnum( "DslGeoPointDistanceType", "DSL Geo Point Distance type.",
                      List.of( "m", "meters", "in", "inch", "yd", "yards", "ft", "feet", "km", "kilometers", "NM", "nmi", "nauticalmiles",
                               "mm", "millimeters", "cm", "centimeters", "mi", "miles" ) );

        registerEnum( "PrincipalType", "Principal type.", List.of( "user", "group", "role" ) );
        registerEnum( "Permission", "Permission.",
                      List.of( "READ", "CREATE", "MODIFY", "DELETE", "PUBLISH", "READ_PERMISSIONS", "WRITE_PERMISSIONS" ) );

        registerEnum( "FormItemType", "Form item type", List.of( "ItemSet", "Layout", "Input", "OptionSet" ) );

        registerEnum( "ComponentType", "Component type.", List.of( "page", "layout", "image", "part", "text", "fragment" ) );
    }

    private void registerEnum( String name, String description, List<String> values )
    {
        GraphQLEnumType enumType = newEnum( context.uniqueName( name ), description, values );
        context.registerType( enumType.getName(), enumType );
    }
}
