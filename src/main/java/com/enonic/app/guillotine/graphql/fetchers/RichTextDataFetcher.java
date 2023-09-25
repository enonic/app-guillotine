package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.macro.CustomHtmlPostProcessor;
import com.enonic.app.guillotine.macro.HtmlEditorProcessedResult;
import com.enonic.app.guillotine.macro.MacroDecorator;
import com.enonic.app.guillotine.macro.MacroEditorJsonSerializer;
import com.enonic.app.guillotine.macro.MacroEditorSerializer;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.HtmlEditorResultMapper;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationKeys;
import com.enonic.xp.macro.MacroDescriptor;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.html.HtmlDocument;
import com.enonic.xp.portal.html.HtmlElement;
import com.enonic.xp.portal.url.ProcessHtmlParams;
import com.enonic.xp.site.SiteConfig;

public class RichTextDataFetcher
    implements DataFetcher<Object>
{
    private final String htmlText;

    private final String contentId;

    private final ServiceFacade serviceFacade;

    public RichTextDataFetcher( final String htmlText, final String contentId, final ServiceFacade serviceFacade )
    {
        this.htmlText = htmlText;
        this.contentId = contentId;
        this.serviceFacade = serviceFacade;
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

        PortalRequest portalRequest = PortalRequestAccessor.get();

        Map<String, MacroDescriptor> registeredMacros =
            portalRequest.getSite() != null ? getRegisteredMacrosInSystemForSite( portalRequest ) : getRegisteredMacrosInSystem();

        htmlParams.processMacros( false );
        htmlParams.customHtmlProcessor( processor -> {
            processor.processDefault( new CustomHtmlPostProcessor( links, images ) );

            HtmlDocument htmlDocument = processor.getDocument();
            htmlDocument.select( "figcaption:empty" ).forEach( HtmlElement::remove );
            return htmlDocument.getInnerHtml();
        } );

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
        ProcessHtmlParams htmlParams = new ProcessHtmlParams().portalRequest( PortalRequestAccessor.get() ).value( htmlText );

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

    private Map<String, MacroDescriptor> getRegisteredMacrosInSystemForSite( final PortalRequest portalRequest )
    {
        List<ApplicationKey> applicationKeys = new ArrayList<>();
        applicationKeys.add( ApplicationKey.SYSTEM );
        applicationKeys.addAll(
            portalRequest.getSite().getSiteConfigs().stream().map( SiteConfig::getApplicationKey ).collect( Collectors.toList() ) );

        Map<String, MacroDescriptor> result = new LinkedHashMap<>();

        serviceFacade.getMacroDescriptorService().getByApplications( ApplicationKeys.from( applicationKeys ) ).forEach( macroDescriptor -> {
            if ( !result.containsKey( macroDescriptor.getName() ) )
            {
                result.put( macroDescriptor.getName(), macroDescriptor );
            }
        } );

        return result;
    }

    private Map<String, MacroDescriptor> getRegisteredMacrosInSystem()
    {
        return serviceFacade.getMacroDescriptorService().getAll().stream().collect(
            Collectors.toMap( MacroDescriptor::getName, Function.identity() ) );
    }
}
