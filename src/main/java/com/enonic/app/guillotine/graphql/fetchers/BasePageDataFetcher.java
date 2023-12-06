package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.concurrent.Callable;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.page.GetDefaultPageTemplateParams;
import com.enonic.xp.page.PageTemplate;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.site.Site;

public abstract class BasePageDataFetcher
    implements DataFetcher<Object>
{
    protected final ServiceFacade serviceFacade;

    protected BasePageDataFetcher( final ServiceFacade serviceFacade )
    {
        this.serviceFacade = serviceFacade;
    }

    protected Map<String, Object> resolvePageTemplate( Map<String, Object> contentAsMap, DataFetchingEnvironment environment )
    {
        if ( "portal:page-template".equals( contentAsMap.get( "type" ) ) )
        {
            return contentAsMap;
        }

        Map<String, Object> page = CastHelper.cast( contentAsMap.get( "page" ) );

        if ( page != null )
        {
            if ( page.isEmpty() )
            {
                return getDefaultPageTemplate( contentAsMap );
            }

            if ( page.get( "template" ) != null )
            {
                String templateId = CastHelper.cast( page.get( "template" ) );
                return CastHelper.cast( new GetContentCommand( serviceFacade.getContentService() ).execute( templateId, environment ) );
            }
        }

        return null;
    }

    private Map<String, Object> getDefaultPageTemplate( Map<String, Object> contentAsMap )
    {
        return adminContext( () -> {
            Site nearestSite = serviceFacade.getContentService().getNearestSite( ContentId.from( contentAsMap.get( "_id" ) ) );

            if ( nearestSite != null )
            {
                String contentType = CastHelper.cast( contentAsMap.get( "type" ) );

                GetDefaultPageTemplateParams params =
                    GetDefaultPageTemplateParams.create().contentType( ContentTypeName.from( contentType ) ).site(
                        nearestSite.getId() ).build();

                PageTemplate defaultPageTemplate = serviceFacade.getPageTemplateService().getDefault( params );

                if ( defaultPageTemplate != null )
                {
                    return GuillotineSerializer.serialize( defaultPageTemplate );
                }
            }

            return null;
        } );
    }

    private <T> T adminContext( final Callable<T> callable )
    {
        Context context = ContextAccessor.current();
        return ContextBuilder.from( context ).authInfo(
            AuthenticationInfo.copyOf( context.getAuthInfo() ).principals( RoleKeys.CONTENT_MANAGER_ADMIN ).build() ).build().callWith(
            callable );
    }
}
