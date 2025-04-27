package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.GetContentByIdsParams;

final class ListExtractionStrategy
    implements ExtractionStrategy
{
    private final ContentService contentService;

    ListExtractionStrategy( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Content> extract( final Object jsApiResult )
    {
        if ( jsApiResult instanceof List )
        {
            final List<Map<String, Object>> listContentAsMap = (List<Map<String, Object>>) jsApiResult;

            final List<ContentId> contentIds =
                listContentAsMap.stream().map( content -> (String) content.get( "_id" ) ).map( ContentId::from ).toList();

            if ( contentIds.isEmpty() )
            {
                return Collections.emptyList();
            }

            return contentService.getByIds( new GetContentByIdsParams( ContentIds.from( contentIds ) ) ).stream().toList();
        }

        return Collections.emptyList();
    }
}
