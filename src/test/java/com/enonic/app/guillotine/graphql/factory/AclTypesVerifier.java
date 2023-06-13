package com.enonic.app.guillotine.graphql.factory;

import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getNameForGraphQLTypeReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        verifyAccessControlEntry();
        verifyPermissions();
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

    private void verifyAccessControlEntry()
    {
        GraphQLObjectType type = context.getOutputType( "AccessControlEntry" );

        assertEquals( "Access control entry.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 3, fields.size() );
        assertEquals( "PrincipalKey", getNameForGraphQLTypeReference( type.getFieldDefinition( "principal" ).getType() ) );

        GraphQLOutputType typeOfAllowField = type.getFieldDefinition( "allow" ).getType();
        assertTrue( typeOfAllowField instanceof GraphQLList );
        assertEquals( "Permission", getNameForGraphQLTypeReference( ( (GraphQLList) typeOfAllowField ).getOriginalWrappedType() ) );

        GraphQLOutputType typeOfDenyField = type.getFieldDefinition( "allow" ).getType();
        assertTrue( typeOfDenyField instanceof GraphQLList );
        assertEquals( "Permission", getNameForGraphQLTypeReference( ( (GraphQLList) typeOfDenyField ).getOriginalWrappedType() ) );
    }

    private void verifyPermissions()
    {
        GraphQLObjectType type = context.getOutputType( "Permissions" );

        assertEquals( "Permissions.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 2, fields.size() );
        assertEquals( Scalars.GraphQLBoolean, type.getFieldDefinition( "inheritsPermissions" ).getType() );

        GraphQLOutputType typeOfPermissionsField = type.getFieldDefinition( "permissions" ).getType();
        assertTrue( typeOfPermissionsField instanceof GraphQLList );
        assertEquals( "AccessControlEntry",
                      getNameForGraphQLTypeReference( ( (GraphQLList) typeOfPermissionsField ).getOriginalWrappedType() ) );
    }
}
