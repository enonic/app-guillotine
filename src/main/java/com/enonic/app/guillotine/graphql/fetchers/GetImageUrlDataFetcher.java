package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.ImageUrlParams;
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
        PortalRequest portalRequest = environment.getLocalContext();

        Map<String, Object> sourceAsMap = environment.getSource();

        ImageUrlParams params = new ImageUrlParams().portalRequest( portalRequest ).id( sourceAsMap.get( "_id" ).toString() );

        if ( environment.getArgument( "scale" ) != null )
        {
            params.scale( environment.getArgument( "scale" ) );
        }
        if ( environment.getArgument( "quality" ) != null )
        {
            params.quality( (Integer) environment.getArgument( "quality" ) );
        }
        if ( environment.getArgument( "background" ) != null )
        {
            params.background( environment.getArgument( "background" ) );
        }
        if ( environment.getArgument( "format" ) != null )
        {
            params.format( environment.getArgument( "format" ) );
        }
        if ( environment.getArgument( "filter" ) != null )
        {
            params.filter( environment.getArgument( "filter" ) );
        }
        if ( environment.getArgument( "type" ) != null )
        {
            params.type( environment.getArgument( "type" ) );
        }

        return portalUrlService.imageUrl( params );
    }
}
