package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentByParentResult;
import com.enonic.xp.index.ChildOrder;

public class GetContentChildrenDataFetcher
    implements DataFetcher<Object>
{
    private final ContentService contentService;

    public GetContentChildrenDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        ArgumentsValidator.validateArguments( environment.getArguments() );

        Map<String, Object> sourceAsMap = environment.getSource();

        Integer from = Objects.requireNonNullElse( environment.getArgument( "offset" ), 0 );
        Integer count = Objects.requireNonNullElse( environment.getArgument( "first" ), 10 );
        ChildOrder childOrder = ChildOrder.from( environment.getArgument( "sort" ) );

        try
        {
            FindContentByParentResult children = contentService.findByParent(
                FindContentByParentParams.create().parentId( ContentId.from( sourceAsMap.get( "_id" ) ) ).from( from ).size(
                    count ).childOrder( childOrder ).build() );

            return children.getContents().stream().map( ContentSerializer::serialize ).collect( Collectors.toList() );
        }
        catch ( final ContentNotFoundException e )
        {
            // do nothing
        }

        return Collections.emptyList();
    }
}
