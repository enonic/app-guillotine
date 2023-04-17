package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.CastHelper;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.SecurityHelper;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.xp.content.ContentService;

public class GetContentParentDataFetcher
    implements DataFetcher<Object>
{
    private final ContentService contentService;

    private final GuillotineContext guillotineContext;

    public GetContentParentDataFetcher( final ContentService contentService, final GuillotineContext guillotineContext )
    {
        this.contentService = contentService;
        this.guillotineContext = guillotineContext;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        String parentPath = resolveParentPath( environment );

        if ( parentPath == null )
        {
            return null;
        }

        Object parent = new GetContentCommand( contentService ).execute( parentPath );

        return SecurityHelper.filterForbiddenContent( CastHelper.castToMap( parent ), guillotineContext );
    }

    private String resolveParentPath( DataFetchingEnvironment environment )
    {
        Map<String, Object> contentAsMap = environment.getSource();

        String path = contentAsMap.get( "_path" ).toString();

        int lastSlashIndexOf = path.lastIndexOf( "/" );

        if ( lastSlashIndexOf == 0 )
        {
            return null;
        }

        return path.substring( 0, lastSlashIndexOf );
    }
}
