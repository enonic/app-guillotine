package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

/**
 * The GraphQL type that content of this content type has in query results: the same lookup {@code ContentTypeResolver} does.
 */
public class GetGraphQLTypeNameDataFetcher
    implements DataFetcher<String>
{
    private static final String UNTYPED_CONTENT = "UntypedContent";

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
        final String typeName = contentTypeName != null ? context.getContentType( contentTypeName.toString() ) : null;
        final GraphQLObjectType type = typeName != null ? environment.getGraphQLSchema().getObjectType( typeName ) : null;
        return type != null ? type.getName() : UNTYPED_CONTENT;
    }
}
