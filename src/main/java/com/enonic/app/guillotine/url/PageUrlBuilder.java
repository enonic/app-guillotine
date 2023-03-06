package com.enonic.app.guillotine.url;

import java.util.Objects;

import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;

public class PageUrlBuilder
    extends UrlBuilderBase
{
    private final PageUrlParams params;

    private final ContentService contentService;

    public PageUrlBuilder( final PageUrlParams params, final ContentService contentService )
    {
        this.params = params;
        this.contentService = contentService;
    }

    public String buildUrl()
    {
        Objects.requireNonNull( params.getId(), "Missing mandatory parameter 'id' for image URL" );

        StringBuilder url = new StringBuilder();

        appendPart( url, "api" );
        appendPart( url, "_" );
        appendPart( url, "page" );
        appendPart( url, ContextAccessor.current().getRepositoryId().toString().replace( "com.enonic.cms.", "" ) );
        appendPart( url, ContextAccessor.current().getBranch().toString() );

        Content content = contentService.getById( ContentId.from( params.getId() ) );
        appendPart( url, content.getPath().toString() );

        return rewriteUrl( url.toString(), params.getType(), params.getPortalRequest().getRawRequest() );
    }

}
