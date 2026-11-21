package com.enonic.app.guillotine.graphql.typeresolvers;

import java.util.Map;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;

import com.enonic.app.guillotine.graphql.GuillotineContext;

public class ContentTypeResolver
    implements TypeResolver
{
    private final GuillotineContext context;

    public ContentTypeResolver( final GuillotineContext context )
    {
        this.context = context;
    }

    @Override
    public GraphQLObjectType getType( final TypeResolutionEnvironment env )
    {
        Map<String, Object> sourceAsMap = env.getObject();
        return resolve( context, env.getSchema(), sourceAsMap.get( "type" ).toString() );
    }

    /**
     * The GraphQL type of content of {@code contentTypeName}, which is its {@code __typename}.
     */
    public static GraphQLObjectType resolve( final GuillotineContext context, final GraphQLSchema schema, final String contentTypeName )
    {
        String typeName = contentTypeName != null ? context.getContentType( contentTypeName ) : null;
        GraphQLObjectType contentType = typeName != null ? schema.getObjectType( typeName ) : null;
        return contentType != null ? contentType : schema.getObjectType( "UntypedContent" );
    }
}
