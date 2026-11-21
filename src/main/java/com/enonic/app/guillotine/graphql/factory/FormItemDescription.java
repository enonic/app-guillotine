package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.util.GenericValue;

/**
 * Where the value of a form item of a content type is indexed, used to describe the GraphQL field generated for it: the form label
 * and help text, the index path to use in {@code queryDsl} and, for selectors, the content types the selector accepts.
 */
final class FormItemDescription
{
    private static final List<String> IMAGE_TARGETS = List.of( "media:image", "media:vector" );

    private static final List<String> MEDIA_TARGETS = List.of( "media:*" );

    private static final Pattern FULL_EXPRESSION = Pattern.compile( "[:\\\\^$.|?*+()\\[{]" );

    private final String indexPath;

    private final ApplicationKey applicationKey;

    private FormItemDescription( final String indexPath, final ApplicationKey applicationKey )
    {
        this.indexPath = indexPath;
        this.applicationKey = applicationKey;
    }

    /**
     * The scope of the {@code data} of a content type of {@code applicationKey}.
     */
    static FormItemDescription data( final ApplicationKey applicationKey )
    {
        return new FormItemDescription( "data", applicationKey );
    }

    FormItemDescription child( final String rawName )
    {
        return new FormItemDescription( indexPath + "." + rawName, applicationKey );
    }

    String indexPath()
    {
        return indexPath;
    }

    /**
     * The description of the field generated for {@code formItem}, whose index path is this one, or {@code null} for an item that
     * does not hold a value of its own, such as a form fragment that was not inlined.
     */
    String describe( final FormItem formItem )
    {
        if ( !( formItem instanceof Input || formItem instanceof FormItemSet || formItem instanceof FormOptionSet ||
            formItem instanceof FormOptionSetOption ) )
        {
            return null;
        }
        final List<String> lines = new ArrayList<>();
        final String label = label( formItem );
        if ( label != null && !label.isBlank() )
        {
            lines.add( label.trim() );
        }
        final String helpText = helpText( formItem );
        if ( helpText != null && !helpText.isBlank() )
        {
            lines.add( "Help text: " + helpText.trim() );
        }
        lines.add( "Index path: " + indexPath );
        if ( formItem instanceof Input input )
        {
            final List<String> targets = targets( input );
            if ( !targets.isEmpty() )
            {
                lines.add( "Allowed content types: " + String.join( ", ", targets ) );
            }
        }
        return String.join( "\n", lines );
    }

    private static String label( final FormItem formItem )
    {
        if ( formItem instanceof Input input )
        {
            return input.getLabel();
        }
        if ( formItem instanceof FormItemSet set )
        {
            return set.getLabel();
        }
        if ( formItem instanceof FormOptionSet optionSet )
        {
            return optionSet.getLabel();
        }
        if ( formItem instanceof FormOptionSetOption option )
        {
            return option.getLabel();
        }
        return null;
    }

    private static String helpText( final FormItem formItem )
    {
        if ( formItem instanceof Input input )
        {
            return input.getHelpText();
        }
        if ( formItem instanceof FormItemSet set )
        {
            return set.getHelpText();
        }
        if ( formItem instanceof FormOptionSet optionSet )
        {
            return optionSet.getHelpText();
        }
        if ( formItem instanceof FormOptionSetOption option )
        {
            return option.getHelpText();
        }
        return null;
    }

    /**
     * The content types a selector accepts, as Content Studio offers them. A content selector takes its {@code allowContentType}
     * setting, qualified the way XP's {@code ApplicationWildcardMatcher} does: {@code ${app}} is the content type's application, and a
     * plain name gets it as prefix; without the setting it has none. A media selector keeps only the {@code media:} entries of the
     * setting and accepts any media without them. An image selector ignores the setting and accepts images. Other inputs have none.
     */
    List<String> targets( final Input input )
    {
        final InputTypeName inputType = input.getInputType();
        if ( InputTypeName.IMAGE_SELECTOR.equals( inputType ) )
        {
            return IMAGE_TARGETS;
        }
        if ( InputTypeName.MEDIA_SELECTOR.equals( inputType ) )
        {
            final List<String> media = allowContentType( input ).stream().filter( type -> type.startsWith( "media:" ) ).toList();
            return media.isEmpty() ? MEDIA_TARGETS : media;
        }
        if ( InputTypeName.CONTENT_SELECTOR.equals( inputType ) )
        {
            return allowContentType( input ).stream()
                .map( type -> FULL_EXPRESSION.matcher( type ).find() ? type : "${app}:" + type )
                .map( type -> type.replace( "${app}", applicationKey.toString() ) )
                .toList();
        }
        return List.of();
    }

    private static List<String> allowContentType( final Input input )
    {
        final GenericValue config = input.getInputTypeConfig();
        final GenericValue allowed = config != null ? config.optional( "allowContentType" ).orElse( null ) : null;
        if ( allowed == null )
        {
            return List.of();
        }
        final List<GenericValue> values = allowed.getType() == GenericValue.Type.LIST ? allowed.values() : List.of( allowed );
        final List<String> types = new ArrayList<>();
        for ( final GenericValue value : values )
        {
            if ( value.getType() == GenericValue.Type.STRING && !value.asString().isBlank() )
            {
                types.add( value.asString().trim() );
            }
        }
        return types;
    }
}
