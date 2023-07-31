package com.enonic.app.guillotine.graphql.commands;

import java.util.Map;

import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;

public class GetContentCommand
{
    private final ContentService contentService;

    public GetContentCommand( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    public Map<String, Object> execute( String key )
    {
        if ( key == null )
        {
            return null;
        }
        else if ( key.startsWith( "/" ) )
        {
            return getByPath( ContentPath.from( key ) );
        }
        else
        {
            return getById( ContentId.from( key ) );
        }
    }

    private Map<String, Object> getByPath( ContentPath contentPath )
    {
        try
        {
            return ContentSerializer.serialize( contentService.getByPath( contentPath ) );
        }
        catch ( final ContentNotFoundException e )
        {
            return null;
        }
    }

    private Map<String, Object> getById( ContentId contentId )
    {
        try
        {
            return ContentSerializer.serialize( contentService.getById( contentId ) );
        }
        catch ( final ContentNotFoundException e )
        {
            return null;
        }
    }
}
