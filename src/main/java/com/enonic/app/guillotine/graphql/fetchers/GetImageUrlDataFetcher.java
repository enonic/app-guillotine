package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.app.guillotine.graphql.helper.PortalRequestHelper;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
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
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private String doGet( final DataFetchingEnvironment environment )
    {
        PortalRequest portalRequest = PortalRequestHelper.createPortalRequest( PortalRequestAccessor.get(), environment );

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

        ParamsUrHelper.resolveParams( params.getParams(), environment.getArgument( "params" ) );

        return portalUrlService.imageUrl( params );
    }
}
