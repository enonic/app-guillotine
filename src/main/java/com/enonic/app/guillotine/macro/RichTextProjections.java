package com.enonic.app.guillotine.macro;

import java.util.LinkedHashMap;
import java.util.Map;

import com.enonic.xp.portal.url.AttachmentUrlParts;
import com.enonic.xp.portal.url.ImageUrlParts;
import com.enonic.xp.portal.url.PageUrlParts;
import com.enonic.xp.portal.url.ProcessedHtml;

/**
 * Projections of the links and images of {@link ProcessedHtml} for the {@code Link} and {@code Image} GraphQL types,
 * under the refs XP writes into the HTML.
 */
public final class RichTextProjections
{
    private RichTextProjections()
    {
    }

    public static Map<String, Object> link( final ProcessedHtml.Link link )
    {
        final Map<String, Object> projection = new LinkedHashMap<>();

        projection.put( "linkRef", link.ref() );
        projection.put( "uri", link.uri() );

        switch ( link )
        {
            case ProcessedHtml.ContentLink content ->
            {
                projection.put( "contentId", content.contentId() );
                projection.put( "pageUrl", content.page() == null ? null : pageUrl( content.page() ) );
                projection.put( "fragment", content.fragment() );
            }
            case ProcessedHtml.AttachmentLink attachment ->
            {
                final String intent = attachment.download() ? "download" : "inline";

                final Map<String, Object> media = new LinkedHashMap<>();
                media.put( "intent", intent );
                media.put( "contentId", attachment.contentId() );
                media.put( "mediaUrl", attachment.attachment() == null ? null : attachmentUrl( attachment.attachment(), intent ) );

                projection.put( "contentId", null );
                projection.put( "media", media );
                projection.put( "fragment", "" );
            }
        }

        return projection;
    }

    public static Map<String, Object> image( final ProcessedHtml.Image image )
    {
        final Map<String, Object> projection = new LinkedHashMap<>();

        projection.put( "imageId", image.contentId() );
        projection.put( "imageRef", image.ref() );

        final ProcessedHtml.Style style = image.style();
        if ( style != null )
        {
            final Map<String, Object> styleProjection = new LinkedHashMap<>();
            styleProjection.put( "application", style.application().toString() );
            styleProjection.put( "name", style.name() );
            styleProjection.put( "aspectRatio", style.aspectRatio() );
            styleProjection.put( "filter", style.filter() );
            projection.put( "style", styleProjection );
        }

        projection.put( "src", image.src() == null ? null : imageUrl( image.src() ) );
        projection.put( "srcset", image.srcset().stream().map( RichTextProjections::source ).toList() );

        return projection;
    }

    private static Map<String, Object> source( final ProcessedHtml.Source source )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "width", source.width() );
        result.put( "imageUrl", imageUrl( source.url() ) );
        return result;
    }

    private static Map<String, Object> imageUrl( final ImageUrlParts parts )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", parts.path() );
        result.put( "queryString", parts.queryString() );
        result.put( "context", parts.context() );
        result.put( "id", parts.id() );
        result.put( "fingerprint", parts.fingerprint() );
        result.put( "scale", parts.scale() );
        result.put( "name", parts.name() );
        return result;
    }

    private static Map<String, Object> pageUrl( final PageUrlParts parts )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "baseUrl", parts.baseUrl() );
        result.put( "path", parts.path() );
        result.put( "queryString", parts.queryString() );
        return result;
    }

    private static Map<String, Object> attachmentUrl( final AttachmentUrlParts parts, final String intent )
    {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put( "path", parts.path() );
        result.put( "queryString", parts.queryString() );
        result.put( "context", parts.context() );
        result.put( "id", parts.id() );
        result.put( "fingerprint", parts.fingerprint() );
        result.put( "name", parts.name() );
        result.put( "intent", intent );
        return result;
    }
}
