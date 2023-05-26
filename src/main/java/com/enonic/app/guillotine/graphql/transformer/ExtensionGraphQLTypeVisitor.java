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

import com.enonic.app.guillotine.graphql.BaseCreationCallbackParams;
import com.enonic.app.guillotine.graphql.OutputObjectCreationCallbackParams;
import com.enonic.xp.script.ScriptValue;

public class ExtensionGraphQLTypeVisitor
    extends GraphQLTypeVisitorStub
{

    private final Map<String, ScriptValue> creationCallbacksDefs;

    public ExtensionGraphQLTypeVisitor( final SchemaExtensions schemaExtensions )
    {
        this.creationCallbacksDefs = schemaExtensions.getCreationCallbacks();
    }

    @Override
    public TraversalControl visitGraphQLInterfaceType( final GraphQLInterfaceType node,
                                                       final TraverserContext<GraphQLSchemaElement> context )
    {
        if ( creationCallbacksDefs != null && creationCallbacksDefs.get( node.getName() ) != null )
        {
            ScriptValue creationCallbackFn = creationCallbacksDefs.get( node.getName() );

            BaseCreationCallbackParams creationCallbackParams = new BaseCreationCallbackParams();
            creationCallbackFn.call( creationCallbackParams );

            String newDescription = Objects.requireNonNullElse( creationCallbackParams.getDescription(), node.getDescription() );
            List<GraphQLFieldDefinition> newFields = GraphQLFieldsMerger.merge( node.getFieldDefinitions(), creationCallbackParams );

            GraphQLInterfaceType transformedNode = node.transform( builder -> builder.description( newDescription ).fields( newFields ) );

            return changeNode( context, transformedNode );

        }
        return super.visitGraphQLInterfaceType( node, context );
    }

    @Override
    public TraversalControl visitGraphQLObjectType( final GraphQLObjectType node, final TraverserContext<GraphQLSchemaElement> context )
    {
        if ( creationCallbacksDefs != null )
        {
            GraphQLObjectType transformedNode = node;

            boolean transformedCount = false;

            if ( creationCallbacksDefs.get( transformedNode.getName() ) != null )
            {
                ScriptValue creationCallback = creationCallbacksDefs.get( transformedNode.getName() );
                transformedNode = executeCreationCallback( transformedNode, creationCallback );

                transformedCount = true;
            }

            Set<String> interfaceNames =
                transformedNode.getInterfaces().stream().map( GraphQLNamedOutputType::getName ).collect( Collectors.toSet() );

            for ( String interfaceName : interfaceNames )
            {
                ScriptValue creationCallback = creationCallbacksDefs.get( interfaceName );
                if ( creationCallback != null )
                {
                    BaseCreationCallbackParams creationCallbackParams = new BaseCreationCallbackParams();
                    creationCallback.call( creationCallbackParams );

                    List<GraphQLFieldDefinition> newFields =
                        GraphQLFieldsMerger.merge( transformedNode.getFieldDefinitions(), creationCallbackParams );

                    transformedNode = transformedNode.transform( builder -> builder.replaceFields( newFields ) );

                    transformedCount = true;
                }
            }

            if ( transformedCount )
            {
                return changeNode( context, transformedNode );
            }
        }
        return super.visitGraphQLObjectType( node, context );
    }

    private GraphQLObjectType executeCreationCallback( final GraphQLObjectType node, ScriptValue creationCallback )
    {
        if ( !creationCallback.isFunction() )
        {
            throw new IllegalArgumentException( String.format( "Creation callback must be a function for type '%s'", node.getName() ) );
        }

        OutputObjectCreationCallbackParams creationCallbackParams = new OutputObjectCreationCallbackParams();
        creationCallback.call( creationCallbackParams );

        return node.transform( builder -> transformGraphQLObjectType( builder, node, creationCallbackParams ) );
    }

    private void transformGraphQLObjectType( final GraphQLObjectType.Builder builder, final GraphQLObjectType node,
                                             final OutputObjectCreationCallbackParams creationCallbackParams )
    {
        String newDescription = Objects.requireNonNullElse( creationCallbackParams.getDescription(), node.getDescription() );
        List<? extends GraphQLType> newInterfaces = Objects.requireNonNullElse( creationCallbackParams.getInterfaces(),
                                                                                Objects.requireNonNullElse( node.getInterfaces(),
                                                                                                            List.of() ) );
        List<GraphQLFieldDefinition> newFields = GraphQLFieldsMerger.merge( node.getFieldDefinitions(), creationCallbackParams );

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
    }

}
