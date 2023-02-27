package com.enonic.app.guillotine.handler;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.portal.html.HtmlElement;
import com.enonic.xp.portal.url.HtmlElementPostProcessor;

class CustomHtmlPostProcessor
    implements HtmlElementPostProcessor
{
    private final List<Map<String, Object>> links;

    private final List<Map<String, Object>> images;

    CustomHtmlPostProcessor( final List<Map<String, Object>> links, final List<Map<String, Object>> images )
    {
        this.links = links;
        this.images = images;
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
            element.setAttribute( "data-image-ref", imgEditorRef );
            wrapImageSrc( element, project, branch );
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

    private void wrapImageSrc( final HtmlElement element, final String project, final String branch )
    {
        String src = element.getAttribute( "src" );
        element.setAttribute( "src", processImageSrc( src, project, branch ) );

        String srcset = element.getAttribute( "srcset" );
        if ( srcset != null )
        {
            element.setAttribute( "srcset", Arrays.stream( srcset.split( ",", -1 ) ).map( String::trim ).map(
                value -> processImageSrc( value, project, branch ) ).collect( Collectors.joining( "," ) ) );
        }
    }

    private String processImageSrc( String value, String project, String branch )
    {
        int position = value.indexOf( "/_/" );
        if ( position == 0 )
        {
            return "/site/" + project + "/" + branch + value;
        }
        else
        {
            return value.substring( 0, position ) + "/site/" + project + "/" + branch + value.substring( position );
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
}
