package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.PageUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;

public class GetPageUrlDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlGeneratorService portalUrlGeneratorService;

    private final ContentService contentService;

    public GetPageUrlDataFetcher( final PortalUrlGeneratorService portalUrlGeneratorService, final ContentService contentService )
    {
        this.portalUrlGeneratorService = portalUrlGeneratorService;
        this.contentService = contentService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    @SuppressWarnings("unchecked")
    private String doGet( final DataFetchingEnvironment environment )
    {
        final Content content = GuillotineLocalContextHelper.resolveContent( environment );

        if ( content == null )
        {
            return null;
        }

        final PageUrlGeneratorParams.Builder builder = PageUrlGeneratorParams.create()
            .setBaseUrl( GuillotineLocalContextHelper.getPageBaseUrl( environment ) )
            .setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) )
            .setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) )
            .setContent( () -> content )
            .setNearestSite( () -> contentService.getNearestSite( content.getId() ) );

        if ( environment.getArgument( "params" ) instanceof Map queryParams )
        {
            builder.setQueryParams( ParamsUrHelper.convertToMultimap( queryParams ) );
        }

        return portalUrlGeneratorService.pageUrl( builder.build() );
    }
}
