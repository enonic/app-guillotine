package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.portal.url.PageUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetPageUrlDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetPageUrlDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private String doGet( final DataFetchingEnvironment environment )
    {
        final Content content = GuillotineLocalContextHelper.resolveContent( environment );

        if ( content == null )
        {
            return null;
        }

        final PageUrlParams params = new PageUrlParams().id( content.getId().toString() )
            .projectName( GuillotineLocalContextHelper.getProjectName( environment ).toString() )
            .branch( GuillotineLocalContextHelper.getBranch( environment ).getValue() );

        if ( environment.getArgument( "params" ) instanceof Map<?, ?> queryParams )
        {
            queryParams.forEach( ( key, value ) -> params.param( key.toString(), value ) );
        }

        return GuillotineLocalContextHelper.prependBaseUrl( GuillotineLocalContextHelper.getPageBaseUrl( environment ),
                                                            portalUrlService.pageUrl( params ) );
    }
}