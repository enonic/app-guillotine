package com.enonic.app.guillotine.graphql.fetchers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
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

    private final GuillotineContext guillotineContext;

    public FormItemDataFetcher( final FormItem formItem, final ServiceFacade serviceFacade, final GuillotineContext guillotineContext )

    {
        this.formItem = formItem;
        this.serviceFacade = serviceFacade;
        this.guillotineContext = guillotineContext;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();

        Occurrences occurrences = FormItemTypesHelper.getOccurrences( formItem );

        Map<String, Object> localContext = environment.getLocalContext();

        if ( occurrences.getMaximum() == 1 )
        {
            Object value = sourceAsMap.get( formItem.getName() );

            if ( formItem.getType() == FormItemType.INPUT && value != null )
            {
                InputTypeName inputType = ( (Input) formItem ).getInputType();
                if ( inputType.equals( InputTypeName.HTML_AREA ) )
                {
                    return new RichTextDataFetcher( (String) value, serviceFacade, guillotineContext ).execute( environment );
                }
                if ( inputType.equals( InputTypeName.ATTACHMENT_UPLOADER ) )
                {
                    Map<String, Object> attachmentsAsMap = CastHelper.cast( localContext.get( Constants.ATTACHMENTS_FIELD ) );
                    return attachmentsAsMap.get( (String) value );
                }
                if ( inputType.equals( InputTypeName.CONTENT_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_SELECTOR ) ||
                    inputType.equals( InputTypeName.IMAGE_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_UPLOADER ) )
                {
                    return getContentAsMap( (String) value, environment );
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
                        value -> new RichTextDataFetcher( (String) value, serviceFacade, guillotineContext ).execute(
                            environment ) ).collect( Collectors.toList() );
                }
                if ( inputType.equals( InputTypeName.ATTACHMENT_UPLOADER ) )
                {
                    Map<String, Object> attachmentsAsMap = CastHelper.cast( localContext.get( Constants.ATTACHMENTS_FIELD ) );
                    return values.stream().map( value -> attachmentsAsMap.get( (String) value ) ).collect( Collectors.toList() );
                }
                if ( inputType.equals( InputTypeName.CONTENT_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_SELECTOR ) ||
                    inputType.equals( InputTypeName.IMAGE_SELECTOR ) || inputType.equals( InputTypeName.MEDIA_UPLOADER ) )
                {
                    return values.stream().map( value -> getContentAsMap( (String) value, environment ) ).collect( Collectors.toList() );
                }
            }
            return values;
        }

    }

    private Map<String, Object> getContentAsMap( final String contentId, final DataFetchingEnvironment environment )
    {
        return new GetContentCommand( serviceFacade.getContentService() ).execute( contentId, environment );
    }

}
