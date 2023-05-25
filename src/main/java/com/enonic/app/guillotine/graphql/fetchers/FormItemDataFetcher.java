package com.enonic.app.guillotine.graphql.fetchers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemType;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeName;

public class FormItemDataFetcher
    implements DataFetcher<Object>
{
    private final FormItem formItem;

    private final ServiceFacade serviceFacade;

    private Map<String, Object> contentAsMap;

    public FormItemDataFetcher( final FormItem formItem, final ServiceFacade serviceFacade )

    {
        this.formItem = formItem;
        this.serviceFacade = serviceFacade;
    }

    public FormItemDataFetcher( final Map<String, Object> contentAsMap, final FormItem formItem, final ServiceFacade serviceFacade )

    {
        this.contentAsMap = contentAsMap;
        this.formItem = formItem;
        this.serviceFacade = serviceFacade;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();
        if ( contentAsMap != null )
        {
            sourceAsMap = CastHelper.cast( sourceAsMap.get( "__data" ) );
        }

        Occurrences occurrences = FormItemTypesHelper.getOccurrences( formItem );

        if ( occurrences.getMaximum() == 1 )
        {
            Object value = sourceAsMap.get( formItem.getName() );

            if ( formItem.getType() == FormItemType.INPUT )
            {
                InputTypeName inputType = ( (Input) formItem ).getInputType();
                if ( inputType.equals( InputTypeName.HTML_AREA ) )
                {
                    return new RichTextDataFetcher( (String) value, null, serviceFacade ).get( environment ); // TODO contentId
                }
                if ( inputType.equals( InputTypeName.ATTACHMENT_UPLOADER ) && contentAsMap != null )
                {
                    Map<String, Object> attachmentsAsMap = CastHelper.cast( contentAsMap.get( "attachments" ) );
                    return attachmentsAsMap.get( (String) value );
                }
                if ( inputType.equals( InputTypeName.CONTENT_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_SELECTOR ) ||
                    inputType.equals( InputTypeName.IMAGE_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_UPLOADER ) )
                {
                    return new GetContentCommand( serviceFacade.getContentService() ).execute( (String) value );
                }
            }
            return value;
        }
        else
        {
            ArgumentsValidator.validateArguments( environment.getArguments() );

            List<Object> values = ArrayHelper.forceArray( sourceAsMap.get( formItem.getName() ) );

            Map<String, Object> arguments = environment.getArguments();
            if ( arguments.get( "offset" ) != null || arguments.get( "first" ) != null )
            {
                int offset = Objects.requireNonNullElse( (Integer) arguments.get( "offset" ), 0 );
                int first = Objects.requireNonNullElse( (Integer) arguments.get( "first" ), values.size() );
                values = ArrayHelper.slice( values, offset, first );
            }

            if ( formItem.getType() == FormItemType.INPUT )
            {
                InputTypeName inputType = ( (Input) formItem ).getInputType();
                if ( inputType.equals( InputTypeName.HTML_AREA ) )
                {
                    return values.stream().map(
                        value -> new RichTextDataFetcher( (String) value, null, serviceFacade ).execute( environment ) ).collect(
                        // TODO contentId
                        Collectors.toList() );
                }
                if ( inputType.equals( InputTypeName.ATTACHMENT_UPLOADER ) && contentAsMap != null )
                {
                    return values.stream().map( value -> {
                        Map<String, Object> attachmentsAsMap = CastHelper.cast( contentAsMap.get( "attachments" ) );
                        return attachmentsAsMap.get( (String) value );
                    } );
                }
                if ( inputType.equals( InputTypeName.CONTENT_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_SELECTOR ) ||
                    inputType.equals( InputTypeName.IMAGE_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_UPLOADER ) )
                {
                    return values.stream().map(
                        value -> new GetContentCommand( serviceFacade.getContentService() ).execute( (String) value ) ).collect(
                        Collectors.toList() );
                }
            }
            return values;
        }

    }

}
