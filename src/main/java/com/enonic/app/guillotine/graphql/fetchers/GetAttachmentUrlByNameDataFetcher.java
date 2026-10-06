package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.portal.url.AttachmentUrlPartsParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetAttachmentUrlByNameDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private final PortalUrlService portalUrlService;

    public GetAttachmentUrlByNameDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
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
            ? UrlPartsHelper.toMap( portalUrlService.attachmentUrlParts( buildParams( environment, attachmentAsMap, content ) ) )
            : new LinkedHashMap<>();

        result.put( "intent", download != null && download ? "download" : "inline" );

        return result;
    }

    @SuppressWarnings("unchecked")
    private static AttachmentUrlPartsParams buildParams( final DataFetchingEnvironment environment,
                                                             final Map<String, Object> attachmentAsMap, final Content content )
    {
        final Boolean download = environment.getArgument( "download" );

        final AttachmentUrlPartsParams.Builder builder = AttachmentUrlPartsParams.create();

        builder.setName( attachmentAsMap.get( "name" ).toString() );
        builder.setDownload( download != null && download );
        builder.setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) );
        builder.setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) );
        builder.setContent( () -> content );

        return builder.build();
    }
}
