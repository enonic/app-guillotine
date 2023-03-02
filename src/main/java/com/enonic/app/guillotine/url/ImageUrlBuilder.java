package com.enonic.app.guillotine.url;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.hash.Hashing;

import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.Media;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.portal.url.UrlTypeConstants;
import com.enonic.xp.web.HttpStatus;
import com.enonic.xp.web.WebException;
import com.enonic.xp.web.servlet.ServletRequestUrlHelper;
import com.enonic.xp.web.servlet.UriRewritingResult;

public class ImageUrlBuilder
    extends UrlBuilderBase
{
    private final ImageUrlParams params;

    private final ContentService contentService;

    public ImageUrlBuilder( final ImageUrlParams params, final ContentService contentService )
    {
        this.params = params;
        this.contentService = contentService;
    }

    public String buildUrl()
    {
        Objects.requireNonNull( params.getId(), "Missing mandatory parameter 'id' for image URL" );
        Objects.requireNonNull( params.getScale(), "Missing mandatory parameter 'scale' for image URL" );

        final Media media = resolveMedia();
        final String hash = resolveHash( media );
        final String name = resolveName( media );
        final String scale = resolveScale();

        StringBuilder url = new StringBuilder();

        appendPart( url, "api" );
        appendPart( url, "_" );
        appendPart( url, "image" );
        appendPart( url, ContextAccessor.current().getRepositoryId().toString().replace( "com.enonic.cms.", "" ) );
        appendPart( url, ContextAccessor.current().getBranch().toString() );
        appendPart( url, media.getId() + ":" + hash );
        appendPart( url, scale );
        appendPart( url, name );

        final UriRewritingResult rewritingResult = ServletRequestUrlHelper.rewriteUri( params.getPortalRequest().getRawRequest(), url.toString() );

        String uri = rewritingResult.getRewrittenUri();

        if ( UrlTypeConstants.ABSOLUTE.equals( this.params.getType() ) )
        {
            return ServletRequestUrlHelper.getServerUrl( params.getPortalRequest().getRawRequest() ) + uri;
        }
        else if ( UrlTypeConstants.WEBSOCKET.equals( this.params.getType() ) )
        {
            return ServletRequestUrlHelper.getServerUrl( new HttpServletRequestWrapper( params.getPortalRequest().getRawRequest() )
            {
                @Override
                public String getScheme()
                {
                    return isSecure() ? "wss" : "ws";
                }
            } ) + uri;
        }
        else
        {
            return uri;
        }
    }

    private Media resolveMedia()
    {
        final Content content;
        try
        {
            content = contentService.getById( ContentId.from( params.getId() ) );
        }
        catch ( ContentNotFoundException e )
        {
            throw new WebException( HttpStatus.NOT_FOUND, String.format( "Image [%s] not found", params.getId() ), e );
        }
        if ( !content.getType().isDescendantOfMedia() && !content.getType().isMedia() )
        {
            throw WebException.notFound( String.format( "Image with [%s] id not found", content.getId() ) );
        }
        return (Media) content;
    }

    private String resolveHash( final Media media )
    {
        String binaryKey = this.contentService.getBinaryKey( media.getId(), media.getMediaAttachment().getBinaryReference() );
        return Hashing.sha1().newHasher().putString( String.valueOf( binaryKey ), StandardCharsets.UTF_8 ).putString(
            String.valueOf( media.getFocalPoint() ), StandardCharsets.UTF_8 ).putString( String.valueOf( media.getCropping() ),
                                                                                         StandardCharsets.UTF_8 ).putString(
            String.valueOf( media.getOrientation() ), StandardCharsets.UTF_8 ).hash().toString();
    }

    private String resolveName( final Media media )
    {
        return media.getName().toString();
    }

    private String resolveScale()
    {
        return params.getScale().replaceAll( "\\s", "" ).replaceAll( "[(,]", "-" ).replace( ")", "" );
    }
}
