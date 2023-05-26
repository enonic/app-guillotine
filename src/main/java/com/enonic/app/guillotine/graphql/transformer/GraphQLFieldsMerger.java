package com.enonic.app.guillotine.graphql.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;

import com.enonic.app.guillotine.graphql.BaseCreationCallbackParams;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GraphQLHelper;

public class GraphQLFieldsMerger
{
    public static List<GraphQLFieldDefinition> merge( List<GraphQLFieldDefinition> originalFields,
                                                      BaseCreationCallbackParams creationCallbackParams )
    {
        Map<String, GraphQLFieldDefinition> fieldsAsMap =
            originalFields.stream().collect( Collectors.toMap( GraphQLFieldDefinition::getName, Function.identity() ) );

        if ( creationCallbackParams.getAddFields() != null )
        {
            Map<String, Map<String, Object>> addFields = creationCallbackParams.getAddFields();

            addFields.forEach( ( fieldName, settings ) -> {
                GraphQLType type = CastHelper.cast( settings.get( "type" ) );
                List<GraphQLArgument> arguments = extractArguments( CastHelper.cast( settings.get( "args" ) ) );
                fieldsAsMap.put( fieldName, GraphQLHelper.outputField( fieldName, type, arguments ) );
            } );
        }

        if ( creationCallbackParams.getModifyFields() != null )
        {
            Map<String, Map<String, Object>> modifyFields = creationCallbackParams.getModifyFields();

            modifyFields.forEach( ( fieldName, settings ) -> {
                GraphQLFieldDefinition originalField = fieldsAsMap.get( fieldName );

                GraphQLType type = Objects.requireNonNullElse( CastHelper.cast( settings.get( "type" ) ), originalField.getType() );
                List<GraphQLArgument> arguments = Objects.requireNonNullElse( extractArguments( CastHelper.cast( settings.get( "args" ) ) ),
                                                                              originalField.getArguments() );

                fieldsAsMap.put( fieldName, GraphQLHelper.outputField( fieldName, type, arguments ) );
            } );
        }

        if ( creationCallbackParams.getRemoveFields() != null )
        {
            creationCallbackParams.getRemoveFields().forEach( fieldsAsMap::remove );
        }

        return new ArrayList<>( fieldsAsMap.values() );
    }

    private static List<GraphQLArgument> extractArguments( Map<String, Object> arguments )
    {
        if ( arguments != null )
        {
            return arguments.entrySet().stream().map(
                e -> GraphQLHelper.newArgument( e.getKey(), CastHelper.cast( e.getValue() ) ) ).collect( Collectors.toList() );
        }
        return null;
    }
}
