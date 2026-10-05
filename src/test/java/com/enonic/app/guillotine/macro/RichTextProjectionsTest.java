package com.enonic.app.guillotine.macro;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.portal.url.ProcessedHtml;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RichTextProjectionsTest
{
    @Test
    public void contentLinkHasItsContent()
    {
        final Map<String, Object> link =
            RichTextProjections.link( new ProcessedHtml.ContentLink( "link-1", "content://first", "first", null, "" ) );

        assertEquals( "first", link.get( "contentId" ) );
    }

    @Test
    public void mediaLinkHasItsMediaContent()
    {
        final Map<String, Object> link =
            RichTextProjections.link( new ProcessedHtml.AttachmentLink( "link-2", "media://download/doc", "doc", null, true ) );

        assertEquals( "doc", link.get( "contentId" ) );
        assertEquals( "doc", CastHelper.<Map<String, Object>>cast( link.get( "media" ) ).get( "contentId" ) );
    }
}
