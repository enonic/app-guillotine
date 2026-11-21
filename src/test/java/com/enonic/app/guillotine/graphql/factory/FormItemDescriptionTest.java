package com.enonic.app.guillotine.graphql.factory;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.util.GenericValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FormItemDescriptionTest
{
    private static final FormItemDescription DATA = FormItemDescription.data( ApplicationKey.from( "com.example.myapp" ) );

    @Test
    public void indexPathFollowsRawNames()
    {
        assertEquals( "data.my-set.my_field", DATA.child( "my-set" ).child( "my_field" ).indexPath() );
    }

    @Test
    public void missingLabelIsLeftOut()
    {
        final FormItemSet set = FormItemSet.create()
            .name( "address" )
            .addFormItem( Input.create().name( "street" ).label( "Street" ).inputType( InputTypeName.TEXT_LINE ).build() )
            .build();

        assertEquals( "Index path: data.address", DATA.child( "address" ).describe( set ) );
    }

    @Test
    public void itemSetHasLabelAndPath()
    {
        final FormItemSet set = FormItemSet.create()
            .name( "address" )
            .label( "Address" )
            .addFormItem( Input.create().name( "street" ).label( "Street" ).inputType( InputTypeName.TEXT_LINE ).build() )
            .build();

        assertEquals( "Address\nIndex path: data.address", DATA.child( "address" ).describe( set ) );
    }

    @Test
    public void helpTextFollowsLabel()
    {
        final Input input = Input.create()
            .name( "author" )
            .label( "Author" )
            .helpText( "  The person who wrote the article.  " )
            .inputType( InputTypeName.TEXT_LINE )
            .build();

        assertEquals( "Author\nHelp text: The person who wrote the article.\nIndex path: data.author",
                      DATA.child( "author" ).describe( input ) );
    }

    @Test
    public void blankHelpTextIsLeftOut()
    {
        final FormItemSet set = FormItemSet.create()
            .name( "address" )
            .label( "Address" )
            .helpText( " " )
            .addFormItem( Input.create().name( "street" ).label( "Street" ).inputType( InputTypeName.TEXT_LINE ).build() )
            .build();

        assertEquals( "Address\nIndex path: data.address", DATA.child( "address" ).describe( set ) );
    }

    @Test
    public void selectorTargetsResolveTheApplication()
    {
        final Input input = selector( InputTypeName.CONTENT_SELECTOR, GenericValue.newObject()
            .put( "allowContentType",
                  GenericValue.newList()
                      .add( GenericValue.stringValue( "person" ) )
                      .add( GenericValue.stringValue( "${app}:article" ) )
                      .add( GenericValue.stringValue( "base:folder" ) )
                      .add( GenericValue.stringValue( "com.other:*" ) )
                      .build() )
            .build() );

        assertEquals( List.of( "com.example.myapp:person", "com.example.myapp:article", "base:folder", "com.other:*" ),
                      DATA.targets( input ) );
        assertEquals( "Author\nIndex path: data.author\n" +
                          "Allowed content types: com.example.myapp:person, com.example.myapp:article, base:folder, com.other:*",
                      DATA.child( "author" ).describe( input ) );
    }

    @Test
    public void expressionsAreNotPrefixed()
    {
        final Input input = selector( InputTypeName.CONTENT_SELECTOR, GenericValue.newObject()
            .put( "allowContentType", GenericValue.newList()
                .add( GenericValue.stringValue( "${app}:*" ) )
                .add( GenericValue.stringValue( "landing.page" ) )
                .build() )
            .build() );

        assertEquals( List.of( "com.example.myapp:*", "landing.page" ), DATA.targets( input ) );
    }

    @Test
    public void imageSelectorAcceptsImages()
    {
        assertEquals( List.of( "media:image", "media:vector" ), DATA.targets( selector( InputTypeName.IMAGE_SELECTOR, null ) ) );
    }

    @Test
    public void imageSelectorIgnoresSetting()
    {
        final Input input = selector( InputTypeName.IMAGE_SELECTOR, GenericValue.newObject()
            .put( "allowContentType", GenericValue.newList().add( GenericValue.stringValue( "media:vector" ) ).build() )
            .build() );

        assertEquals( List.of( "media:image", "media:vector" ), DATA.targets( input ) );
    }

    @Test
    public void mediaSelectorDefaultsToAnyMedia()
    {
        assertEquals( List.of( "media:*" ), DATA.targets( selector( InputTypeName.MEDIA_SELECTOR, null ) ) );
    }

    @Test
    public void mediaSelectorKeepsOnlyMediaTypes()
    {
        final Input input = selector( InputTypeName.MEDIA_SELECTOR, GenericValue.newObject()
            .put( "allowContentType", GenericValue.newList()
                .add( GenericValue.stringValue( "media:document" ) )
                .add( GenericValue.stringValue( "person" ) )
                .add( GenericValue.stringValue( "media:audio" ) )
                .build() )
            .build() );

        assertEquals( List.of( "media:document", "media:audio" ), DATA.targets( input ) );
    }

    @Test
    public void mediaSelectorWithoutMediaTypesAcceptsAnyMedia()
    {
        final Input input = selector( InputTypeName.MEDIA_SELECTOR, GenericValue.newObject()
            .put( "allowContentType", GenericValue.newList().add( GenericValue.stringValue( "${app}:person" ) ).build() )
            .build() );

        assertEquals( List.of( "media:*" ), DATA.targets( input ) );
    }

    @Test
    public void contentSelectorWithoutSettingHasNoTargets()
    {
        assertTrue( DATA.targets( selector( InputTypeName.CONTENT_SELECTOR, null ) ).isEmpty() );
        assertEquals( "Author\nIndex path: data.author",
                      DATA.child( "author" ).describe( selector( InputTypeName.CONTENT_SELECTOR, null ) ) );
    }

    @Test
    public void otherInputsHaveNoTargets()
    {
        final Input input = Input.create()
            .name( "title" )
            .label( "Title" )
            .inputType( InputTypeName.TEXT_LINE )
            .inputTypeConfig( GenericValue.newObject().put( "allowContentType", "person" ).build() )
            .build();

        assertTrue( DATA.targets( input ).isEmpty() );
    }

    private static Input selector( final InputTypeName type, final GenericValue config )
    {
        final Input.Builder builder = Input.create().name( "author" ).label( "Author" ).inputType( type );
        if ( config != null )
        {
            builder.inputTypeConfig( config );
        }
        return builder.build();
    }
}
