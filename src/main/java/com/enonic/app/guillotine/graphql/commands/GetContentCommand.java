package com.enonic.app.guillotine.graphql.commands;

import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
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

    public Map<String, Object> execute( String key, DataFetchingEnvironment environment )
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doExecute( key ) );
    }

    private Map<String, Object> doExecute( String key )
    {
        if ( key == null || key.isEmpty() )
        {
            return null;
        }
        return key.startsWith( "/" ) ? getByPath( ContentPath.from( key ) ) : getById( ContentId.from( key ) );
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
