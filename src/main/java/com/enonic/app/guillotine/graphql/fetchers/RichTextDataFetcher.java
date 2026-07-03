package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.macro.CustomHtmlPostProcessor;
import com.enonic.app.guillotine.macro.HtmlEditorProcessedResult;
import com.enonic.app.guillotine.macro.MacroDecorator;
import com.enonic.app.guillotine.macro.MacroEditorJsonSerializer;
import com.enonic.app.guillotine.macro.MacroEditorSerializer;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.HtmlEditorResultMapper;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.portal.html.HtmlDocument;
import com.enonic.xp.portal.html.HtmlElement;
import com.enonic.xp.portal.url.ProcessHtmlParams;

public class RichTextDataFetcher
    implements DataFetcher<Object>
{
    private final String htmlText;

    private final ServiceFacade serviceFacade;

    private final GuillotineContext guillotineContext;

    public RichTextDataFetcher( final String htmlText, final ServiceFacade serviceFacade, final GuillotineContext guillotineContext )
    {
        this.htmlText = htmlText;
        this.serviceFacade = serviceFacade;
        this.guillotineContext = guillotineContext;
    }

    public Object execute( final DataFetchingEnvironment environment )
    {
        try
        {
            return GuillotineLocalContextHelper.executeInContext( environment, () -> get( environment ) );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        ProcessHtmlParams htmlParams = createProcessHtmlParams( environment );

        List<Map<String, Object>> links = new ArrayList<>();
        List<Map<String, Object>> images = new ArrayList<>();
        List<MacroDecorator> processedMacros = new ArrayList<>();

        Map<String, MacroDescriptor> registeredMacros = guillotineContext.getMacroDecorators();

        final String pageBaseUrl = GuillotineLocalContextHelper.getPageBaseUrl( environment );

        final String mediaBaseUrl = GuillotineLocalContextHelper.getMediaBaseUrl( environment );

        htmlParams.processMacros( false );
        htmlParams.customStyleDescriptorsCallback( () -> serviceFacade.getStyleDescriptorService().getAll() );
        htmlParams.customHtmlProcessor( processor -> {
            HtmlDocument htmlDocument = processor.getDocument();

            final List<HtmlElement> contentLinks = pageBaseUrl == null || pageBaseUrl.isBlank()
                ? List.of()
                : htmlDocument.select( "[href]" )
                    .stream()
                    .filter( element -> {
                        final String href = element.getAttribute( "href" );
                        return href != null && href.startsWith( "content://" );
                    } )
                    .collect( Collectors.toList() );

            processor.processDefault( new CustomHtmlPostProcessor( links, images ) );

            contentLinks.forEach( element -> element.setAttribute( "href", GuillotineLocalContextHelper.prependBaseUrl( pageBaseUrl,
                                                                                                                       element.getAttribute(
                                                                                                                           "href" ) ) ) );

            if ( mediaBaseUrl != null )
            {
                prependMediaBaseUrl( htmlDocument, "src", mediaBaseUrl );
                prependMediaBaseUrl( htmlDocument, "href", mediaBaseUrl );
                prependMediaBaseUrlToSrcset( htmlDocument, mediaBaseUrl );
            }

            htmlDocument.select( "figcaption:empty" ).forEach( HtmlElement::remove );
            return htmlDocument.getInnerHtml();
        } );

        String processedHtml =
            serviceFacade.getMacroService().evaluateMacros( serviceFacade.getPortalUrlService().processHtml( htmlParams ), macro -> {
                if ( !registeredMacros.containsKey( macro.getName() ) )
                {
                    return macro.toString();
                }
                MacroDecorator macroDecorator = MacroDecorator.from( macro );
                processedMacros.add( macroDecorator );
                return new MacroEditorSerializer( macroDecorator ).serialize();
            } );

        HtmlEditorProcessedResult.Builder builder =
            HtmlEditorProcessedResult.create().setRaw( htmlText ).setImages( images ).setLinks( links ).setProcessedHtml( processedHtml );

        if ( !processedMacros.isEmpty() )
        {
            final List<Map<String, Object>> macrosAsJson = processedMacros.stream().map(
                macro -> new MacroEditorJsonSerializer( macro, registeredMacros.get( macro.getMacro().getName() ) ).serialize() ).collect(
                Collectors.toList() );

            builder.setMacrosAsJson( macrosAsJson );
        }

        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new HtmlEditorResultMapper( builder.build() ).serialize( generator );
        return generator.getRoot();
    }

    private static void prependMediaBaseUrl( final HtmlDocument htmlDocument, final String attributeName, final String mediaBaseUrl )
    {
        htmlDocument.select( "[" + attributeName + "]" ).forEach( element -> {
            final String value = element.getAttribute( attributeName );
            final String rewritten = GuillotineLocalContextHelper.prependMediaBaseUrl( mediaBaseUrl, value );
            if ( !Objects.equals( value, rewritten ) )
            {
                element.setAttribute( attributeName, rewritten );
            }
        } );
    }

    private static void prependMediaBaseUrlToSrcset( final HtmlDocument htmlDocument, final String mediaBaseUrl )
    {
        htmlDocument.select( "[srcset]" ).forEach( element -> {
            final String srcset = element.getAttribute( "srcset" );
            if ( srcset == null || srcset.isBlank() )
            {
                return;
            }
            final String processed = Arrays.stream( srcset.split( "," ) ).map( entry -> {
                final String candidate = entry.stripLeading();
                final String rewritten = GuillotineLocalContextHelper.prependMediaBaseUrl( mediaBaseUrl, candidate );
                if ( Objects.equals( candidate, rewritten ) )
                {
                    return entry;
                }
                final String leadingWhitespace = entry.substring( 0, entry.length() - candidate.length() );
                return leadingWhitespace + rewritten;
            } ).collect( Collectors.joining( "," ) );
            if ( !srcset.equals( processed ) )
            {
                element.setAttribute( "srcset", processed );
            }
        } );
    }

    private ProcessHtmlParams createProcessHtmlParams( DataFetchingEnvironment environment )
    {
        Map<String, Object> processHtmlParams = environment.getArgument( "processHtml" );

        final ProcessHtmlParams htmlParams =
            new ProcessHtmlParams().value( htmlText ).baseUrl( GuillotineLocalContextHelper.resolveMediaBaseUrl( environment ) );

        if ( processHtmlParams != null )
        {
            if ( processHtmlParams.containsKey( "imageWidths" ) )
            {
                htmlParams.imageWidths( (List<Integer>) processHtmlParams.get( "imageWidths" ) );
            }
            if ( processHtmlParams.containsKey( "imageSizes" ) )
            {
                htmlParams.imageSizes( processHtmlParams.get( "imageSizes" ).toString() );
            }
        }

        return htmlParams;
    }
}
