package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.NodeMapper;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodeNotFoundException;

public class GetComponentsDataFetcher
    extends BasePageDataFetcher
{
    public GetComponentsDataFetcher( final ServiceFacade serviceFacade )
    {
        super( serviceFacade );
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


}
