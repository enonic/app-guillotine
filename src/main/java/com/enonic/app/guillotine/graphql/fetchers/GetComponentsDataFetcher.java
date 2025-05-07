package com.enonic.app.guillotine.graphql.fetchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.ArrayHelper;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
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
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Object doGet( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> sourceAsMap = environment.getSource();

        if ( sourceAsMap == null )
        {
            return null;
        }

        final boolean resolveTemplate = Objects.requireNonNullElse( environment.getArgument( "resolveTemplate" ), false );
        final boolean resolveFragment = Objects.requireNonNullElse( environment.getArgument( "resolveFragment" ), false );

        final Map<String, Object> pageTemplate = resolveTemplate ? resolvePageTemplate( sourceAsMap, environment ) : null;

        final String nodeId = CastHelper.cast( pageTemplate == null ? sourceAsMap.get( "_id" ) : pageTemplate.get( "_id" ) );

        final Map<String, Object> nodeAsMap = getNode( nodeId );

        if ( nodeAsMap == null )
        {
            return null;
        }

        List<Map<String, Object>> components = CastHelper.cast( ArrayHelper.forceArray( nodeAsMap.get( "components" ) ) );

        if ( !resolveFragment )
        {
            if ( nodeId.equals( sourceAsMap.get( "_id" ) ) )
            {
                return components;
            }
            else
            {
                return buildDataFetcherResult( pageTemplate, environment ).data( components ).build();
            }
        }
        else
        {
            return resolveInlineFragments( components, environment );
        }
    }

    private List<Object> resolveInlineFragments( final List<Map<String, Object>> components, final DataFetchingEnvironment environment )
    {
        List<Object> inlinedComponents = new ArrayList<>();

        components.forEach( component -> {
            if ( "fragment".equals( component.get( "type" ) ) )
            {
                Map<String, Object> fragment = CastHelper.cast( component.get( "fragment" ) );
                String fragmentId = CastHelper.cast( fragment.get( "id" ) );

                Map<String, Object> fragmentAsMap = getNode( fragmentId );

                if ( fragmentAsMap != null )
                {
                    final Map<String, Object> currentContent =
                        new GetContentCommand( serviceFacade.getContentService() ).execute( fragmentId, environment );

                    final List<Map<String, Object>> fragmentComponents =
                        CastHelper.cast( ArrayHelper.forceArray( fragmentAsMap.get( "components" ) ) );

                    fragmentComponents.forEach( fragmentComponent -> {
                        String path = "" + component.get( "path" ) +
                            ( "/".equals( fragmentComponent.get( "path" ) ) ? "" : fragmentComponent.get( "path" ) );
                        fragmentComponent.put( "path", path );

                        inlinedComponents.add( buildDataFetcherResult( currentContent, environment ).data( fragmentComponent ).build() );
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

    private DataFetcherResult.Builder<Object> buildDataFetcherResult( final Map<String, Object> currentContent,
                                                                      final DataFetchingEnvironment environment )
    {
        final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );
        newLocalContext.put( Constants.CURRENT_CONTENT_FIELD, currentContent );

        return DataFetcherResult.newResult().localContext( Collections.unmodifiableMap( newLocalContext ) );
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
