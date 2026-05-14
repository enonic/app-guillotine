package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetAttachmentUrlByNameDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetAttachmentUrlByNameDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return doGet( environment );
    }

    private String doGet( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> attachmentAsMap = environment.getSource();

        final Map<String, Object> currentContentAsMap = GuillotineLocalContextHelper.getCurrentContent( environment );

        AttachmentUrlParams params = new AttachmentUrlParams().id( currentContentAsMap.get( "_id" ).toString() ).name(
            attachmentAsMap.get( "name" ).toString() ).download( Objects.toString( environment.getArgument( "download" ), "false" ) ).type(
            environment.getArgument( "type" ) );

        ParamsUrHelper.resolveParams( params.getParams(), environment.getArgument( "params" ) );

        return portalUrlService.attachmentUrl( params );
    }
}
