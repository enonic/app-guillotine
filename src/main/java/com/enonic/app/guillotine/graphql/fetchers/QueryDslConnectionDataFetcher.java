package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.commands.FindContentsCommand;
import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentService;

public class QueryDslConnectionDataFetcher
    extends QueryBaseDataFetcher
{
    private final ContentService contentService;

    public QueryDslConnectionDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        super( context );
        this.contentService = contentService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( DataFetchingEnvironment environment )
    {
        ArgumentsValidator.validateDslQuery( environment.getArguments() );

        int offset = environment.getArgument( "after" ) != null ?
            Integer.parseInt( ConnectionHelper.decodeCursor( environment.getArgument( "after" ) ) ) + 1 : 0;

        int first = environment.getArgument( "first" ) != null ? environment.getArgument( "first" ) : 10;

        Map<String, Object> queryResult =
            new FindContentsCommand( createQueryParams( offset, first, environment, true ), contentService ).execute();

        Map<String, Object> result = new HashMap<>();

        result.put( "total", queryResult.get( "total" ) );
        result.put( "start", offset );
        result.put( "hits", queryResult.get( "hits" ) );
        result.put( "aggregationsAsJson", queryResult.get( "aggregations" ) );
        result.put( "highlightAsJson", queryResult.get( "highlight" ) );

        return result;
    }
}
