package com.enonic.app.guillotine.graphql.transformer;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.enonic.app.guillotine.graphql.ArgumentsValidator;
import com.enonic.app.guillotine.graphql.commands.FindContentsCommand;
import com.enonic.app.guillotine.graphql.commands.FindContentsParams;
import com.enonic.app.guillotine.graphql.helper.AggregationHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.HighlightHelper;
import com.enonic.app.guillotine.graphql.helper.QueryDslHelper;
import com.enonic.app.guillotine.mapper.ContentMapper;
import com.enonic.app.guillotine.mapper.SiteMapper;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.project.ProjectConstants;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.site.Site;

public class GuillotineUtil
{
    private static final ResourceKey GUILLOTINE_RESOURCE_KEY = ResourceKey.from( ApplicationKey.from( "com.enonic.app.guillotine" ), "/" );

    private static final String KEY_IS_REQUIRED = "The \"key\" parameter is required";

    private final ContentService contentService;

    private final PortalScriptService scriptService;

    public GuillotineUtil( final ContentService contentService, final PortalScriptService scriptService )
    {
        this.contentService = contentService;
        this.scriptService = scriptService;
    }

    public Object getContent( String key, String project, String branch )
    {
        return executeInContext( Objects.requireNonNull( key, KEY_IS_REQUIRED ), project, branch, this::getContentByKey );
    }

    public Object getSite( String key, String project, String branch )
    {
        return executeInContext( Objects.requireNonNull( key, KEY_IS_REQUIRED ), project, branch, this::getSiteByKey );
    }

    public Object query( Map<String, Object> params, String project, String branch )
    {
        return createContext( project, branch ).callWith( () -> {
            ArgumentsValidator.validateDslQuery( params );
            return toNativeObject( new FindContentsCommand( createFindContentsParams( params ), contentService ).executeFromJS() );
        } );
    }

    private Object executeInContext( String key, String project, String branch, Function<String, Object> callback )
    {
        return createContext( project, branch ).callWith( () -> {
            try
            {
                return callback.apply( key );
            }
            catch ( ContentNotFoundException e )
            {
                return null;
            }
        } );
    }

    private Object getContentByKey( String key )
    {
        final Content content =
            key.startsWith( "/" ) ? contentService.getByPath( ContentPath.from( key ) ) : contentService.getById( ContentId.from( key ) );
        return toNativeObject( new ContentMapper( content ) );
    }

    private Object getSiteByKey( String key )
    {
        final Site site = key.startsWith( "/" )
            ? contentService.findNearestSiteByPath( ContentPath.from( key ) )
            : contentService.getNearestSite( ContentId.from( key ) );

        final SiteMapper object = new SiteMapper( site );
        return toNativeObject( object );
    }

    private Object toNativeObject( final Object object )
    {
        return scriptService.toNativeObject( GUILLOTINE_RESOURCE_KEY, object );
    }

    private Context createContext( String project, String branch )
    {
        final ContextBuilder contextBuilder = ContextBuilder.from( ContextAccessor.current() );

        if ( branch != null )
        {
            contextBuilder.branch( branch );
        }
        if ( project != null )
        {
            contextBuilder.repositoryId( ProjectConstants.PROJECT_REPO_ID_PREFIX + project );
        }

        return contextBuilder.build();
    }

    private FindContentsParams createFindContentsParams( final Map<String, Object> paramsAsMap )
    {
        FindContentsParams.Builder builder =
            FindContentsParams.create().setStart( CastHelper.cast( paramsAsMap.get( "offset" ) ) ).setFirst(
                CastHelper.cast( paramsAsMap.get( "first" ) ) );

        if ( paramsAsMap.get( "query" ) != null )
        {
            builder.setQuery( QueryDslHelper.createDslQuery( CastHelper.cast( paramsAsMap.get( "query" ) ) ) );
        }
        if ( paramsAsMap.get( "aggregations" ) != null )
        {
            builder.setAggregations( AggregationHelper.createAggregations( CastHelper.cast( paramsAsMap.get( "aggregations" ) ) ) );
        }
        if ( paramsAsMap.get( "highlight" ) != null )
        {
            builder.setHighlight( HighlightHelper.createHighlight( CastHelper.cast( paramsAsMap.get( "highlight" ) ) ) );
        }
        if ( paramsAsMap.get( "sort" ) != null )
        {
            if ( paramsAsMap.get( "sort" ) instanceof String )
            {
                builder.setSort( paramsAsMap.get( "sort" ) );

            }
            builder.setSort( paramsAsMap.get( "sort" ) );
        }
        return builder.build();
    }
}
