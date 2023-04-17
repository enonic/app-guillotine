package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;

import com.enonic.xp.security.PrincipalKey;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;

public class AclTypesFactory
{
    private final GuillotineContext guillotineContext;

    public AclTypesFactory( final GuillotineContext guillotineContext )
    {
        this.guillotineContext = guillotineContext;
    }

    public void create()
    {
        createPrincipalKeyType();
        createAccessControlEntryType();
        createPermissionsType();
    }

    private void createPrincipalKeyType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "value", Scalars.GraphQLString ) );
        fields.add( outputField( "type", guillotineContext.getEnumType( "PrincipalType" ) ) );
        fields.add( outputField( "idProvider", Scalars.GraphQLString ) );
        fields.add( outputField( "principalId", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( "PrincipalKey", "Principal key.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "value", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.toString();
        } );
        guillotineContext.registerDataFetcher( outputObject.getName(), "type", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.getType().name().toLowerCase();
        } );
        guillotineContext.registerDataFetcher( outputObject.getName(), "idProvider", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.getIdProviderKey() != null ? principalKey.getIdProviderKey().toString() : null;
        } );
        guillotineContext.registerDataFetcher( outputObject.getName(), "principalId", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.getId();
        } );
    }

    private void createAccessControlEntryType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "principal", guillotineContext.getOutputType( "PrincipalKey" ) ) );
        fields.add( outputField( "allow", new GraphQLList( guillotineContext.getEnumType( "Permission" ) ) ) );
        fields.add( outputField( "deny", new GraphQLList( guillotineContext.getEnumType( "Permission" ) ) ) );

        GraphQLObjectType outputObject = newObject( "AccessControlEntry", "Access control entry.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );
    }

    private void createPermissionsType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "inheritsPermissions", Scalars.GraphQLBoolean ) );
        fields.add(outputField( "permissions", new GraphQLList( guillotineContext.getOutputType( "AccessControlEntry" ) ) ) );

        GraphQLObjectType outputObject = newObject( "Permissions", "Permissions.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );
    }
}
