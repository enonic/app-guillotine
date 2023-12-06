package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentByParentResult;
import com.enonic.xp.index.ChildOrder;

public class GetContentChildrenConnectionDataFetcher
    implements DataFetcher<Object>
{
    private final ContentService contentService;

    public GetContentChildrenConnectionDataFetcher( final ContentService contentService )
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
        ArgumentsValidator.validateArguments( environment.getArguments() );

        Map<String, Object> contentAsMap = environment.getSource();

        int offset = environment.getArgument( "after" ) != null ?
            Integer.parseInt( ConnectionHelper.decodeCursor( environment.getArgument( "after" ) ) ) + 1 : 0;

        Integer count = Objects.requireNonNullElse( environment.getArgument( "first" ), 10 );
        ChildOrder childOrder = ChildOrder.from( environment.getArgument( "sort" ) );

        FindContentByParentResult children = contentService.findByParent(
            FindContentByParentParams.create().parentId( ContentId.from( contentAsMap.get( "_id" ) ) ).from( offset ).size(
                count ).childOrder( childOrder ).build() );

        return map( children.getTotalHits(), offset,
                    children.getContents().stream().map( GuillotineSerializer::serialize ).collect( Collectors.toList() ) );
    }

    private Map<String, Object> map( long total, int offset, List<Map<String, Object>> children )
    {
        Map<String, Object> result = new HashMap<>();

        result.put( "total", Long.valueOf( total ).intValue() );
        result.put( "start", offset );
        result.put( "hits", children );

        return result;
    }
}
