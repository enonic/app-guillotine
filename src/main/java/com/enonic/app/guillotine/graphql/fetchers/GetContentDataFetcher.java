package com.enonic.app.guillotine.graphql.fetchers;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetContentDataFetcher
    extends BaseContentDataFetcher
{
    public GetContentDataFetcher( final ContentService contentService, final PortalUrlService portalUrlService )
    {
        super( contentService, portalUrlService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> {
            final Content content = getContent( environment );
            return GuillotineSerializer.serialize( content );
        } );
    }
}
