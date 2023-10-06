package com.enonic.app.guillotine.graphql.factory;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getNameForGraphQLTypeReference;
import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getOriginalTypeFromGraphQLList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeadlessCmsTypeVerifier
{
    private final GuillotineContext context;

    public HeadlessCmsTypeVerifier( final GuillotineContext context )
    {
        this.context = context;
    }

    public void verify()
    {
        GraphQLObjectType guillotineApiType = context.getOutputType( "HeadlessCms" );
        assertEquals( "Headless CMS", guillotineApiType.getDescription() );

        assertEquals( 9, guillotineApiType.getFieldDefinitions().size() );

        // verify get field
        GraphQLFieldDefinition getField = guillotineApiType.getFieldDefinition( "get" );
        assertEquals( "Content", getNameForGraphQLTypeReference( getField.getType() ) );
        assertEquals( 1, getField.getArguments().size() );
        assertEquals( Scalars.GraphQLID, getField.getArgument( "key" ).getType() );

        // verify getChildren field
        GraphQLFieldDefinition getChildrenField = guillotineApiType.getFieldDefinition( "getChildren" );
        assertEquals( "Content", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( guillotineApiType, "getChildren" ) ) );
        assertEquals( 4, getChildrenField.getArguments().size() );
        assertEquals( Scalars.GraphQLID, getChildrenField.getArgument( "key" ).getType() );
        assertEquals( Scalars.GraphQLInt, getChildrenField.getArgument( "offset" ).getType() );
        assertEquals( Scalars.GraphQLInt, getChildrenField.getArgument( "first" ).getType() );
        assertEquals( Scalars.GraphQLString, getChildrenField.getArgument( "sort" ).getType() );

        // verify getChildrenConnection field
        GraphQLFieldDefinition getChildrenConnectionField = guillotineApiType.getFieldDefinition( "getChildrenConnection" );
        assertEquals( "ContentConnection", getNameForGraphQLTypeReference( getChildrenConnectionField.getType() ) );
        assertEquals( 4, getChildrenConnectionField.getArguments().size() );
        assertEquals( Scalars.GraphQLID, getChildrenConnectionField.getArgument( "key" ).getType() );
        assertEquals( Scalars.GraphQLString, getChildrenConnectionField.getArgument( "after" ).getType() );
        assertEquals( Scalars.GraphQLInt, getChildrenConnectionField.getArgument( "first" ).getType() );
        assertEquals( Scalars.GraphQLString, getChildrenConnectionField.getArgument( "sort" ).getType() );

        // verify getPermission field
        GraphQLFieldDefinition getPermissionsField = guillotineApiType.getFieldDefinition( "getPermissions" );
        assertEquals( "Permissions", getNameForGraphQLTypeReference( getPermissionsField.getType() ) );
        assertEquals( 1, getPermissionsField.getArguments().size() );
        assertEquals( Scalars.GraphQLID, getPermissionsField.getArgument( "key" ).getType() );

        // verify getSite field
        GraphQLFieldDefinition getSiteField = guillotineApiType.getFieldDefinition( "getSite" );
        assertEquals( "portal_Site", getNameForGraphQLTypeReference( getSiteField.getType() ) );

        // verify queryDsl field
        GraphQLFieldDefinition queryDslField = guillotineApiType.getFieldDefinition( "queryDsl" );
        assertEquals( "Content", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( guillotineApiType, "queryDsl" ) ) );
        assertEquals( 4, queryDslField.getArguments().size() );
        assertEquals( "QueryDSLInput", getNameForGraphQLTypeReference( queryDslField.getArgument( "query" ).getType() ) );
        assertEquals( Scalars.GraphQLInt, queryDslField.getArgument( "offset" ).getType() );
        assertEquals( Scalars.GraphQLInt, queryDslField.getArgument( "first" ).getType() );
        assertEquals( "SortDslInput", getNameForGraphQLTypeReference(
            ( (GraphQLList) queryDslField.getArgument( "sort" ).getType() ).getOriginalWrappedType() ) );

        // verify queryDslConnection field
        GraphQLFieldDefinition queryDslConnectionField = guillotineApiType.getFieldDefinition( "queryDslConnection" );
        assertEquals( "QueryDSLContentConnection",
                      getNameForGraphQLTypeReference( guillotineApiType.getFieldDefinition( "queryDslConnection" ).getType() ) );
        assertEquals( 6, queryDslConnectionField.getArguments().size() );
        assertEquals( "QueryDSLInput", getNameForGraphQLTypeReference(
            ( (GraphQLNonNull) queryDslConnectionField.getArgument( "query" ).getType() ).getOriginalWrappedType() ) );
        assertEquals( Scalars.GraphQLString, queryDslConnectionField.getArgument( "after" ).getType() );
        assertEquals( Scalars.GraphQLInt, queryDslConnectionField.getArgument( "first" ).getType() );
        assertEquals( "AggregationInput", getNameForGraphQLTypeReference(
            ( (GraphQLList) queryDslConnectionField.getArgument( "aggregations" ).getType() ).getOriginalWrappedType() ) );
        assertEquals( "HighlightInputType",
                      getNameForGraphQLTypeReference( queryDslConnectionField.getArgument( "highlight" ).getType() ) );
        assertEquals( "SortDslInput", getNameForGraphQLTypeReference(
            ( (GraphQLList) queryDslField.getArgument( "sort" ).getType() ).getOriginalWrappedType() ) );

        // verify getType field
        GraphQLFieldDefinition getTypeField = guillotineApiType.getFieldDefinition( "getType" );
        assertEquals( "ContentType", getNameForGraphQLTypeReference( getTypeField.getType() ) );
        assertEquals( 1, getTypeField.getArguments().size() );
        assertEquals( Scalars.GraphQLString, ( (GraphQLNonNull) getTypeField.getArgument( "name" ).getType() ).getOriginalWrappedType() );

        // verify getTypes field
        GraphQLFieldDefinition getTypesField = guillotineApiType.getFieldDefinition( "getTypes" );
        assertEquals( "ContentType", getNameForGraphQLTypeReference( ( (GraphQLList) getTypesField.getType() ).getOriginalWrappedType() ) );
    }
}
