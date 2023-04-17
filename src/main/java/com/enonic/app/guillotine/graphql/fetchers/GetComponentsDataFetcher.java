package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ArrayHelper;
import com.enonic.app.guillotine.graphql.CastHelper;
import com.enonic.app.guillotine.graphql.GuillotineMapGenerator;
import com.enonic.app.guillotine.graphql.ServiceFacade;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.mapper.ContentMapper;
import com.enonic.app.guillotine.mapper.NodeMapper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeNotFoundException;
import com.enonic.xp.page.GetDefaultPageTemplateParams;
import com.enonic.xp.page.PageTemplate;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.site.Site;

public class GetComponentsDataFetcher
    implements DataFetcher<Object>
{
    private final ServiceFacade serviceFacade;

    public GetComponentsDataFetcher( final ServiceFacade serviceFacade )
    {
        this.serviceFacade = serviceFacade;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = environment.getSource();

        boolean resolveTemplate = Objects.requireNonNullElse( environment.getArgument( "resolveTemplate" ), false );
        boolean resolveFragment = Objects.requireNonNullElse( environment.getArgument( "resolveFragment" ), false );

        Map<String, Object> pageTemplate = resolveTemplate ? resolvePageTemplate( sourceAsMap ) : null;

        String nodeId = CastHelper.cast( pageTemplate == null ? sourceAsMap.get( "_id" ) : pageTemplate.get( "_id" ) );

        Map<String, Object> nodeAsMap = getNode( nodeId );

        if ( nodeAsMap == null )
        {
            return null;
        }

        List<Map<String, Object>> components = CastHelper.cast( ArrayHelper.forceArray( nodeAsMap.get( "components" ) ) );

        if ( !resolveFragment )
        {
            return components;
        }

        return resolveInlineFragments( components );
    }

    private List<Map<String, Object>> resolveInlineFragments( List<Map<String, Object>> components )
    {
        List<Map<String, Object>> inlinedComponents = new ArrayList<>();

        components.forEach( component -> {
            if ( "fragment".equals( component.get( "type" ) ) )
            {
                Map<String, Object> fragment = CastHelper.cast( component.get( "fragment" ) );
                String fragmentId = CastHelper.cast( fragment.get( "id" ) );

                Map<String, Object> fragmentAsMap = getNode( fragmentId );

                if ( fragmentAsMap != null )
                {
                    List<Map<String, Object>> fragmentComponents =
                        CastHelper.cast( ArrayHelper.forceArray( fragmentAsMap.get( "components" ) ) );
                    fragmentComponents.forEach( fragmentComponent -> {
                        String path = "" + component.get( "path" ) +
                            ( "/".equals( fragmentComponent.get( "path" ) ) ? "" : fragmentComponent.get( "path" ) );
                        fragmentComponent.put( "path", path );

                        inlinedComponents.add( fragmentComponent );
                    } );
                }
            }
            else
            {
                inlinedComponents.add( component );
            }
        } );

        return inlinedComponents;
    }

    private Map<String, Object> resolvePageTemplate( Map<String, Object> contentAsMap )
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
                return CastHelper.cast( new GetContentCommand( serviceFacade.getContentService() ).execute( templateId ) );
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
                    GuillotineMapGenerator generator = new GuillotineMapGenerator();
                    new ContentMapper( defaultPageTemplate ).serialize( generator );
                    return CastHelper.cast( generator.getRoot() );
                }
            }

            return null;
        } );
    }

    private Map<String, Object> getNode( String nodeId )
    {
        try
        {
            Node node = serviceFacade.getNodeService().getById( NodeId.from( nodeId ) );
            if ( node == null )
            {
                return null;
            }

            GuillotineMapGenerator generator = new GuillotineMapGenerator();
            new NodeMapper( node ).serialize( generator );
            return CastHelper.cast( generator.getRoot() );
        }
        catch ( final NodeNotFoundException e )
        {
            return null;
        }
    }

    private <T> T adminContext( final Callable<T> callable )
    {
        Context context = ContextAccessor.current();
        return ContextBuilder.from( context ).authInfo(
            AuthenticationInfo.copyOf( context.getAuthInfo() ).principals( RoleKeys.CONTENT_MANAGER_ADMIN ).build() ).build().callWith(
            callable );
    }
}
