package com.enonic.app.guillotine.graphql.fetchers;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.helper.ArrayHelper;
import com.enonic.app.guillotine.helper.CastHelper;

public class GetPageAsJsonDataFetcher
    extends BasePageDataFetcher
{
    public GetPageAsJsonDataFetcher( final ServiceFacade serviceFacade )
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

        Map<String, Object> page = CastHelper.cast( pageTemplate == null ? sourceAsMap.get( "page" ) : pageTemplate.get( "page" ) );

        if ( resolveFragment )
        {
            inlineFragmentContentComponents( page );
        }

        return page;
    }

    private void inlineFragmentContentComponents( Map<String, Object> sourceAsMap )
    {
        if ( sourceAsMap != null && sourceAsMap.get( "regions" ) != null )
        {
            Map<String, Object> regions = CastHelper.cast( sourceAsMap.get( "regions" ) );

            regions.keySet().forEach( regionName -> {
                Map<String, Object> region = CastHelper.cast( regions.get( regionName ) );

                List<Map<String, Object>> components = CastHelper.cast( ArrayHelper.forceArray( region.get( "components" ) ) );

                for ( int i = 0; i < components.size(); i++ )
                {
                    Map<String, Object> component = components.get( i );

                    if ( "fragment".equals( component.get( "type" ) ) && component.get( "fragment" ) != null )
                    {
                        Map<String, Object> fragmentComponent =
                            new GetContentCommand( serviceFacade.getContentService() ).execute( component.get( "fragment" ).toString() );

                        if ( fragmentComponent != null )
                        {
                            Map<String, Object> fragment = CastHelper.cast( fragmentComponent.get( "fragment" ) );

                            fragment.put( "path", component.get( "path" ) );
                            prefixContentComponentPaths( component, CastHelper.cast( component.get( "path" ) ) );

                            components.set( i, fragment );

                            //No need to call recursively as a fragment cannot contain a fragment
                        }
                    }
                    else if ( "layout".equals( component.get( "type" ) ) )
                    {
                        inlineFragmentContentComponents( component );
                    }
                }
            } );
        }
    }

    private void prefixContentComponentPaths( Map<String, Object> sourceAsMap, String prefix )
    {
        if ( sourceAsMap != null && sourceAsMap.get( "regions" ) != null )
        {
            Map<String, Object> regions = CastHelper.cast( sourceAsMap.get( "regions" ) );

            regions.keySet().forEach( regionName -> {
                Map<String, Object> region = CastHelper.cast( regions.get( regionName ) );

                List<Map<String, Object>> components = CastHelper.cast( ArrayHelper.forceArray( region.get( "components" ) ) );

                components.forEach( component -> {
                    component.put( "path", prefix + component.get( "path" ) );

                    if ( "layout".equals( component.get( "type" ) ) )
                    {
                        prefixContentComponentPaths( component, prefix );
                    }
                } );
            } );
        }
    }
}
