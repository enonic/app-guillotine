package com.enonic.app.guillotine.handler;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.enonic.app.guillotine.url.ImageUrlBuilder;
import com.enonic.app.guillotine.url.ImageUrlParams;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.html.HtmlElement;
import com.enonic.xp.portal.url.HtmlElementPostProcessor;

class CustomHtmlPostProcessor
    implements HtmlElementPostProcessor
{
    private final List<Map<String, Object>> links;

    private final List<Map<String, Object>> images;

    private final ContentService contentService;

    private final PortalRequest portalRequest;

    private final List<Integer> imagesWidths;

    CustomHtmlPostProcessor( final List<Map<String, Object>> links, final List<Map<String, Object>> images,
                             final ContentService contentService, final PortalRequest portalRequest, final List<Integer> imagesWidths )
    {
        this.links = links;
        this.images = images;
        this.contentService = contentService;
        this.portalRequest = portalRequest;
        this.imagesWidths = imagesWidths;
    }

    @Override
    public void process( final HtmlElement element, final Map<String, String> properties )
    {
        String project = ContextAccessor.current().getRepositoryId().toString().replace( "com.enonic.cms.", "" );
        String branch = ContextAccessor.current().getBranch().toString();

        if ( "a".equals( element.getTagName() ) )
        {
            final String linkEditorRef = UUID.randomUUID().toString();
            element.setAttribute( "data-link-ref", linkEditorRef );
            wrapContentHref( element, project, branch );
            links.add( buildLinkProjection( linkEditorRef, properties ) );
        }
        if ( "img".equals( element.getTagName() ) )
        {
            final String imgEditorRef = UUID.randomUUID().toString();
            final String contentId = properties.get( "contentId" );
            final String urlType = properties.get( "type" );

            element.setAttribute( "data-image-ref", imgEditorRef );
            element.setAttribute( "src", buildImageUrl( contentId, urlType, 768 ) );

            if ( imagesWidths != null && !imagesWidths.isEmpty() )
            {
                String srcset =
                    imagesWidths.stream().map( width -> buildImageUrl( contentId, urlType, width ) + " " + width + "w" ).collect(
                        Collectors.joining( "," ) );
                element.setAttribute( "srcset", srcset );
            }

            images.add( buildImageProjection( imgEditorRef, properties ) );
        }
    }

    private void wrapContentHref( final HtmlElement element, final String project, final String branch )
    {
        String href = element.getAttribute( "href" );
        if ( href.startsWith( "http" ) )
        {
            int position = href.indexOf( "/", href.indexOf( "://" ) + 3 );
            element.setAttribute( "href", href.substring( 0, position ) + "/site/" + project + "/" + branch + "/" +
                href.substring( position + 1 ) );
        }
        else
        {
            element.setAttribute( "href", "/site/" + project + "/" + branch + href );
        }
    }

    private Map<String, Object> buildLinkProjection( String linkRef, Map<String, String> properties )
    {
        final Map<String, Object> projection = new LinkedHashMap<>();

        String mode = properties.get( "mode" );

        projection.put( "contentId", mode != null ? null : properties.get( "contentId" ) ); // only for content
        projection.put( "linkRef", linkRef );
        projection.put( "uri", properties.get( "uri" ) );

        if ( mode != null )
        {
            Map<String, Object> mediaAsMap = new LinkedHashMap<>();
            mediaAsMap.put( "intent", mode );
            mediaAsMap.put( "contentId", properties.get( "contentId" ) );
            projection.put( "media", mediaAsMap ); // only for media
        }

        return projection;
    }

    private Map<String, Object> buildImageProjection( final String imgEditorRef, final Map<String, String> properties )
    {
        final Map<String, Object> imageProjection = new LinkedHashMap<>();

        imageProjection.put( "imageId", properties.get( "contentId" ) );
        imageProjection.put( "imageRef", imgEditorRef );

        final Map<String, Object> styleProjection = new LinkedHashMap<>();

        if ( properties.containsKey( "style:name" ) )
        {
            styleProjection.put( "name", properties.get( "style:name" ) );
        }
        if ( properties.containsKey( "style:aspectRatio" ) )
        {
            styleProjection.put( "aspectRatio", properties.get( "style:aspectRatio" ) );
        }
        if ( properties.containsKey( "style:filter" ) )
        {
            styleProjection.put( "filter", properties.get( "style:filter" ) );
        }

        if ( !styleProjection.isEmpty() )
        {
            imageProjection.put( "style", styleProjection );
        }

        return imageProjection;
    }

    public List<Map<String, Object>> getLinks()
    {
        return links;
    }

    public List<Map<String, Object>> getImages()
    {
        return images;
    }

    private String buildImageUrl( String id, String urlType, Integer width )
    {
        final ImageUrlParams params = new ImageUrlParams();

        params.setId( id );
        params.setType( urlType );
        params.setScale( "width-" + width );
        params.setPortalRequest( portalRequest );

        return new ImageUrlBuilder( params, contentService ).buildUrl();
    }
}
