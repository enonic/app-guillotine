package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.ContentSerializer;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentByParentResult;
import com.enonic.xp.index.ChildOrder;

public class GetChildrenDataFetcher
    extends BaseContentDataFetcher
{
    public GetChildrenDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        super( context, contentService );
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

        Map<String, Object> parentAsMap = getContent( environment, true );

        if ( parentAsMap != null )
        {
            Integer from = Objects.requireNonNullElse( environment.getArgument( "offset" ), 0 );
            Integer count = Objects.requireNonNullElse( environment.getArgument( "first" ), 10 );
            ChildOrder childOrder = ChildOrder.from( environment.getArgument( "sort" ) );

            try
            {
                FindContentByParentResult children = contentService.findByParent(
                    FindContentByParentParams.create().parentId( ContentId.from( parentAsMap.get( "_id" ) ) ).from( from ).size(
                        count ).childOrder( childOrder ).build() );

                return children.getContents().stream().map( ContentSerializer::serialize ).collect( Collectors.toList() );
            }
            catch ( final ContentNotFoundException e )
            {
                // do nothing
            }
        }
        return Collections.emptyList();
    }

}
