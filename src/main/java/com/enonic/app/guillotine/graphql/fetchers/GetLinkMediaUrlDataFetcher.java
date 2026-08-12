package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;

public class GetLinkMediaUrlDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private final PortalUrlGeneratorService portalUrlGeneratorService;

    private final ContentService contentService;

    public GetLinkMediaUrlDataFetcher( final PortalUrlGeneratorService portalUrlGeneratorService, final ContentService contentService )
    {
        this.portalUrlGeneratorService = portalUrlGeneratorService;
        this.contentService = contentService;
    }

    @Override
    public Map<String, Object> get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final Map<String, Object> sourceAsMap = environment.getSource();

        final Object contentId = sourceAsMap == null ? null : sourceAsMap.get( "contentId" );
        if ( contentId == null )
        {
            return null;
        }

        final Object intent = sourceAsMap.get( "intent" );

        return GuillotineLocalContextHelper.executeInContext( environment, () -> {
            final boolean partsSelected = UrlPartsHelper.anyAttachmentPartSelected( environment.getSelectionSet() );
            final boolean urlSelected = environment.getSelectionSet().contains( "url" );

            final Map<String, Object> result = new LinkedHashMap<>();

            if ( partsSelected || urlSelected )
            {
                final Content content = contentService.getById( ContentId.from( contentId.toString() ) );

                if ( partsSelected )
                {
                    result.putAll( UrlPartsHelper.toMap(
                        portalUrlGeneratorService.attachmentUrlParts( buildParams( environment, content, intent, null ) ) ) );
                }

                if ( urlSelected )
                {
                    result.put( "url", portalUrlGeneratorService.attachmentUrl(
                        buildParams( environment, content, intent, GuillotineLocalContextHelper.getAttachmentBaseUrl( environment ) ) ) );
                }
            }

            result.put( "intent", intent == null ? null : intent.toString() );

            return result;
        } );
    }

    private static AttachmentUrlGeneratorParams buildParams( final DataFetchingEnvironment environment, final Content content,
                                                             final Object intent, final String mediaBaseUrl )
    {
        return AttachmentUrlGeneratorParams.create()
            .setContent( () -> content )
            .setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) )
            .setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) )
            .setDownload( "download".equals( intent ) )
            .setMediaBaseUrl( mediaBaseUrl )
            .build();
    }
}
