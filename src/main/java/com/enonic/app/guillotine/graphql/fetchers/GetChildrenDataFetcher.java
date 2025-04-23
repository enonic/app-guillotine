package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentByParentResult;
import com.enonic.xp.index.ChildOrder;

public class GetChildrenDataFetcher
    extends BaseContentDataFetcher
{
    public GetChildrenDataFetcher( final ContentService contentService )
    {
        super( contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        ArgumentsValidator.validateArguments( environment.getArguments() );

        Content parent = getContent( environment, true );

        if ( parent != null )
        {
            Integer from = Objects.requireNonNullElse( environment.getArgument( "offset" ), 0 );
            Integer count = Objects.requireNonNullElse( environment.getArgument( "first" ), 10 );
            ChildOrder childOrder = ChildOrder.from( environment.getArgument( "sort" ) );

            FindContentByParentResult children = contentService.findByParent(
                FindContentByParentParams.create().parentId( parent.getId() ).from( from ).size( count ).childOrder( childOrder ).build() );

            final List<Map<String, Object>> data = new ArrayList<>( (int) children.getHits() );

            final Map<String, Content> contentsWithAttachments = new HashMap<>();

            children.getContents().forEach( content -> {
                data.add( GuillotineSerializer.serialize( content ) );

                if ( !content.getAttachments().isEmpty() )
                {
                    contentsWithAttachments.put( content.getId().toString(), content );
                }
            } );

            final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );
            newLocalContext.put( Constants.CONTENTS_WITH_ATTACHMENTS_FIELD, contentsWithAttachments );

            return DataFetcherResult.newResult().localContext( Collections.unmodifiableMap( newLocalContext ) ).data( data ).build();
        }

        return Collections.emptyList();
    }

}
