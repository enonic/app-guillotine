package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
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

    @SuppressWarnings("unchecked")
    private String doGet( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> sourceAsMap = environment.getSource();

        final Content content = GuillotineLocalContextHelper.resolveContent( environment, sourceAsMap.get( "_id" ).toString() );

        if ( content == null )
        {
            return null;
        }

        final Boolean download = environment.getArgument( "download" );

        final AttachmentUrlGeneratorParams.Builder builder = AttachmentUrlGeneratorParams.create();

        builder.setUrlType( environment.getArgument( "type" ) );
        builder.setDownload( download != null && download );
        builder.setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) );
        builder.setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) );
        builder.setContent( () -> content );
        builder.setBaseUrl( GuillotineLocalContextHelper.getSiteBaseUrl( environment ) );

        if ( environment.getArgument( "params" ) instanceof Map queryParams )
        {
            builder.addQueryParams( ParamsUrHelper.convertToMultimap( queryParams ) );
        }

        return portalUrlService.attachmentUrl( builder.build() );
    }
}
