package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.typeresolvers.ContentTypeResolver;

/**
 * The {@code __typename} that content of this content type has in query results.
 */
public class GetGraphQLTypeNameDataFetcher
    implements DataFetcher<String>
{
    private final GuillotineContext context;

    public GetGraphQLTypeNameDataFetcher( final GuillotineContext context )
    {
        this.context = context;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
    {
        final Map<String, Object> sourceAsMap = environment.getSource();
        final Object contentTypeName = sourceAsMap.get( "name" );
        final GraphQLObjectType type = ContentTypeResolver.resolve( context, environment.getGraphQLSchema(),
                                                                    contentTypeName != null ? contentTypeName.toString() : null );
        return type != null ? type.getName() : null;
    }
}
