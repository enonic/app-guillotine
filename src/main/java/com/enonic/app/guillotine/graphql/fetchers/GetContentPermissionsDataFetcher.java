package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.SecurityHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;

public class GetContentPermissionsDataFetcher
    implements DataFetcher<Object>
{

    private final ContentService contentService;

    public GetContentPermissionsDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        if ( !SecurityHelper.canAccessCmsData() )
        {
            throw new IllegalAccessException( "Unauthorized" );
        }

        Map<String, Object> sourceAsMap = environment.getSource();
        Content content = new GetContentCommand( contentService ).executeAndGetContent( sourceAsMap.get( "_id" ).toString(), environment );
        return GuillotineSerializer.serializePermissions( content );
    }
}
