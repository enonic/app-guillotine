package com.enonic.app.guillotine.graphql.fetchers;

import java.util.List;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.macro.HtmlEditorProcessedResult;
import com.enonic.app.guillotine.macro.MacroEditorJsonSerializer;
import com.enonic.app.guillotine.macro.RichTextProjections;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.HtmlEditorResultMapper;
import com.enonic.xp.portal.url.ProcessHtmlPartsParams;
import com.enonic.xp.portal.url.ProcessedHtml;

public class RichTextDataFetcher
    implements DataFetcher<Object>
{
    private final String htmlText;

    private final ServiceFacade serviceFacade;

    public RichTextDataFetcher( final String htmlText, final ServiceFacade serviceFacade )
    {
        this.htmlText = htmlText;
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
        final DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet();
        if ( selectionSet != null && !selectionSet.containsAnyOf( "processedHtml", "macrosAsJson", "macros", "images", "links" ) )
        {
            return serialize( HtmlEditorProcessedResult.create().setRaw( htmlText ).build() );
        }

        final ProcessedHtml result = serviceFacade.getPortalUrlService().processHtmlParts( createProcessHtmlParams( environment ).build() );
        final List<Map<String, Object>> links = result.links().stream().map( RichTextProjections::link ).toList();
        final List<Map<String, Object>> images = result.images().stream().map( RichTextProjections::image ).toList();

        HtmlEditorProcessedResult.Builder builder =
            HtmlEditorProcessedResult.create().setRaw( htmlText ).setImages( images ).setLinks( links ).setProcessedHtml( result.html() );

        final List<Map<String, Object>> macrosAsJson = result.macros().stream().map( macro -> new MacroEditorJsonSerializer( macro ).serialize() ).toList();
        if ( !macrosAsJson.isEmpty() )
        {
            builder.setMacrosAsJson( macrosAsJson );
        }

        return serialize( builder.build() );
    }

    private static Object serialize( final HtmlEditorProcessedResult result )
    {
        final GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new HtmlEditorResultMapper( result ).serialize( generator );
        return generator.getRoot();
    }

    @SuppressWarnings("unchecked")
    private ProcessHtmlPartsParams.Builder createProcessHtmlParams( DataFetchingEnvironment environment )
    {
        Map<String, Object> processHtmlParams = environment.getArgument( "processHtml" );

        final ProcessHtmlPartsParams.Builder htmlParams =
            ProcessHtmlPartsParams.create()
                .value( htmlText )
                .scope( GuillotineLocalContextHelper.getPortalScope( environment, serviceFacade.getPortalUrlService() ) );

        if ( processHtmlParams != null )
        {
            if ( processHtmlParams.containsKey( "imageSrcWidth" ) )
            {
                htmlParams.imageSrcWidth( (Integer) processHtmlParams.get( "imageSrcWidth" ) );
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
}
