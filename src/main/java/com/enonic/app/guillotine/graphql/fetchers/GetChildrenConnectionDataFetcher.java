package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.ConnectionHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentByParentParams;
import com.enonic.xp.content.FindContentIdsByParentResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.index.ChildOrder;

public class GetChildrenConnectionDataFetcher
    extends BaseContentDataFetcher
{
    private static final Logger LOG = LoggerFactory.getLogger( GetChildrenConnectionDataFetcher.class );

    public GetChildrenConnectionDataFetcher( final ContentService contentService )
    {
        super( contentService );
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Map<String, Object> doGet( final DataFetchingEnvironment environment )
    {
        ArgumentsValidator.validateArguments( environment.getArguments() );

        final String key = resolveKeyFromArgumentOrLocalContext( environment );
        final int offset = parseOffset( environment );

        if ( key == null )
        {
            return createConnectionMap( 0, offset, Collections.emptyList() );
        }

        try
        {
            final FindContentIdsByParentResult childrenIdsResult =
                contentService.findIdsByParent( buildParams( environment, key, offset ) );

            final List<Object> serializedContents = contentService.getByIds(
                GetContentByIdsParams.create().contentIds( childrenIdsResult.getContentIds() ).build() ).stream().map(
                GuillotineSerializer::serialize ).collect( Collectors.toList() );

            return createConnectionMap( childrenIdsResult.getTotalHits(), offset, serializedContents );
        }
        catch ( final ContentNotFoundException e )
        {
            LOG.debug( "GetChildrenConnectionDataFetcher failed for key: {}", key, e );
            return createConnectionMap( 0, offset, Collections.emptyList() );
        }
    }

    private int parseOffset( final DataFetchingEnvironment environment )
    {
        final String afterArg = environment.getArgument( "after" );
        return afterArg != null ? Integer.parseInt( ConnectionHelper.decodeCursor( afterArg ) ) + 1 : 0;
    }

    private FindContentByParentParams buildParams( final DataFetchingEnvironment environment, final String key, final int offset )
    {
        final int count = Objects.requireNonNullElse( environment.getArgument( "first" ), 10 );
        final ChildOrder childOrder = ChildOrder.from( environment.getArgument( "sort" ) );

        final FindContentByParentParams.Builder paramsBuilder =
            FindContentByParentParams.create().from( offset ).size( count ).childOrder( childOrder );

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

    private Map<String, Object> createConnectionMap( long total, int offset, List<?> children )
    {
        return Map.of( "total", Math.toIntExact( total ), "start", offset, "hits", children );
    }
}
