package com.enonic.app.guillotine.graphql;

import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.content.ContentId;

public class AttachmentProxy
{
    private final Attachment attachment;

    private final ContentId contentId;

    public AttachmentProxy( final Attachment attachment, final ContentId contentId )
    {
        this.attachment = attachment;
        this.contentId = contentId;
    }

    public ContentId getContentId()
    {
        return contentId;
    }

    public String getMimeType()
    {
        return attachment.getMimeType();
    }

    public String getName()
    {
        return attachment.getName();
    }

    public String getLabel()
    {
        return attachment.getLabel();
    }

    public long getSize()
    {
        return attachment.getSize();
    }
}
