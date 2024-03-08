package com.enonic.app.guillotine.graphql.transformer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.graphql.OutputObjectCreationCallbackParams;

public class ExtensionGraphQLTypeVisitor
    extends GraphQLTypeVisitorStub
{
    private final Map<String, List<OutputObjectCreationCallbackParams>> creationCallbacks;

	private final GuillotineConfigService guillotineConfigService;

	public ExtensionGraphQLTypeVisitor( final Map<String, List<OutputObjectCreationCallbackParams>> creationCallbacksDefs,
										final GuillotineConfigService guillotineConfigService )
	{
		this.creationCallbacks = creationCallbacksDefs;
		this.guillotineConfigService = guillotineConfigService;
	}

    @Override
    public TraversalControl visitGraphQLInterfaceType( final GraphQLInterfaceType node,
                                                       final TraverserContext<GraphQLSchemaElement> context )
    {
        if ( creationCallbacks != null && creationCallbacks.get( node.getName() ) != null )
        {
            GraphQLInterfaceType transformedNode = node;

            for ( OutputObjectCreationCallbackParams creationCallbackParams : creationCallbacks.get( transformedNode.getName() ) )
            {
                String newDescription =
                    Objects.requireNonNullElse( creationCallbackParams.getDescription(), transformedNode.getDescription() );

                List<GraphQLFieldDefinition> newFields =
                    GraphQLFieldsMerger.merge( node.getName(), transformedNode.getFieldDefinitions(), creationCallbackParams, guillotineConfigService );

                transformedNode = transformedNode.transform( builder -> builder.description( newDescription ).fields( newFields ) );
            }

            return changeNode( context, transformedNode );
        }

        return super.visitGraphQLInterfaceType( node, context );
    }

    @Override
    public TraversalControl visitGraphQLObjectType( final GraphQLObjectType node, final TraverserContext<GraphQLSchemaElement> context )
    {
        if ( creationCallbacks != null )
        {
            GraphQLObjectType transformedNode = node;

            boolean transformedCount = false;

            if ( creationCallbacks.get( transformedNode.getName() ) != null )
            {
                for ( OutputObjectCreationCallbackParams creationCallbackParams : creationCallbacks.get( transformedNode.getName() ) )
                {
                    transformedNode = transformGraphQLObjectType( transformedNode, transformedNode, creationCallbackParams );
                    transformedCount = true;
                }
            }

            Set<String> interfaceNames =
                transformedNode.getInterfaces().stream().map( GraphQLNamedOutputType::getName ).collect( Collectors.toSet() );

            for ( String interfaceName : interfaceNames )
            {
                List<OutputObjectCreationCallbackParams> creationCallback = creationCallbacks.get( interfaceName );
                if ( creationCallback != null )
                {
                    for ( OutputObjectCreationCallbackParams creationCallbackParams : creationCallback )
                    {
                        List<GraphQLFieldDefinition> newFields =
							GraphQLFieldsMerger.merge( interfaceName, transformedNode.getFieldDefinitions(), creationCallbackParams,
													   guillotineConfigService );

                        transformedNode = transformedNode.transform( builder -> builder.replaceFields( newFields ) );
                        transformedCount = true;
                    }
                }
            }

            if ( transformedCount )
            {
                return changeNode( context, transformedNode );
            }
        }

        return super.visitGraphQLObjectType( node, context );
    }

    private GraphQLObjectType transformGraphQLObjectType( final GraphQLObjectType node, GraphQLObjectType transformedNode,
                                                                 final OutputObjectCreationCallbackParams creationCallbackParams )
    {
        String newDescription = Objects.requireNonNullElse( creationCallbackParams.getDescription(), transformedNode.getDescription() );

        List<? extends GraphQLType> newInterfaces = Objects.requireNonNullElse( creationCallbackParams.getInterfaces(),
                                                                                Objects.requireNonNullElse( node.getInterfaces(),
                                                                                                            List.of() ) );

		List<GraphQLFieldDefinition> newFields =
			GraphQLFieldsMerger.merge( node.getName(), transformedNode.getFieldDefinitions(), creationCallbackParams, guillotineConfigService );

        transformedNode = transformedNode.transform( builder -> {
            builder.description( newDescription );
            builder.replaceFields( newFields );

            newInterfaces.forEach( newInterface -> {
                if ( newInterface instanceof GraphQLInterfaceType )
                {
                    builder.withInterface( (GraphQLInterfaceType) newInterface );
                }
                if ( newInterface instanceof GraphQLTypeReference )
                {
                    builder.withInterface( (GraphQLTypeReference) newInterface );
                }
            } );
        } );
        return transformedNode;
    }
}
