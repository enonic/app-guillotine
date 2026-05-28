package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentIdsByParentResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.index.ChildOrder;

public class GetChildrenDataFetcher
    extends BaseContentDataFetcher
{
    private static final Logger LOG = LoggerFactory.getLogger( GetChildrenDataFetcher.class );

    public GetChildrenDataFetcher( final GuillotineContext context, final ContentService contentService )
    {
        super( context, contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        ArgumentsValidator.validateArguments( environment.getArguments() );

        final String key = resolveKeyFromArgumentOrLocalContext( environment );
        if ( key == null )
        {
            return Collections.emptyList();
        }

        try
        {
            final FindContentByParentParams params = buildParams( environment, key );
            final FindContentIdsByParentResult childrenIdsResult = contentService.findIdsByParent( params );

            return contentService.getByIds(
                GetContentByIdsParams.create().contentIds( childrenIdsResult.getContentIds() ).build() ).stream().map(
                GuillotineSerializer::serialize ).collect( Collectors.toList() );
        }
        catch ( final ContentNotFoundException e )
        {
            LOG.debug( "GetChildrenDataFetcher failed for key: {}", key, e );
            return Collections.emptyList();
        }
    }

    private FindContentByParentParams buildParams( final DataFetchingEnvironment environment, final String key )
    {
        final int from = Objects.requireNonNullElse( environment.getArgument( "offset" ), 0 );
        final int count = Objects.requireNonNullElse( environment.getArgument( "first" ), 10 );
        final ChildOrder childOrder = ChildOrder.from( environment.getArgument( "sort" ) );

        final FindContentByParentParams.Builder paramsBuilder =
            FindContentByParentParams.create().from( from ).size( count ).childOrder( childOrder );

        if ( key.startsWith( "/" ) )
        {
            paramsBuilder.parentPath( ContentPath.from( key ) );
        }
        else
        {
            paramsBuilder.parentId( ContentId.from( key ) );
        }

        return paramsBuilder.build();
    }
}
