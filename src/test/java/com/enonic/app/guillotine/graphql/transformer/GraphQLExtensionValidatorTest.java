package com.enonic.app.guillotine.graphql.transformer;

import java.util.Set;

import org.junit.jupiter.api.Test;

import graphql.Scalars;

import com.enonic.xp.script.ScriptValue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphQLExtensionValidatorTest
{
    @Test
    public void testTypeWithoutDescriptionIsValid()
    {
        ScriptValue types = typesWithDescription( null );

        assertDoesNotThrow( () -> GraphQLExtensionValidator.validateTypeDefs( types ) );
    }

    @Test
    public void testTypeWithStringDescriptionIsValid()
    {
        ScriptValue description = mock( ScriptValue.class );
        when( description.isValue() ).thenReturn( true );
        when( description.getValue() ).thenReturn( "A type" );

        ScriptValue types = typesWithDescription( description );

        assertDoesNotThrow( () -> GraphQLExtensionValidator.validateTypeDefs( types ) );
    }

    @Test
    public void testTypeWithNonStringDescriptionIsRejected()
    {
        ScriptValue description = mock( ScriptValue.class );
        when( description.isValue() ).thenReturn( true );
        when( description.getValue() ).thenReturn( 42 );

        ScriptValue types = typesWithDescription( description );

        assertThrows( IllegalArgumentException.class, () -> GraphQLExtensionValidator.validateTypeDefs( types ) );
    }

    @Test
    public void testTypeWithObjectDescriptionIsRejected()
    {
        ScriptValue description = mock( ScriptValue.class );
        when( description.isValue() ).thenReturn( false );

        ScriptValue types = typesWithDescription( description );

        assertThrows( IllegalArgumentException.class, () -> GraphQLExtensionValidator.validateTypeDefs( types ) );
    }

    private static ScriptValue typesWithDescription( final ScriptValue description )
    {
        ScriptValue typeMember = mock( ScriptValue.class );
        when( typeMember.getValue() ).thenReturn( Scalars.GraphQLString );

        ScriptValue nameField = mock( ScriptValue.class );
        when( nameField.isObject() ).thenReturn( true );
        when( nameField.getMember( "type" ) ).thenReturn( typeMember );
        when( nameField.getMember( "args" ) ).thenReturn( null );

        ScriptValue fields = mock( ScriptValue.class );
        when( fields.isObject() ).thenReturn( true );
        when( fields.getKeys() ).thenReturn( Set.of( "name" ) );
        when( fields.getMember( "name" ) ).thenReturn( nameField );

        ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        when( typeDef.getMember( "description" ) ).thenReturn( description );
        when( typeDef.getMember( "fields" ) ).thenReturn( fields );

        ScriptValue types = mock( ScriptValue.class );
        when( types.isObject() ).thenReturn( true );
        when( types.getKeys() ).thenReturn( Set.of( "MyType" ) );
        when( types.getMember( "MyType" ) ).thenReturn( typeDef );
        return types;
    }
}
