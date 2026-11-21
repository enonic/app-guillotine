package com.enonic.app.guillotine.graphql;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;

/**
 * Whether Guillotine's data fetchers may read with more rights than the caller has. Over HTTP they may: the default page template
 * is resolved with the Content Studio administrator role, and the {@code scopeKey} argument is checked with XP's
 * {@code contentExists}, which ignores read permissions. A query run with {@link #NONE} reads both with the caller's permissions
 * only, so content the caller cannot read looks missing.
 */
public enum Elevation
{
    ALLOWED, NONE;

    /**
     * @return the setting the query runs with; a query without one may elevate
     */
    public static Elevation of( final DataFetchingEnvironment environment )
    {
        final GraphQLContext graphQLContext = environment.getGraphQlContext();
        final Elevation value = graphQLContext == null ? null : graphQLContext.get( Elevation.class );
        return value == null ? ALLOWED : value;
    }
}
