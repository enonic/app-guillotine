package com.enonic.app.guillotine.graphql.factory;

import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getNameForGraphQLTypeReference;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AclTypesVerifier
{
    private final GuillotineContext context;

    public AclTypesVerifier( final GuillotineContext context )
    {
        this.context = context;
    }

    public void verify()
    {
        verifyPrincipalKey();
    }

    private void verifyPrincipalKey()
    {
        GraphQLObjectType type = context.getOutputType( "PrincipalKey" );

        assertEquals( "Principal key.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 4, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "value" ).getType() );
        assertEquals( "PrincipalType", getNameForGraphQLTypeReference( type.getFieldDefinition( "type" ).getType() ) );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "idProvider" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "principalId" ).getType() );
    }
}
