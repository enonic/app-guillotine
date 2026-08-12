package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;

public class GetAttachmentUrlByNameDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private final PortalUrlGeneratorService portalUrlGeneratorService;

    public GetAttachmentUrlByNameDataFetcher( final PortalUrlGeneratorService portalUrlGeneratorService )
    {
        this.portalUrlGeneratorService = portalUrlGeneratorService;
    }

    @Override
    public Map<String, Object> get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Map<String, Object> doGet( final DataFetchingEnvironment environment )
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

        final Map<String, Object> result = UrlPartsHelper.anyAttachmentPartSelected( environment.getSelectionSet() )
            ? UrlPartsHelper.toMap( portalUrlGeneratorService.attachmentUrlParts( buildParams( environment, attachmentAsMap, content, null ) ) )
            : new LinkedHashMap<>();

        result.put( "intent", download != null && download ? "download" : "inline" );

        if ( environment.getSelectionSet().contains( "url" ) )
        {
            result.put( "url", portalUrlGeneratorService.attachmentUrl(
                buildParams( environment, attachmentAsMap, content, GuillotineLocalContextHelper.getAttachmentBaseUrl( environment ) ) ) );
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static AttachmentUrlGeneratorParams buildParams( final DataFetchingEnvironment environment,
                                                             final Map<String, Object> attachmentAsMap, final Content content,
                                                             final String mediaBaseUrl )
    {
        final Boolean download = environment.getArgument( "download" );

        final AttachmentUrlGeneratorParams.Builder builder = AttachmentUrlGeneratorParams.create();

        builder.setName( attachmentAsMap.get( "name" ).toString() );
        builder.setDownload( download != null && download );
        builder.setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) );
        builder.setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) );
        builder.setContent( () -> content );
        builder.setMediaBaseUrl( mediaBaseUrl );

        if ( environment.getArgument( "params" ) instanceof Map queryParams )
        {
            builder.setQueryParams( ParamsUrHelper.convertToMultimap( queryParams ) );
        }

        return builder.build();
    }
}
