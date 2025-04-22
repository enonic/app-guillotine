package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.Media;
import com.enonic.xp.portal.url.ImageUrlGeneratorParams;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.project.ProjectName;

public class GetImageUrlDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetImageUrlDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private String doGet( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> sourceAsMap = environment.getSource();
        final String contentId = sourceAsMap.get( "_id" ).toString();

        final Map<String, Object> localContext = environment.getLocalContext();

        final Map<String, Content> contents = (Map<String, Content>) localContext.get( Constants.CONTENTS_FIELD );

        if ( contents == null )
        {
            return null;
        }

        final Content content = contents.get( contentId );
        if ( content != null )
        {
            final String siteBaseUrl = GuillotineLocalContextHelper.getSiteBaseUrl( environment );

            final ProjectName projectName =
                ProjectName.from( GuillotineLocalContextHelper.getContextProperty( environment, Constants.PROJECT_ARG ) );

            final Branch branch = Branch.from( GuillotineLocalContextHelper.getContextProperty( environment, Constants.BRANCH_ARG ) );

            final ImageUrlGeneratorParams.Builder builder = ImageUrlGeneratorParams.create();

            builder.setMedia( () -> (Media) content );
            builder.setProjectName( () -> projectName );
            builder.setBranch( () -> branch );
            builder.setBaseUrl( siteBaseUrl );
            builder.setScale( environment.getArgument( "scale" ) );
            builder.setUrlType( environment.getArgument( "type" ) );
            builder.setQuality( environment.getArgument( "quality" ) );
            builder.setBackground( environment.getArgument( "background" ) );
            builder.setFormat( environment.getArgument( "format" ) );
            builder.setFilter( environment.getArgument( "filter" ) );

            final Object queryParams = environment.getArgument( "params" );
            if ( queryParams instanceof Map )
            {
                for ( Map.Entry<String, Object> entry : ( (Map<String, Object>) queryParams ).entrySet() )
                {
                    final Object value = entry.getValue();
                    if ( value instanceof Iterable )
                    {
                        ( (Iterable<?>) value ).forEach( v -> builder.addQueryParam( entry.getKey(), Objects.toString( v, null ) ) );
                    }
                    else
                    {
                        builder.addQueryParam( entry.getKey(), Objects.toString( value, null ) );
                    }
                }
            }

            final ImageUrlGeneratorParams params = builder.build();

            return portalUrlService.imageUrl( params );
        }
        else
        {
            // TODO Remove it when migration is complete
            throw new IllegalArgumentException( "Content is not an image" );
        }
    }
}
