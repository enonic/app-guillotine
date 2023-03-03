package com.enonic.app.guillotine.url;

import java.util.Objects;

import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.attachment.Attachments;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.web.HttpStatus;
import com.enonic.xp.web.WebException;

public class AttachmentUrlBuilder
    extends UrlBuilderBase
{
    private final AttachmentUrlParams params;

    private final ContentService contentService;

    public AttachmentUrlBuilder( final AttachmentUrlParams params, final ContentService contentService )
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
        appendPart( url, "attachment" );
        appendPart( url, ContextAccessor.current().getRepositoryId().toString().replace( "com.enonic.cms.", "" ) );
        appendPart( url, ContextAccessor.current().getBranch().toString() );
        appendPart( url, params.isDownload() ? "download" : "inline" );

        final Content content = resolveContent();
        Attachment attachment = resolveAttachment( content );
        String hash = resolveHash( content, attachment );

        appendPart( url, content.getId().toString() + ":" + hash );
        appendPart( url, attachment.getName() );

        return rewriteUrl( url.toString(), params.getType(), params.getPortalRequest().getRawRequest() );
    }

    private Content resolveContent()
    {
        try
        {
            return contentService.getById( ContentId.from( params.getId() ) );
        }
        catch ( ContentNotFoundException e )
        {
            throw new WebException( HttpStatus.NOT_FOUND, String.format( "Attachment [%s] not found", params.getId() ), e );
        }
    }

    private Attachment resolveAttachment( final Content content )
    {
        final Attachments attachments = content.getAttachments();

        final Attachment attachment;
        if ( this.params.getName() != null )
        {
            attachment = attachments.byName( this.params.getName() );
            if ( attachment == null )
            {
                throw new IllegalArgumentException(
                    "Could not find attachment with name [" + this.params.getName() + "] on content [" + content.getId() + "]" );
            }
        }
        else
        {
            attachment = attachments.byLabel( "source" );
            if ( attachment == null )
            {
                throw new IllegalArgumentException( "Could not find attachment with label [source] on content [" + content.getId() + "]" );
            }
        }

        return attachment;
    }

    private String resolveHash( final Content content, final Attachment attachment )
    {
        return contentService.getBinaryKey( content.getId(), attachment.getBinaryReference() );
    }

}
