package com.enonic.app.guillotine.graphql.fetchers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.ImageTransformations;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.Media;
import com.enonic.xp.portal.url.ImageUrlPartsParams;
import com.enonic.xp.portal.url.PortalUrlService;

public class GetImageUrlDataFetcher
    implements DataFetcher<Map<String, Object>>
{
    private static final List<String> TRANSFORMATION_ARGUMENTS = List.of( "scale", "quality", "background", "format", "filter" );

    private static final String FULL_SCALE = "full";

    private final PortalUrlService portalUrlService;

    public GetImageUrlDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public Map<String, Object> get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private Map<String, Object> doGet( final DataFetchingEnvironment environment )
    {
        for ( final String argument : TRANSFORMATION_ARGUMENTS )
        {
            if ( environment.containsArgument( argument ) )
            {
                ImageTransformations.check( environment, argument );
            }
        }

        final Content content = GuillotineLocalContextHelper.resolveContent( environment );

        if ( content == null )
        {
            return null;
        }

        final Map<String, Object> result = UrlPartsHelper.anyImagePartSelected( environment.getSelectionSet() )
            ? UrlPartsHelper.toMap( portalUrlService.imageUrlParts( buildParams( environment, content ) ) )
            : new LinkedHashMap<>();

        return result;
    }

    @SuppressWarnings("unchecked")
    private static ImageUrlPartsParams buildParams( final DataFetchingEnvironment environment, final Content content )
    {
        final ImageUrlPartsParams.Builder builder = ImageUrlPartsParams.create();

        builder.setMedia( () -> (Media) content );
        builder.setProjectName( () -> GuillotineLocalContextHelper.getProjectName( environment ) );
        builder.setBranch( () -> GuillotineLocalContextHelper.getBranch( environment ) );
        builder.setScale( Objects.requireNonNullElse( environment.getArgument( "scale" ), FULL_SCALE ) );
        builder.setQuality( environment.getArgument( "quality" ) );
        builder.setBackground( environment.getArgument( "background" ) );
        builder.setFormat( environment.getArgument( "format" ) );
        builder.setFilter( environment.getArgument( "filter" ) );

        return builder.build();
    }
}
