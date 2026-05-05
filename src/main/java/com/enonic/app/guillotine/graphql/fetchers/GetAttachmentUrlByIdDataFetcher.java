package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetAttachmentUrlByIdDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetAttachmentUrlByIdDataFetcher( final PortalUrlService portalUrlService )
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
        Map<String, Object> sourceAsMap = environment.getSource();

        AttachmentUrlParams params = new AttachmentUrlParams().id( sourceAsMap.get( "_id" ).toString() ).download(
            Objects.toString( environment.getArgument( "download" ), "false" ) ).type( environment.getArgument( "type" ) );

        ParamsUrHelper.resolveParams( params.getParams(), environment.getArgument( "params" ) );

        return portalUrlService.attachmentUrl( params );
    }
}
