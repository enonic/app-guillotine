package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;

final class SingleExtractionStrategy
    implements ExtractionStrategy
{
    private final ContentService contentService;

    SingleExtractionStrategy( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Content> extract( final Object jsApiResult )
    {
        if ( jsApiResult instanceof Map )
        {
            final Map<String, Object> contentAsMap = (Map<String, Object>) jsApiResult;
            final String contentId = (String) contentAsMap.get( "_id" );
            return List.of( this.contentService.getById( ContentId.from( contentId ) ) );
        }

        return Collections.emptyList();
    }
}
