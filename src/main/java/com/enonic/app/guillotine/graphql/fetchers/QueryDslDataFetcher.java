package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.commands.FindContentsCommand;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.form.PropertyTreeMarshallerService;

public class QueryDslDataFetcher
    extends QueryBaseDataFetcher
{
    private final ContentService contentService;

    private final PropertyTreeMarshallerService propertyTreeMarshallerService;

    public QueryDslDataFetcher( final ContentService contentService, final PropertyTreeMarshallerService propertyTreeMarshallerService )
    {
        this.contentService = contentService;
        this.propertyTreeMarshallerService = propertyTreeMarshallerService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        ArgumentsValidator.validateDslQuery( environment.getArguments() );

        Map<String, Object> queryResult = new FindContentsCommand(
            createQueryParams( environment.getArgument( "offset" ), environment.getArgument( "first" ), environment ), contentService,
            propertyTreeMarshallerService ).execute();

        return queryResult.get( "hits" );
    }
}
