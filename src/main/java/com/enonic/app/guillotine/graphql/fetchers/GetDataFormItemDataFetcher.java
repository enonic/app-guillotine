package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.helper.CastHelper;
import com.enonic.app.guillotine.graphql.FormItemDataFetcher;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.xp.form.FormItem;

public class GetDataFormItemDataFetcher
    implements DataFetcher<Object>
{
    private final FormItem formItem;

    private final ServiceFacade serviceFacade;

    public GetDataFormItemDataFetcher( final FormItem formItem, final ServiceFacade serviceFacade )
    {
        this.formItem = formItem;
        this.serviceFacade = serviceFacade;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();
        Map<String, Object> contentAsMap =
            sourceAsMap.containsKey( "__contentWithAttachments" ) ? CastHelper.cast( sourceAsMap.get( "__contentWithAttachments" ) ) : null;
        return new FormItemDataFetcher( contentAsMap, formItem, serviceFacade ).get( environment );
    }
}
