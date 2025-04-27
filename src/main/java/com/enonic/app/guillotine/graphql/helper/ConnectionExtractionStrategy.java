package com.enonic.app.guillotine.graphql.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.GetContentByIdsParams;

final class ConnectionExtractionStrategy
    implements ExtractionStrategy
{
    private final ContentService contentService;

    ConnectionExtractionStrategy( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Content> extract( final Object jsApiResult )
    {
        if ( !( jsApiResult instanceof Map ) )
        {
            return Collections.emptyList();
        }

        final Map<String, Object> connectionAsMap = (Map<String, Object>) jsApiResult;

        final List<Map<String, Object>> edges = (List<Map<String, Object>>) connectionAsMap.get( "edges" );

        if ( edges == null )
        {
            return Collections.emptyList();
        }

        final List<ContentId> contentIds =
            edges.stream().map( edge -> (Map<String, Object>) edge.get( "node" ) ).map( node -> (String) node.get( "_id" ) ).filter(
                Objects::nonNull ).map( ContentId::from ).toList();

        if ( contentIds.isEmpty() )
        {
            return Collections.emptyList();
        }

        return contentService.getByIds( new GetContentByIdsParams( ContentIds.from( contentIds ) ) ).stream().toList();
    }
}
