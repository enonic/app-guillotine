package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.site.Site;

public class GetContentSiteDataFetcher
    implements DataFetcher<Object>
{

    private final ContentService contentService;

    public GetContentSiteDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        Map<String, Object> sourceMap = environment.getSource();

        Site site = contentService.getNearestSite( ContentId.from( sourceMap.get( "_id" ).toString() ) );

        return GuillotineSerializer.serialize( site );
    }
}
