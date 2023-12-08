package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.Constants;
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
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
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

        htmlParams.processMacros( false );
        htmlParams.customStyleDescriptorsCallback( () -> serviceFacade.getStyleDescriptorService().getAll() );
        htmlParams.customHtmlProcessor( processor -> {
            processor.processDefault( new CustomHtmlPostProcessor( links, images ) );

            HtmlDocument htmlDocument = processor.getDocument();
            htmlDocument.select( "figcaption:empty" ).forEach( HtmlElement::remove );
            return htmlDocument.getInnerHtml();
        } );

        Map<String, Object> localContext = environment.getLocalContext();
        String contentId = (String) localContext.get( Constants.CONTENT_ID_FIELD );

        String processedHtml =
            serviceFacade.getMacroService().evaluateMacros( serviceFacade.getPortalUrlService().processHtml( htmlParams ), macro -> {
                if ( !registeredMacros.containsKey( macro.getName() ) )
                {
                    return macro.toString();
                }
                MacroDecorator macroDecorator = MacroDecorator.from( macro, contentId );
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

    private ProcessHtmlParams createProcessHtmlParams( DataFetchingEnvironment environment )
    {
        final PortalRequest portalRequest = createPortalRequest( PortalRequestAccessor.get(), environment );

        ProcessHtmlParams htmlParams = new ProcessHtmlParams().portalRequest( portalRequest ).value( htmlText );

        Map<String, Object> processHtmlParams = environment.getArgument( "processHtml" );

        if ( processHtmlParams != null )
        {
            if ( processHtmlParams.containsKey( "type" ) )
            {
                htmlParams.type( processHtmlParams.get( "type" ).toString() );
            }
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

    private PortalRequest createPortalRequest( final PortalRequest source, final DataFetchingEnvironment environment )
    {
        final PortalRequest result = new PortalRequest();

        result.setRepositoryId( GuillotineLocalContextHelper.getRepositoryId( environment, source.getRepositoryId() ) );
        result.setBranch( GuillotineLocalContextHelper.getBranch( environment, source.getBranch() ) );
        result.setApplicationKey( source.getApplicationKey() );
        result.setContentPath( source.getContentPath() );
        result.setMode( source.getMode() );
        result.setRawRequest( source.getRawRequest() );
        result.setContent( source.getContent() );
        result.setSite( source.getSite() );
        result.setMethod( source.getMethod() );
        result.setBaseUri( source.getBaseUri() );
        result.setRawPath( source.getRawPath() );
        result.setWebSocketContext( source.getWebSocketContext() );
        result.setComponent( source.getComponent() );
        result.setControllerScript( source.getControllerScript() );
        result.setPageDescriptor( source.getPageDescriptor() );
        result.setPageTemplate( source.getPageTemplate() );
        result.setContextPath( source.getContextPath() );
        result.setValidTicket( source.isValidTicket() );
        result.setBody( source.getBody() );
        result.setIdProvider( source.getIdProvider() );
        result.setHost( source.getHost() );
        result.setPort( source.getPort() );
        result.setScheme( source.getScheme() );
        result.setPath( source.getPath() );
        result.setEndpointPath( source.getEndpointPath() );
        result.setUrl( source.getUrl() );
        result.setContentType( source.getContentType() );
        result.setRemoteAddress( source.getRemoteAddress() );

        return result;
    }
}
