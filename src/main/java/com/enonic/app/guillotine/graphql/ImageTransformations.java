package com.enonic.app.guillotine.graphql;

import java.util.Locale;
import java.util.Objects;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;

/**
 * Whether a query may ask for image transformations: the scale, quality, background, format and filter of
 * {@code imageUrl}, and the widths of rich text images. XP signs every image URL it generates, so a query that may ask
 * for any transformation can have any image variant signed.
 */
public enum ImageTransformations
{
    ALLOWED, DENIED;

    /**
     * Name of the context attribute, set by a virtual host mapping, that decides it for the requests the mapping
     * matches: {@code allow}, {@code deny} or {@code auto}, the default.
     */
    public static final String CONTEXT_ATTRIBUTE = "guillotine.imageTransformations";

    /**
     * @return whether a query received over HTTP may ask for image transformations. With {@code auto}, a query sent to
     * Guillotine's API endpoint or in the admin may, and one sent to Guillotine mounted on a site or a webapp may not
     */
    public static ImageTransformations ofRequest()
    {
        final String setting = Objects.toString( ContextAccessor.current().getAttribute( CONTEXT_ATTRIBUTE ), "" ).strip();
        return switch ( setting.toLowerCase( Locale.ROOT ) )
        {
            case "allow" -> ALLOWED;
            case "deny" -> DENIED;
            default -> auto();
        };
    }

    private static ImageTransformations auto()
    {
        final PortalRequest portalRequest = PortalRequestAccessor.get();
        if ( portalRequest == null )
        {
            return ALLOWED;
        }
        final String baseUri = Objects.toString( portalRequest.getBaseUri(), "" );
        return baseUri.startsWith( "/api/" ) || baseUri.startsWith( "/admin/" ) ? ALLOWED : DENIED;
    }

    /**
     * @return the setting the query runs with; a query without one, such as one run in-process, may ask for any
     */
    public static ImageTransformations of( final DataFetchingEnvironment environment )
    {
        final GraphQLContext graphQLContext = environment.getGraphQlContext();
        final ImageTransformations value = graphQLContext == null ? null : graphQLContext.get( ImageTransformations.class );
        return value == null ? ALLOWED : value;
    }

    /**
     * @throws IllegalArgumentException when the query may not ask for image transformations and the argument is given
     */
    public static void check( final DataFetchingEnvironment environment, final String argument )
    {
        if ( of( environment ) == DENIED )
        {
            throw new IllegalArgumentException( "Image transformations are not allowed here: remove the '" + argument + "' argument" );
        }
    }
}
