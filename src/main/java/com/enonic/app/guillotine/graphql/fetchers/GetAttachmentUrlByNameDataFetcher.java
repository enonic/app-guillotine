package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;

public class GetAttachmentUrlByNameDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlGeneratorService portalUrlGeneratorService;

    public GetAttachmentUrlByNameDataFetcher( final PortalUrlGeneratorService portalUrlGeneratorService )
    {
        this.portalUrlGeneratorService = portalUrlGeneratorService;
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
        final Map<String, Object> attachmentAsMap = environment.getSource();
        if ( attachmentAsMap == null )
        {
            return null;
        }

        final Content content = GuillotineLocalContextHelper.resolveContent( environment );

        if ( content == null )
        {
            return null;
        }

        final Boolean download = environment.getArgument( "download" );

        final AttachmentUrlGeneratorParams.Builder builder = AttachmentUrlGeneratorParams.create();

        builder.setUrlType( environment.getArgument( "type" ) );
        builder.setName( attachmentAsMap.get( "name" ).toString() );
        builder.setDownload( download != null && download );
        builder.setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) );
        builder.setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) );
        builder.setContent( () -> content );
        builder.setBaseUrl( GuillotineLocalContextHelper.getSiteBaseUrl( environment ) );

        if ( environment.getArgument( "params" ) instanceof Map queryParams )
        {
            builder.setQueryParams( ParamsUrHelper.convertToMultimap( queryParams ) );
        }

        return portalUrlGeneratorService.attachmentUrl( builder.build() );
    }
}
