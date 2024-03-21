package com.enonic.app.guillotine.graphql.factory;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.ComponentDescriptorService;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeConfig;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.inputtype.InputTypeProperty;
import com.enonic.xp.macro.MacroDescriptors;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.mixin.MixinService;
import com.enonic.xp.schema.xdata.XDatas;
import com.enonic.xp.security.PrincipalKey;

public class TypeFactoryTest
{
    @Test
    public void testCreate()
    {
        ServiceFacade serviceFacade = Mockito.mock( ServiceFacade.class );
        ComponentDescriptorService componentDescriptorService = Mockito.mock( ComponentDescriptorService.class );
        ContentTypeService contentTypeService = Mockito.mock( ContentTypeService.class );

        Mockito.when( componentDescriptorService.getMacroDescriptors( Mockito.anyList() ) ).thenReturn( MacroDescriptors.empty() );

        Mockito.when( componentDescriptorService.getExtraData( Mockito.anyString() ) ).thenReturn(
            XDatas.from( TestFixtures.GPS_METADATA ) );

        Mockito.when( contentTypeService.getAll() ).thenReturn( createContentTypes() );

        Mockito.when( serviceFacade.getComponentDescriptorService() ).thenReturn( componentDescriptorService );
        Mockito.when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

		MixinService mixinService = Mockito.mock( MixinService.class );
		Mockito.when( mixinService.inlineFormItems( Mockito.any() ) ).thenReturn( null );
		Mockito.when( serviceFacade.getMixinService() ).thenReturn( mixinService );

        GuillotineContext context = GuillotineContext.create().addApplications( List.of( "com.enonic.app.testapp" ) ).build();

        TypeFactory factory = new TypeFactory( context, serviceFacade );
        factory.createTypes();
        new HeadlessCmsTypeFactory( context, serviceFacade ).create();

        new EnumTypesVerifier( context ).verify();
        new AclTypesVerifier( context ).verify();
        new InputTypesVerifier( context ).verify();
        new GenericTypesVerifier( context ).verify();
        new FormTypesVerify( context ).verify();
        new ContentTypesVerifier( context ).verify();
        new HeadlessCmsTypeVerifier( context ).verify();
    }

    private ContentTypes createContentTypes()
    {
        ContentType contentType = ContentType.create()
            .name( ContentTypeName.media() )
            .form( Form.create().build() )
            .setAbstract()
            .setFinal()
            .allowChildContent(true )
            .setBuiltIn()
            .displayNameExpression( "displayNameExpression" )
            .displayName( "displayName" )
            .description("description" )
            .modifiedTime( Instant.ofEpochSecond( 1000 ) )
            .createdTime( Instant.ofEpochSecond( 1000 ) )
            .creator( PrincipalKey.ofAnonymous() )
            .modifier( PrincipalKey.ofAnonymous() ).build();

        FieldSet fieldSet = FieldSet.create()
            .label( "My layout" )
            .name( "myLayout" )
            .addFormItem( FormItemSet.create()
                              .name( "mySet" )
                              .required( true )
                              .addFormItem( Input.create()
                                                .name( "myInput" )
                                                .label( "Input" )
                                                .inputType( InputTypeName.TEXT_LINE )
                                                .build()
                              ).build()
            ).build();

        ContentType myContentType = ContentType.create()
            .superType( ContentTypeName.structured() )
            .name( "com.enonic.app.testapp:my_type" )
            .addFormItem( fieldSet )
            .addFormItem( Input.create()
                              .name( "dateOfBirth" )
                              .label( "Birth of Date" )
                              .occurrences( 0, 1 )
                              .inputType( InputTypeName.DATE )
                              .build() )
            .addFormItem( FormOptionSet.create()
                              .name( "blocks" )
                              .occurrences( Occurrences.create( 1, 1 ) )
                              .addOptionSetOption( FormOptionSetOption.create()
                                                       .name( "text" )
                                                       .addFormItem(
                                                           Input.create()
                                                               .name( "text" )
                                                               .label( "Text" )
                                                               .occurrences( 1, 1 )
                                                               .inputType( InputTypeName.HTML_AREA )
                                                               .build()
                                                       )
                                                       .build()
                              )
                              .addOptionSetOption( FormOptionSetOption.create()
                                                       .name( "icon" )
                                                       .addFormItem(
                                                           Input.create()
                                                               .name( "icon" )
                                                               .label( "Icon" )
                                                               .occurrences( 1, 1 )
                                                               .inputType( InputTypeName.ATTACHMENT_UPLOADER )
                                                               .build()
                                                       )
                                                       .build()
                              )
                              .build() )
            .addFormItem( FormItemSet.create()
                              .name( "cast" )
                              .label( "Cast" )
                              .occurrences( 0, 0 )
                              .addFormItem(
                                  Input.create()
                                      .name( "actor" )
                                      .label( "Actor" )
                                      .occurrences( 1, 1 )
                                      .inputType( InputTypeName.CONTENT_SELECTOR )
                                      .inputTypeConfig( InputTypeConfig.create()
                                                            .property(
                                                                InputTypeProperty.create("allowContentType", "person").build()
                                                            ).build()
                                      ).build()
                              )
                              .addFormItem(
                                  Input.create()
                                      .name( "abstract" )
                                      .label( "Abstract" )
                                      .occurrences( 1, 1 )
                                      .inputType( InputTypeName.TEXT_AREA )
                                      .build()
                              )
                              .addFormItem(
                                  Input.create()
                                      .name( "photos" )
                                      .label( "Photos" )
                                      .occurrences( 0, 0 )
                                      .inputType( InputTypeName.IMAGE_SELECTOR )
                                      .build()
                              ).build()
            )
            .build();

        return ContentTypes.from( contentType, myContentType );
    }

}
