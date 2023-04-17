package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.helper.ConnectionHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentByParentResult;
import com.enonic.xp.index.ChildOrder;

public class GetChildrenConnectionDataFetcher
    extends BaseContentDataFetcher
{
    public GetChildrenConnectionDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        super( context, contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        ArgumentsValidator.validateArguments( environment.getArguments() );

        Map<String, Object> parentAsMap = getContent( environment, true );

        int offset = environment.getArgument( "after" ) != null ?
            Integer.parseInt( ConnectionHelper.decodeCursor( environment.getArgument( "after" ) ) ) + 1 : 0;

        if ( parentAsMap != null )
        {
            Integer count = Objects.requireNonNullElse( environment.getArgument( "first" ), 10 );
            ChildOrder childOrder = ChildOrder.from( environment.getArgument( "sort" ) );

            try
            {
                FindContentByParentResult children = contentService.findByParent(
                    FindContentByParentParams.create().parentId( ContentId.from( parentAsMap.get( "_id" ) ) ).from( offset ).size(
                        count ).childOrder( childOrder ).build() );

                return map( children.getTotalHits(), offset,
                            children.getContents().stream().map( ContentSerializer::serialize ).collect( Collectors.toList() ) );
            }
            catch ( final ContentNotFoundException e )
            {
                // do nothing
            }
        }
        return map( 0, offset, Collections.emptyList() );
    }

    private Map<String, Object> map( long total, int offset, List<Map<String, Object>> children )
    {
        Map<String, Object> result = new HashMap<>();

        result.put( "total", total );
        result.put( "start", offset );
        result.put( "hits", children );

        return result;
    }
}
