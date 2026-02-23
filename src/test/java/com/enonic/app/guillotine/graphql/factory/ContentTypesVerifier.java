package com.enonic.app.guillotine.graphql.factory;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getNameForGraphQLTypeReference;
import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getOriginalTypeFromGraphQLList;
import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getOriginalTypeFromGraphQLNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentTypesVerifier
{
    private final GuillotineContext context;

    public ContentTypesVerifier( final GuillotineContext context )
    {
        this.context = context;
    }

    public void verify()
    {
        verifyContent();
        verifyUntypedContent();

        verifyPageInfo();
        verifyContentConnection();
        verifyContentEdge();
        verifyQueryDSLContentConnection();

        verifyDynamicallyCreatedContentType();
    }

    private void verifyDynamicallyCreatedContentType()
    {
        GraphQLObjectType type = context.getOutputType( "com_enonic_app_testapp_MyType" );
        assertEquals( 31, type.getFieldDefinitions().size() );

        GraphQLFieldDefinition dataField = type.getFieldDefinition( "data" );

        GraphQLObjectType dataType = (GraphQLObjectType) dataField.getType();

        assertEquals( "com_enonic_app_testapp_MyType_Data", dataType.getName() );
        assertEquals( "my_type - com.enonic.app.testapp:my_type data", dataType.getDescription() );

        assertEquals( 4, dataType.getFieldDefinitions().size() );

        GraphQLObjectType mySetDataType = (GraphQLObjectType) dataType.getFieldDefinition( "mySet" ).getType();
        assertEquals( "com_enonic_app_testapp_MyType_MySet", mySetDataType.getName() );

        assertEquals( 1, mySetDataType.getFieldDefinitions().size() );
        assertEquals( Scalars.GraphQLString, mySetDataType.getFieldDefinition( "myInput" ).getType() );

        assertEquals( ExtendedScalars.Date, dataType.getFieldDefinition( "dateOfBirth" ).getType() );

        GraphQLObjectType blocksType = (GraphQLObjectType) dataType.getFieldDefinition( "blocks" ).getType();
        assertEquals( "com_enonic_app_testapp_MyType_Blocks", blocksType.getName() );

        assertEquals( 3, blocksType.getFieldDefinitions().size() );

        GraphQLEnumType optionEnum = (GraphQLEnumType) blocksType.getFieldDefinition( "_selected" ).getType();
        assertEquals( "com_enonic_app_testapp_MyType_Blocks_OptionEnum", optionEnum.getName() );
        assertEquals( "text", optionEnum.getValue( "text" ).getValue() );
        assertEquals( "icon", optionEnum.getValue( "icon" ).getValue() );

        GraphQLObjectType iconFieldType = (GraphQLObjectType) blocksType.getFieldDefinition( "icon" ).getType();
        assertEquals( "com_enonic_app_testapp_MyType_Icon", iconFieldType.getName() );
        assertEquals( 1, iconFieldType.getFieldDefinitions().size() );
        assertEquals( "Attachment", getNameForGraphQLTypeReference( iconFieldType.getFieldDefinition( "icon" ).getType() ) );

        GraphQLObjectType textFieldType = (GraphQLObjectType) blocksType.getFieldDefinition( "text" ).getType();
        assertEquals( "com_enonic_app_testapp_MyType_Text", textFieldType.getName() );
        assertEquals( 1, textFieldType.getFieldDefinitions().size() );
        assertEquals( "RichText", getNameForGraphQLTypeReference( textFieldType.getFieldDefinition( "text" ).getType() ) );

        GraphQLType castType = dataType.getFieldDefinition( "cast" ).getType();
        assertTrue( castType instanceof GraphQLList );
        GraphQLObjectType castObjectType = (GraphQLObjectType) ( (GraphQLList) castType ).getWrappedType();
        assertEquals( "com_enonic_app_testapp_MyType_Cast", castObjectType.getName() );

        assertEquals( 3, castObjectType.getFieldDefinitions().size() );

        GraphQLFieldDefinition actorField = castObjectType.getFieldDefinition( "actor" );
        assertEquals( "Content", getNameForGraphQLTypeReference( actorField.getType() ) );

        assertEquals( Scalars.GraphQLString, castObjectType.getFieldDefinition( "abstract" ).getType() );

        GraphQLType photosType = castObjectType.getFieldDefinition( "photos" ).getType();
        assertTrue( photosType instanceof GraphQLList );
        assertEquals( "Content", getNameForGraphQLTypeReference( ( (GraphQLList) photosType ).getWrappedType() ) );
    }

    private void verifyQueryDSLContentConnection()
    {
        GraphQLObjectType type = context.getOutputType( "QueryDSLContentConnection" );
        assertEquals( "QueryDSLContentConnection.", type.getDescription() );

        assertEquals( Scalars.GraphQLInt, getOriginalTypeFromGraphQLNonNull( type, "totalCount" ) );
        assertEquals( ExtendedScalars.Json, type.getField( "aggregationsAsJson" ).getType() );
        assertEquals( ExtendedScalars.Json, type.getField( "highlightAsJson" ).getType() );
        assertEquals( "ContentEdge", ( (GraphQLObjectType) getOriginalTypeFromGraphQLList( type, "edges" ) ).getName() );
        assertEquals( "PageInfo", getNameForGraphQLTypeReference( type.getField( "pageInfo" ).getType() ) );
    }

    private void verifyContentEdge()
    {
        GraphQLObjectType type = context.getOutputType( "ContentEdge" );
        assertEquals( "ContentEdge.", type.getDescription() );

        assertEquals( "Content", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLNonNull( type, "node" ) ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "cursor" ) );
    }

    private void verifyContentConnection()
    {
        GraphQLObjectType type = context.getOutputType( "ContentConnection" );
        assertEquals( "ContentConnection.", type.getDescription() );

        assertEquals( Scalars.GraphQLInt, getOriginalTypeFromGraphQLNonNull( type, "totalCount" ) );
        assertEquals( "ContentEdge", ( (GraphQLObjectType) getOriginalTypeFromGraphQLList( type, "edges" ) ).getName() );
        assertEquals( "PageInfo", getNameForGraphQLTypeReference( type.getField( "pageInfo" ).getType() ) );
    }

    private void verifyPageInfo()
    {
        GraphQLObjectType type = context.getOutputType( "PageInfo" );
        assertEquals( "PageInfo", type.getDescription() );

        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "startCursor" ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "endCursor" ) );
        assertEquals( Scalars.GraphQLBoolean, getOriginalTypeFromGraphQLNonNull( type, "hasNext" ) );
    }

    private void verifyContent()
    {
        GraphQLInterfaceType type = context.getInterfaceType( "Content" );

        assertEquals( "Content.", type.getDescription() );

        verifyCommonContentFields( type );
    }

    private void verifyUntypedContent()
    {
        GraphQLObjectType type = context.getOutputType( "UntypedContent" );

        assertEquals( "UntypedContent", type.getDescription() );

        assertEquals( Scalars.GraphQLID, getOriginalTypeFromGraphQLNonNull( type, "_id" ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "_name" ) );

        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "_path" ) );

        assertEquals( "Content", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "_references" ) ) );
        assertEquals( Scalars.GraphQLFloat, type.getField( "_score" ).getType() );

        assertEquals( "PrincipalKey", getNameForGraphQLTypeReference( type.getField( "creator" ).getType() ) );
        assertEquals( "PrincipalKey", getNameForGraphQLTypeReference( type.getField( "modifier" ).getType() ) );
        assertEquals( "PrincipalKey", getNameForGraphQLTypeReference( type.getField( "owner" ).getType() ) );
        assertEquals( ExtendedScalars.DateTime, type.getField( "createdTime" ).getType() );
        assertEquals( ExtendedScalars.DateTime, type.getField( "modifiedTime" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "type" ).getType() );
        assertEquals( "ContentType", getNameForGraphQLTypeReference( type.getField( "contentType" ).getType() ) );
        assertEquals( Scalars.GraphQLString, type.getField( "displayName" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "language" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "valid" ).getType() );
        assertEquals( ExtendedScalars.Json, type.getField( "dataAsJson" ).getType() );
        assertEquals( "Mixin", getNameForGraphQLTypeReference( type.getField( "x" ).getType() ) );
        assertEquals( ExtendedScalars.Json, type.getField( "xAsJson" ).getType() );

        GraphQLFieldDefinition pageAsJsonField = type.getField( "pageAsJson" );
        assertEquals( ExtendedScalars.Json, pageAsJsonField.getType() );
        assertEquals( Scalars.GraphQLBoolean, pageAsJsonField.getArgument( "resolveTemplate" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, pageAsJsonField.getArgument( "resolveFragment" ).getType() );

        assertEquals( "Content", getNameForGraphQLTypeReference( type.getField( "pageTemplate" ).getType() ) );
        assertEquals( "Component", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "components" ) ) );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "components" ).getArgument( "resolveTemplate" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "components" ).getArgument( "resolveFragment" ).getType() );

        assertEquals( "Attachment", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "attachments" ) ) );
        assertEquals( "PublishInfo", getNameForGraphQLTypeReference( type.getField( "publish" ).getType() ) );

        assertEquals( "portal_Site", getNameForGraphQLTypeReference( type.getField( "site" ).getType() ) );
        assertEquals( "Content", getNameForGraphQLTypeReference( type.getField( "parent" ).getType() ) );

        assertEquals( "Content", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "children" ) ) );
        assertEquals( Scalars.GraphQLInt, type.getField( "children" ).getArgument( "offset" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "children" ).getArgument( "first" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "children" ).getArgument( "sort" ).getType() );

        GraphQLFieldDefinition childrenConnectionField = type.getField( "childrenConnection" );
        assertEquals( "ContentConnection", getNameForGraphQLTypeReference( childrenConnectionField.getType() ) );
        assertEquals( Scalars.GraphQLString, childrenConnectionField.getArgument( "after" ).getType() );
        assertEquals( Scalars.GraphQLInt, childrenConnectionField.getArgument( "first" ).getType() );
        assertEquals( Scalars.GraphQLString, childrenConnectionField.getArgument( "sort" ).getType() );

        assertEquals( "Permissions", getNameForGraphQLTypeReference( type.getField( "permissions" ).getType() ) );
    }

    private static void verifyCommonContentFields( final GraphQLInterfaceType type )
    {
        assertEquals( Scalars.GraphQLID, getOriginalTypeFromGraphQLNonNull( type, "_id" ) );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "_name" ) );

        GraphQLFieldDefinition pathField = type.getFieldDefinition( "_path" );
        assertEquals( Scalars.GraphQLString, getOriginalTypeFromGraphQLNonNull( type, "_path" ) );

        assertEquals( "Content", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "_references" ) ) );
        assertEquals( Scalars.GraphQLFloat, type.getField( "_score" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "_project" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "_branch" ).getType() );

        assertEquals( "PrincipalKey", getNameForGraphQLTypeReference( type.getField( "creator" ).getType() ) );
        assertEquals( "PrincipalKey", getNameForGraphQLTypeReference( type.getField( "modifier" ).getType() ) );
        assertEquals( "PrincipalKey", getNameForGraphQLTypeReference( type.getField( "owner" ).getType() ) );
        assertEquals( ExtendedScalars.DateTime, type.getField( "createdTime" ).getType() );
        assertEquals( ExtendedScalars.DateTime, type.getField( "modifiedTime" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "type" ).getType() );
        assertEquals( "ContentType", getNameForGraphQLTypeReference( type.getField( "contentType" ).getType() ) );
        assertEquals( Scalars.GraphQLString, type.getField( "displayName" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "language" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "valid" ).getType() );
        assertEquals( ExtendedScalars.Json, type.getField( "dataAsJson" ).getType() );
        assertEquals( "Mixin", getNameForGraphQLTypeReference( type.getField( "x" ).getType() ) );
        assertEquals( ExtendedScalars.Json, type.getField( "xAsJson" ).getType() );

        GraphQLFieldDefinition pageAsJsonField = type.getField( "pageAsJson" );
        assertEquals( ExtendedScalars.Json, pageAsJsonField.getType() );
        assertEquals( Scalars.GraphQLBoolean, pageAsJsonField.getArgument( "resolveTemplate" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, pageAsJsonField.getArgument( "resolveFragment" ).getType() );

        assertEquals( "Content", getNameForGraphQLTypeReference( type.getField( "pageTemplate" ).getType() ) );
        assertEquals( "Component", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "components" ) ) );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "components" ).getArgument( "resolveTemplate" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getField( "components" ).getArgument( "resolveFragment" ).getType() );

        assertEquals( "Attachment", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "attachments" ) ) );
        assertEquals( "PublishInfo", getNameForGraphQLTypeReference( type.getField( "publish" ).getType() ) );

        assertEquals( "portal_Site", getNameForGraphQLTypeReference( type.getField( "site" ).getType() ) );
        assertEquals( "Content", getNameForGraphQLTypeReference( type.getField( "parent" ).getType() ) );

        assertEquals( "Content", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "children" ) ) );
        assertEquals( Scalars.GraphQLInt, type.getField( "children" ).getArgument( "offset" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getField( "children" ).getArgument( "first" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getField( "children" ).getArgument( "sort" ).getType() );

        GraphQLFieldDefinition childrenConnectionField = type.getField( "childrenConnection" );
        assertEquals( "ContentConnection", getNameForGraphQLTypeReference( childrenConnectionField.getType() ) );
        assertEquals( Scalars.GraphQLString, childrenConnectionField.getArgument( "after" ).getType() );
        assertEquals( Scalars.GraphQLInt, childrenConnectionField.getArgument( "first" ).getType() );
        assertEquals( Scalars.GraphQLString, childrenConnectionField.getArgument( "sort" ).getType() );

        assertEquals( "Permissions", getNameForGraphQLTypeReference( type.getField( "permissions" ).getType() ) );
    }
}
