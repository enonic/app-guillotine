package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeUtil;

import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.util.GenericValue;

/**
 * The data fields of a content type: its form walked the way Guillotine generates the {@code data} type, keeping only items that have a
 * GraphQL field. Each field carries its GraphQL selection path and its index path, which keeps the raw form item names.
 */
final class TypeFields
{
    static final String ITEM_SET = "ItemSet";

    static final String OPTION_SET = "OptionSet";

    private static final String SELECTED = "._selected";

    private static final int MAX_HELP_CHARS = 160;

    private static final Set<String> SELECTORS =
        Set.of( InputTypeName.CONTENT_SELECTOR.toString(), InputTypeName.IMAGE_SELECTOR.toString(),
                InputTypeName.MEDIA_SELECTOR.toString() );

    private static final List<String> IMAGE_TARGETS = List.of( "media:image", "media:vector" );

    private TypeFields()
    {
    }

    /**
     * One data field. {@code name} is the GraphQL selection path below {@code data} and {@code path} the index path for filters, sorts
     * and aggregations. {@code type} is the GraphQL output type of the field.
     */
    record Field(String name, String path, String label, String help, String input, boolean repeated, boolean required,
                 List<String> targets, GraphQLOutputType type, boolean nested)
    {
        Map<String, Object> toMap()
        {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put( "name", name );
            map.put( "path", path );
            final String item = name.endsWith( SELECTED ) ? name.substring( 0, name.length() - SELECTED.length() ) : name;
            final String last = item.substring( item.lastIndexOf( '.' ) + 1 );
            if ( label != null && !label.isBlank() && !label.equalsIgnoreCase( last ) )
            {
                map.put( "label", label );
            }
            map.put( "help", help );
            map.put( "input", input );
            if ( repeated )
            {
                map.put( "repeated", true );
            }
            if ( required )
            {
                map.put( "required", true );
            }
            map.put( "targets", targets );
            return map;
        }

        /**
         * Whether the field holds ids of other content: a content, image or media selector. Uploaders are typed as content in GraphQL
         * but store the file in the item itself.
         */
        boolean isReference()
        {
            return SELECTORS.contains( input );
        }
    }

    /**
     * The data fields of {@code contentType}, whose GraphQL type {@code type} is; none when either is {@code null}.
     */
    static List<Field> of( final ContentType contentType, final GraphQLObjectType type )
    {
        if ( contentType == null || type == null )
        {
            return List.of();
        }
        return of( contentType.getForm(), dataType( type ), contentType.getName().getApplicationKey().toString() );
    }

    /**
     * The GraphQL type of the {@code data} field of a content type's GraphQL type, or {@code null}.
     */
    static GraphQLObjectType dataType( final GraphQLObjectType type )
    {
        return type.getFieldDefinition( "data" ) != null &&
            GraphQLTypeUtil.unwrapAll( type.getFieldDefinition( "data" ).getType() ) instanceof GraphQLObjectType data ? data : null;
    }

    static List<Field> of( final Iterable<? extends FormItem> form, final GraphQLObjectType dataType, final String applicationKey )
    {
        final List<Field> result = new ArrayList<>();
        if ( dataType != null )
        {
            walk( form, dataType, "", "data.", false, applicationKey, result );
        }
        return result;
    }

    private static void walk( final Iterable<? extends FormItem> items, final GraphQLObjectType parent, final String namePrefix,
                              final String pathPrefix, final boolean nested, final String applicationKey, final List<Field> result )
    {
        for ( final FormItem item : FormItemTypesHelper.getFilteredFormItems( items ) )
        {
            final String graphQlName = StringNormalizer.create( item.getName() );
            final GraphQLFieldDefinition definition = parent.getFieldDefinition( graphQlName );
            if ( definition == null )
            {
                continue;
            }
            final String name = namePrefix + graphQlName;
            final String path = pathPrefix + item.getName();
            final boolean repeated = FormItemTypesHelper.getOccurrences( item ).getMaximum() != 1;
            final boolean required = FormItemTypesHelper.getOccurrences( item ).getMinimum() > 0;
            final GraphQLOutputType type = definition.getType();

            if ( item instanceof Input input )
            {
                result.add( new Field( name, path, input.getLabel(), help( input.getHelpText() ), input.getInputType().toString(), repeated,
                                       required, targets( input, applicationKey ), type, nested ) );
            }
            else if ( item instanceof FormItemSet set && GraphQLTypeUtil.unwrapAll( type ) instanceof GraphQLObjectType setType )
            {
                result.add( new Field( name, path, set.getLabel(), help( set.getHelpText() ), ITEM_SET, repeated, required, null, type,
                                       nested ) );
                walk( set, setType, name + ".", path + ".", true, applicationKey, result );
            }
            else if ( item instanceof FormOptionSet optionSet &&
                GraphQLTypeUtil.unwrapAll( type ) instanceof GraphQLObjectType optionSetType )
            {
                result.add( new Field( name + SELECTED, path + SELECTED, optionSet.getLabel(), help( optionSet.getHelpText() ),
                                       OPTION_SET, repeated || optionSet.getMultiselection().getMaximum() != 1, required, null, type,
                                       nested ) );
                for ( final FormOptionSetOption option : optionSet )
                {
                    final String optionName = StringNormalizer.create( option.getName() );
                    if ( optionSetType.getFieldDefinition( optionName ) != null &&
                        GraphQLTypeUtil.unwrapAll( optionSetType.getFieldDefinition( optionName ).getType() ) instanceof
                            GraphQLObjectType optionType )
                    {
                        walk( option, optionType, name + "." + optionName + ".", path + "." + option.getName() + ".", true,
                              applicationKey, result );
                    }
                }
            }
        }
    }

    private static String help( final String help )
    {
        return help == null || help.isBlank() ? null : McpJson.shorten( help.trim(), MAX_HELP_CHARS );
    }

    /**
     * The content types a selector accepts, from its {@code allowContentType} setting; an image selector accepts images by default. A
     * selector without the setting accepts any type, which is {@code null}.
     */
    static List<String> targets( final Input input, final String applicationKey )
    {
        final InputTypeName inputType = input.getInputType();
        final boolean image = InputTypeName.IMAGE_SELECTOR.equals( inputType );
        if ( !image && !InputTypeName.CONTENT_SELECTOR.equals( inputType ) && !InputTypeName.MEDIA_SELECTOR.equals( inputType ) )
        {
            return null;
        }
        final List<String> configured = new ArrayList<>();
        final GenericValue config = input.getInputTypeConfig();
        final GenericValue allowed = config != null ? config.optional( "allowContentType" ).orElse( null ) : null;
        if ( allowed != null )
        {
            final List<GenericValue> values = allowed.getType() == GenericValue.Type.LIST ? allowed.values() : List.of( allowed );
            for ( final GenericValue value : values )
            {
                if ( value.getType() == GenericValue.Type.STRING && !value.asString().isBlank() )
                {
                    final String target = value.asString().trim().replace( "${app}", applicationKey );
                    configured.add( target.contains( ":" ) || !target.matches( "[\\w.\\-]+" ) ? target : applicationKey + ":" + target );
                }
            }
        }
        if ( !configured.isEmpty() )
        {
            return configured;
        }
        return image ? IMAGE_TARGETS : null;
    }
}
