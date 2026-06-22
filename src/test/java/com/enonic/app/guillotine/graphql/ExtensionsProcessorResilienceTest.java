package com.enonic.app.guillotine.graphql;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;

import com.enonic.app.guillotine.graphql.transformer.SchemaExtensions;
import com.enonic.xp.script.ScriptValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtensionsProcessorResilienceTest
{
    private static final String BROKEN = "BrokenType";

    private static final String GOOD = "GoodType";

    @Test
    public void testProcessSkipsBrokenEnumType()
    {
        SchemaExtensions extensions = SchemaExtensions.create().addEnum( GOOD, goodEnumDef() ).addEnum( BROKEN, brokenObjectDef() ).build();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();
        new ExtensionsProcessor( typesRegister ).process( extensions );

        assertHasTypeOfKind( typesRegister, GOOD, GraphQLEnumType.class );
        assertMissingType( typesRegister, BROKEN );
    }

    @Test
    public void testProcessSkipsBrokenInputType()
    {
        SchemaExtensions extensions =
            SchemaExtensions.create().addInputType( GOOD, goodInputTypeDef() ).addInputType( BROKEN, brokenObjectDef() ).build();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();
        new ExtensionsProcessor( typesRegister ).process( extensions );

        assertHasTypeOfKind( typesRegister, GOOD, GraphQLInputObjectType.class );
        assertMissingType( typesRegister, BROKEN );
    }

    @Test
    public void testProcessSkipsBrokenOutputType()
    {
        SchemaExtensions extensions =
            SchemaExtensions.create().addType( GOOD, goodOutputTypeDef() ).addType( BROKEN, brokenObjectDef() ).build();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();
        new ExtensionsProcessor( typesRegister ).process( extensions );

        assertHasTypeOfKind( typesRegister, GOOD, GraphQLObjectType.class );
        assertMissingType( typesRegister, BROKEN );
    }

    @Test
    public void testProcessSkipsBrokenInterfaceType()
    {
        SchemaExtensions extensions =
            SchemaExtensions.create().addInterface( GOOD, goodInterfaceTypeDef() ).addInterface( BROKEN, brokenObjectDef() ).build();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();
        new ExtensionsProcessor( typesRegister ).process( extensions );

        assertHasTypeOfKind( typesRegister, GOOD, GraphQLInterfaceType.class );
        assertMissingType( typesRegister, BROKEN );
    }

    @Test
    public void testProcessSkipsBrokenUnionType()
    {
        SchemaExtensions extensions =
            SchemaExtensions.create().addUnion( GOOD, goodUnionTypeDef() ).addUnion( BROKEN, brokenObjectDef() ).build();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();
        new ExtensionsProcessor( typesRegister ).process( extensions );

        assertHasTypeOfKind( typesRegister, GOOD, GraphQLUnionType.class );
        assertMissingType( typesRegister, BROKEN );
    }

    @Test
    public void testProcessSkipsBrokenFieldResolverPerField()
    {
        ScriptValue goodResolver = mock( ScriptValue.class );

        SchemaExtensions extensions = SchemaExtensions.create().addResolver( "MyType", "goodField", goodResolver ).addResolver( "MyType",
                                                                                                                                "brokenField",
                                                                                                                                null ).build();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();
        new ExtensionsProcessor( typesRegister ).process( extensions );

        DataFetcher<?> goodFetcher = typesRegister.getResolvers().get( "MyType" ).get( "goodField" );
        assertNotNull( goodFetcher, "Good field resolver must be registered when sibling field fails" );
        assertNull( typesRegister.getResolvers().get( "MyType" ).get( "brokenField" ),
                    "Broken field resolver must not be registered" );
    }

    @Test
    public void testProcessSkipsThrowingCreationCallback()
    {
        ScriptValue goodCallback = mock( ScriptValue.class );

        ScriptValue brokenCallback = mock( ScriptValue.class );
        when( brokenCallback.call( any() ) ).thenThrow( new RuntimeException( "Simulated callback failure" ) );

        SchemaExtensions extensions =
            SchemaExtensions.create().addCreationCallback( "MyType", brokenCallback ).addCreationCallback( "MyType", goodCallback ).build();

        GraphQLTypesRegister typesRegister = new GraphQLTypesRegister();
        new ExtensionsProcessor( typesRegister ).process( extensions );

        List<OutputObjectCreationCallbackParams> callbacks = typesRegister.getCreationCallbacks().get( "MyType" );
        assertNotNull( callbacks, "Sibling creation callback must still be applied when one throws" );
        assertEquals( 1, callbacks.size(), "Only the good callback must be recorded" );
    }

    private static ScriptValue goodEnumDef()
    {
        ScriptValue desc = scalarString( "Good enum" );

        ScriptValue valueA = mock( ScriptValue.class );
        when( valueA.getValue() ).thenReturn( "VALUE_A" );

        ScriptValue values = mock( ScriptValue.class );
        when( values.getKeys() ).thenReturn( Set.of( "VALUE_A" ) );
        when( values.getMember( "VALUE_A" ) ).thenReturn( valueA );

        ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        when( typeDef.getMember( "description" ) ).thenReturn( desc );
        when( typeDef.getMember( "values" ) ).thenReturn( values );
        return typeDef;
    }

    private static ScriptValue goodInputTypeDef()
    {
        ScriptValue desc = scalarString( "Good input" );

        ScriptValue nameField = mock( ScriptValue.class );
        when( nameField.getValue() ).thenReturn( Scalars.GraphQLString );

        ScriptValue fields = mock( ScriptValue.class );
        when( fields.getKeys() ).thenReturn( Set.of( "name" ) );
        when( fields.getMember( "name" ) ).thenReturn( nameField );

        ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        when( typeDef.getMember( "description" ) ).thenReturn( desc );
        when( typeDef.getMember( "fields" ) ).thenReturn( fields );
        return typeDef;
    }

    private static ScriptValue goodOutputTypeDef()
    {
        ScriptValue desc = scalarString( "Good output" );
        ScriptValue fields = objectFieldsWithName();

        ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        when( typeDef.getMember( "description" ) ).thenReturn( desc );
        when( typeDef.getMember( "interfaces" ) ).thenReturn( null );
        when( typeDef.getMember( "fields" ) ).thenReturn( fields );
        return typeDef;
    }

    private static ScriptValue goodInterfaceTypeDef()
    {
        ScriptValue desc = scalarString( "Good interface" );
        ScriptValue fields = objectFieldsWithName();

        ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        when( typeDef.getMember( "description" ) ).thenReturn( desc );
        when( typeDef.getMember( "fields" ) ).thenReturn( fields );
        return typeDef;
    }

    private static ScriptValue goodUnionTypeDef()
    {
        ScriptValue desc = scalarString( "Good union" );

        GraphQLObjectType member1 = GraphQLObjectType.newObject().name( "Member1" ).field(
            f -> f.name( "id" ).type( Scalars.GraphQLID ) ).build();
        GraphQLObjectType member2 = GraphQLObjectType.newObject().name( "Member2" ).field(
            f -> f.name( "id" ).type( Scalars.GraphQLID ) ).build();

        ScriptValue typesDefs = mock( ScriptValue.class );
        when( typesDefs.getList() ).thenReturn( List.of( member1, member2 ) );

        ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        when( typeDef.getMember( "description" ) ).thenReturn( desc );
        when( typeDef.getMember( "types" ) ).thenReturn( typesDefs );
        return typeDef;
    }

    private static ScriptValue objectFieldsWithName()
    {
        ScriptValue typeMember = mock( ScriptValue.class );
        when( typeMember.getValue() ).thenReturn( Scalars.GraphQLString );

        ScriptValue nameField = mock( ScriptValue.class );
        when( nameField.getMember( "type" ) ).thenReturn( typeMember );
        when( nameField.getMember( "args" ) ).thenReturn( null );

        ScriptValue fields = mock( ScriptValue.class );
        when( fields.getKeys() ).thenReturn( Set.of( "name" ) );
        when( fields.getMember( "name" ) ).thenReturn( nameField );
        return fields;
    }

    private static ScriptValue brokenObjectDef()
    {
        ScriptValue typeDef = mock( ScriptValue.class );
        when( typeDef.isObject() ).thenReturn( true );
        doThrow( new RuntimeException( "Simulated failure" ) ).when( typeDef ).getMember( "description" );
        return typeDef;
    }

    private static ScriptValue scalarString( String value )
    {
        ScriptValue scriptValue = mock( ScriptValue.class );
        when( scriptValue.getValue( String.class ) ).thenReturn( value );
        return scriptValue;
    }

    private static void assertHasTypeOfKind( GraphQLTypesRegister typesRegister, String typeName, Class<? extends GraphQLType> kind )
    {
        boolean found = typesRegister.getAdditionalTypes().stream().anyMatch(
            t -> kind.isInstance( t ) && typeName.equals( ( (GraphQLNamedType) t ).getName() ) );
        assertTrue( found, "Expected " + kind.getSimpleName() + " '" + typeName + "' to be registered" );
    }

    private static void assertMissingType( GraphQLTypesRegister typesRegister, String typeName )
    {
        boolean found = typesRegister.getAdditionalTypes().stream().anyMatch(
            t -> t instanceof GraphQLNamedType && typeName.equals( ( (GraphQLNamedType) t ).getName() ) );
        assertFalse( found, "Type '" + typeName + "' must not be registered" );
    }
}
