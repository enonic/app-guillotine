package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.ContentTypeMapper;
import com.enonic.xp.form.Form;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;
import com.enonic.xp.schema.mixin.MixinService;

public class ContentTypeDataFetcher
    implements DataFetcher<Object>
{
    private final MixinService mixinService;

    private final ContentTypeService contentTypeService;

    public ContentTypeDataFetcher( final MixinService mixinService, final ContentTypeService contentTypeService )
    {
        this.mixinService = mixinService;
        this.contentTypeService = contentTypeService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> contentAsMap = environment.getSource();
        ContentType contentType =
            contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.from( contentAsMap.get( "type" ).toString() ) ) );

        if ( contentType != null )
        {
            GuillotineMapGenerator generator = new GuillotineMapGenerator();
            new ContentTypeMapper( inlineMixins( contentType ) ).serialize( generator );
            return generator.getRoot();
        }
        return null;
    }

    private ContentType inlineMixins( final ContentType contentType )
    {
        ContentType.Builder builder = ContentType.create( contentType );

        Form inlinedForm = mixinService.inlineFormItems( contentType.getForm() );
        if ( inlinedForm == null )
        {
            return contentType;
        }

        return builder.form( inlinedForm ).build();
    }
}
