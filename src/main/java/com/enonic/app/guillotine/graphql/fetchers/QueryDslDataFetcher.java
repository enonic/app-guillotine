package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.commands.FindContentsCommand;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.sortvalues.SortValuesProperty;

public class QueryDslDataFetcher
    extends QueryBaseDataFetcher
{
    private final ContentService contentService;

    public QueryDslDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
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
            createQueryParams( environment.getArgument( "offset" ), environment.getArgument( "first" ), environment ),
            contentService ).execute();

        return queryResult.get( "hits" );
    }

//    private Object doGet( final DataFetchingEnvironment environment )
//    {
//        ArgumentsValidator.validateDslQuery( environment.getArguments() );
//
//        final FindContentIdsByQueryResult queryResult = new FindContentsCommand(
//            createQueryParams( environment.getArgument( "offset" ), environment.getArgument( "first" ), environment ),
//            contentService ).execute();
//
//        final ContentIds contentIds = queryResult.getContentIds();
//
//        if ( contentIds.isEmpty() )
//        {
//            return List.of();
//        }
//
//        final Contents contents = this.contentService.getByIds( new GetContentByIdsParams( contentIds ) );
//
//        final List<Map<String, Object>> data = new ArrayList<>( (int) queryResult.getHits() );
//
//        final Map<String, Content> contentsWithAttachments = new HashMap<>();
//
//        contents.forEach( content -> {
//            final Float contentScore = queryResult.getScore() != null ? queryResult.getScore().get( content.getId() ) : null;
//
//            final SortValuesProperty contentSort = queryResult.getSort() != null ? queryResult.getSort().get( content.getId() ) : null;
//
//            data.add( GuillotineSerializer.serialize( content, contentSort, contentScore ) );
//
//            if ( !content.getAttachments().isEmpty() )
//            {
//                contentsWithAttachments.put( content.getId().toString(), content );
//            }
//        } );
//
//        final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );
//        newLocalContext.put( Constants.CONTENTS_WITH_ATTACHMENTS_FIELD, contentsWithAttachments );
//
//        return DataFetcherResult.newResult().localContext( Collections.unmodifiableMap( newLocalContext ) ).data( data ).build();
//    }
}
