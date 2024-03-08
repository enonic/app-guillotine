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

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.graphql.OutputObjectCreationCallbackParams;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.graphql.helper.GraphQLHelper;

public class GraphQLFieldsMerger
{
	public static List<GraphQLFieldDefinition> merge( String typeName, List<GraphQLFieldDefinition> originalFields,
													  OutputObjectCreationCallbackParams creationCallbackParams,
													  GuillotineConfigService guillotineConfigService )
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
				if ( originalField != null )
				{
					GraphQLType type = Objects.requireNonNullElse( CastHelper.cast( settings.get( "type" ) ), originalField.getType() );
					List<GraphQLArgument> arguments =
						Objects.requireNonNullElse( extractArguments( CastHelper.cast( settings.get( "args" ) ) ),
													originalField.getArguments() );

					fieldsAsMap.put( fieldName, GraphQLHelper.outputField( fieldName, type, arguments ) );
				}
				else if ( guillotineConfigService.isThrowErrorOnModifyingUnknownFields() )
				{
					throw new IllegalArgumentException( String.format( "Field '%s' does not exist in type '%s'.", fieldName, typeName ) );
				}
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
