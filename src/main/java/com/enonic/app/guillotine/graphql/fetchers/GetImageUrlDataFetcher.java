package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
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

    @SuppressWarnings("unchecked")
    private String doGet( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> sourceAsMap = environment.getSource();
        if ( sourceAsMap == null )
        {
            return null;
        }

        final Content content =
            GuillotineLocalContextHelper.resolveContentWithAttachment( environment, sourceAsMap.get( "_id" ).toString() );

        if ( content == null )
        {
            return null;
        }

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

        if ( environment.getArgument( "params" ) instanceof Map queryParams )
        {
            builder.addQueryParams( ParamsUrHelper.convertToMultimap( queryParams ) );
        }

        final ImageUrlGeneratorParams params = builder.build();

        return portalUrlService.imageUrl( params );
    }
}
