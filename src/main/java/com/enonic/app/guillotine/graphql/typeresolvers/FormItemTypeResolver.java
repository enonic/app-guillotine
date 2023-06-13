package com.enonic.app.guillotine.graphql.typeresolvers;

import java.util.Map;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.helper.CastHelper;

public class FormItemTypeResolver
    implements TypeResolver
{
    private final GuillotineContext context;

    public FormItemTypeResolver( final GuillotineContext context )
    {
        this.context = context;
    }

    @Override
    public GraphQLObjectType getType( final TypeResolutionEnvironment env )
    {
        Object source = env.getObject();
        if ( source instanceof Map )
        {
            Map<String, Object> sourceAsMap = CastHelper.cast( source );
            switch ( sourceAsMap.get( "formItemType" ).toString() )
            {
                case "ItemSet":
                    return context.getOutputType( "FormItemSet" );
                case "Layout":
                    return context.getOutputType( "FormLayout" );
                case "Input":
                    return context.getOutputType( "FormInput" );
                case "OptionSet":
                    return context.getOutputType( "FormOptionSet" );
            }
        }
        return null;
    }
}
