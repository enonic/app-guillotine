package com.enonic.app.guillotine.mapper;

import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.content.Content;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public final class AttachmentsMapper
    implements MapSerializable
{
    private final Content content;

    public AttachmentsMapper( final Content content )
    {
        this.content = content;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        if ( content.getAttachments() != null )
        {
            for ( Attachment attachment : content.getAttachments() )
            {
                gen.map( attachment.getName() );
                serializeAttachment( gen, attachment );
                gen.end();
            }
        }
    }

    private void serializeAttachment( final MapGenerator gen, final Attachment attachment )
    {
        gen.value( "name", attachment.getName() );
        gen.value( "label", attachment.getLabel() );
        gen.value( "size", attachment.getSize() );
        gen.value( "mimeType", attachment.getMimeType() );
    }
}
