package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.ContentTypeMapper;
import com.enonic.xp.form.Form;
import com.enonic.xp.schema.content.CmsFormFragmentService;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;

public class ContentTypeDataFetcher
    implements DataFetcher<Object>
{
    private final CmsFormFragmentService cmsFormFragmentService;

    private final ContentTypeService contentTypeService;

    public ContentTypeDataFetcher( final CmsFormFragmentService cmsFormFragmentService, final ContentTypeService contentTypeService )
    {
        this.cmsFormFragmentService = cmsFormFragmentService;
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

        Form inlinedForm = cmsFormFragmentService.inlineFormItems( contentType.getForm() );
        if ( inlinedForm == null )
        {
            return contentType;
        }

        return builder.form( inlinedForm ).build();
    }
}
