package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;

public class GetLinkMediaUrlPartsDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private final PortalUrlGeneratorService portalUrlGeneratorService;

    private final ContentService contentService;

    public GetLinkMediaUrlPartsDataFetcher( final PortalUrlGeneratorService portalUrlGeneratorService,
                                            final ContentService contentService )
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
            final Content content = contentService.getById( ContentId.from( contentId.toString() ) );

            final AttachmentUrlGeneratorParams params = AttachmentUrlGeneratorParams.create()
                .setContent( () -> content )
                .setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) )
                .setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) )
                .setDownload( "download".equals( intent ) )
                .build();

            return UrlPartsHelper.toMap( portalUrlGeneratorService.attachmentUrlParts( params ),
                                         intent == null ? null : intent.toString() );
        } );
    }
}
