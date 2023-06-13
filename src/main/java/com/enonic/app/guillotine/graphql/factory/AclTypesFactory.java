package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.xp.security.PrincipalKey;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class AclTypesFactory
{
    private final GuillotineContext context;

    public AclTypesFactory( final GuillotineContext context )
    {
        this.context = context;
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
        fields.add( outputField( "type", GraphQLTypeReference.typeRef( "PrincipalType" ) ) );
        fields.add( outputField( "idProvider", Scalars.GraphQLString ) );
        fields.add( outputField( "principalId", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "PrincipalKey" ), "Principal key.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "value", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.toString();
        } );
        context.registerDataFetcher( outputObject.getName(), "type", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.getType().name().toLowerCase();
        } );
        context.registerDataFetcher( outputObject.getName(), "idProvider", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.getIdProviderKey() != null ? principalKey.getIdProviderKey().toString() : null;
        } );
        context.registerDataFetcher( outputObject.getName(), "principalId", env -> {
            PrincipalKey principalKey = PrincipalKey.from( env.getSource() );
            return principalKey.getId();
        } );
    }

    private void createAccessControlEntryType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "principal", GraphQLTypeReference.typeRef( "PrincipalKey" ) ) );
        fields.add( outputField( "allow", new GraphQLList( GraphQLTypeReference.typeRef( "Permission" ) ) ) );
        fields.add( outputField( "deny", new GraphQLList( GraphQLTypeReference.typeRef( "Permission" ) ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "AccessControlEntry" ), "Access control entry.", fields );
        context.registerType( outputObject.getName(), outputObject );
    }

    private void createPermissionsType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "inheritsPermissions", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "permissions", new GraphQLList( GraphQLTypeReference.typeRef( "AccessControlEntry" ) ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "Permissions" ), "Permissions.", fields );
        context.registerType( outputObject.getName(), outputObject );
    }
}
