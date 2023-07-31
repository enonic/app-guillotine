package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;

public class GetContentReferencesDataFetcher
    implements DataFetcher<Object>
{
    private final ContentService contentService;

    public GetContentReferencesDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> contentAsMap = environment.getSource();

        return contentService.getOutboundDependencies( ContentId.from( contentAsMap.get( "_id" ).toString() ) ).stream().map( contentId -> {
            Content content = contentService.getById( contentId );
            return ContentSerializer.serialize( content );
        } ).collect( Collectors.toList() );
    }
}
