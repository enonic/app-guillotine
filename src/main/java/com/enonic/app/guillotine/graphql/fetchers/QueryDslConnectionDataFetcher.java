package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.commands.FindContentsCommand;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.QueryMapper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;

public class QueryDslConnectionDataFetcher
    extends QueryBaseDataFetcher
{
    private final ContentService contentService;

    public QueryDslConnectionDataFetcher( final ContentService contentService )
    {
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

        final FindContentIdsByQueryResult queryResult =
            new FindContentsCommand( createQueryParams( offset, first, environment ), contentService ).execute();

        final GuillotineMapGenerator generator = new GuillotineMapGenerator();

        if ( queryResult.getContentIds().isEmpty() )
        {
            new QueryMapper( Contents.empty(), queryResult ).serialize( generator );

            final Map<String, Object> serialized = CastHelper.cast( generator.getRoot() );

            Map<String, Object> result = new HashMap<>();

            result.put( "total", serialized.get( "total" ) );
            result.put( "start", offset );
            result.put( "hits", serialized.get( "hits" ) );
            result.put( "aggregationsAsJson", serialized.get( "aggregations" ) );
            result.put( "highlightAsJson", serialized.get( "highlight" ) );

            return result;
        }
        else {
            final Contents contents = this.contentService.getByIds( new GetContentByIdsParams( queryResult.getContentIds() ) );

            final Map<String, Content> contentsWithAttachments =
                contents.stream().filter( content -> !content.getAttachments().isEmpty() ).collect(
                    Collectors.toMap( content -> content.getId().toString(), Function.identity() ) );

            final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );
            newLocalContext.put( Constants.CONTENTS_WITH_ATTACHMENTS_FIELD, contentsWithAttachments );

            new QueryMapper( contents, queryResult ).serialize( generator );

            final Map<String, Object> serialized = CastHelper.cast( generator.getRoot() );

            final Map<String, Object> data = new HashMap<>();

            data.put( "total", serialized.get( "total" ) );
            data.put( "start", offset );
            data.put( "hits", serialized.get( "hits" ) );
            data.put( "aggregationsAsJson", serialized.get( "aggregations" ) );
            data.put( "highlightAsJson", serialized.get( "highlight" ) );

            return DataFetcherResult.newResult().localContext( Collections.unmodifiableMap( newLocalContext ) ).data( data ).build();
        }
    }
}
