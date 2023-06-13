package com.enonic.app.guillotine.graphql.typeresolvers;

import java.util.Map;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
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
        String contentTypeName = context.getContentType( sourceAsMap.get( "type" ).toString() );
        GraphQLObjectType contentType = env.getSchema().getObjectType( contentTypeName );
        return contentType != null ? contentType : env.getSchema().getObjectType( "UntypedContent" );
    }
}
