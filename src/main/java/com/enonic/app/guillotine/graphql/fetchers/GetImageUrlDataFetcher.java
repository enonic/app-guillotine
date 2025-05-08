package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.Media;
import com.enonic.xp.portal.url.ImageUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetImageUrlDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetImageUrlDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
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

        final ImageUrlGeneratorParams.Builder builder = ImageUrlGeneratorParams.create();

        builder.setMedia( () -> (Media) content );
        builder.setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) );
        builder.setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) );
        builder.setBaseUrl( GuillotineLocalContextHelper.getSiteBaseUrl( environment ) );
        builder.setScale( environment.getArgument( "scale" ) );
        builder.setUrlType( environment.getArgument( "type" ) );
        builder.setQuality( environment.getArgument( "quality" ) );
        builder.setBackground( environment.getArgument( "background" ) );
        builder.setFormat( environment.getArgument( "format" ) );
        builder.setFilter( environment.getArgument( "filter" ) );

        if ( environment.getArgument( "params" ) instanceof Map queryParams )
        {
            builder.addQueryParams( ParamsUrHelper.convertToMultimap( queryParams ) );
        }

        final ImageUrlGeneratorParams params = builder.build();

        return portalUrlService.imageUrl( params );
    }
}
