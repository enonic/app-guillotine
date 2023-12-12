package com.enonic.app.guillotine.graphql.factory;

import java.util.List;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import com.enonic.app.guillotine.graphql.GuillotineContext;

import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getNameForGraphQLTypeReference;
import static com.enonic.app.guillotine.graphql.factory.GraphQLTestHelper.getOriginalTypeFromGraphQLList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericTypesVerifier
{
    private final GuillotineContext context;

    public GenericTypesVerifier( final GuillotineContext context )
    {
        this.context = context;
    }

    public void verify()
    {
        verifyGeoPoint();
        verifyMediaFocalPoint();
        verifyMediaUploader();
        verifySiteConfigurator();
        verifyPublishInfo();
        verifyAttachment();
        verifyIcon();
        verifyContentType();
        verifyImageStyle();
        verifyImage();
        verifyMedia();
        verifyLink();
        verifyRichText();
    }

    private void verifyRichText()
    {
        GraphQLObjectType type = context.getOutputType( "RichText" );

        assertEquals( "RichText type.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 6, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "raw" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "processedHtml" ).getType() );
        assertEquals( ExtendedScalars.Json, type.getFieldDefinition( "macrosAsJson" ).getType() );
        assertEquals( "Macro", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "macros" ) ) );
        assertEquals( "Image", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "images" ) ) );
        assertEquals( "Link", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "links" ) ) );
    }

    private void verifyLink()
    {
        GraphQLObjectType type = context.getOutputType( "Link" );

        assertEquals( "Link type.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 4, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "ref" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "uri" ).getType() );
        assertEquals( "Content", getNameForGraphQLTypeReference( type.getFieldDefinition( "content" ).getType() ) );
        assertEquals( "Media", getNameForGraphQLTypeReference( type.getFieldDefinition( "media" ).getType() ) );
    }

    private void verifyMedia()
    {
        GraphQLObjectType type = context.getOutputType( "Media" );

        assertEquals( "Media type.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 2, fields.size() );
        assertEquals( "Content", getNameForGraphQLTypeReference( type.getFieldDefinition( "content" ).getType() ) );
        assertEquals( "MediaIntentType", getNameForGraphQLTypeReference( type.getFieldDefinition( "intent" ).getType() ) );
    }

    private void verifyImage()
    {
        GraphQLObjectType type = context.getOutputType( "Image" );

        assertEquals( "Image type.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 3, fields.size() );
        assertEquals( "Content", getNameForGraphQLTypeReference( type.getFieldDefinition( "image" ).getType() ) );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "ref" ).getType() );
        assertEquals( "ImageStyle", getNameForGraphQLTypeReference( type.getFieldDefinition( "style" ).getType() ) );
    }

    private void verifyImageStyle()
    {
        GraphQLObjectType type = context.getOutputType( "ImageStyle" );

        assertEquals( "ImageStyle type.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 3, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "aspectRatio" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "filter" ).getType() );
    }

    private void verifyContentType()
    {
        GraphQLObjectType type = context.getOutputType( "ContentType" );

        assertEquals( "Content type.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 11, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "displayName" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "description" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "superType" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getFieldDefinition( "abstract" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getFieldDefinition( "final" ).getType() );
        assertEquals( Scalars.GraphQLBoolean, type.getFieldDefinition( "allowChildContent" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "contentDisplayNameScript" ).getType() );
        assertEquals( "Icon", getNameForGraphQLTypeReference( type.getFieldDefinition( "icon" ).getType() ) );
        assertEquals( "FormItem", getNameForGraphQLTypeReference( getOriginalTypeFromGraphQLList( type, "form" ) ) );
        assertEquals( ExtendedScalars.Json, type.getFieldDefinition( "formAsJson" ).getType() );
    }

    private void verifyIcon()
    {
        GraphQLObjectType type = context.getOutputType( "Icon" );

        assertEquals( "Icon.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 2, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "mimeType" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "modifiedTime" ).getType() );
    }

    private void verifyAttachment()
    {
        GraphQLObjectType type = context.getOutputType( "Attachment" );

        assertEquals( "Attachment.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 5, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "name" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "label" ).getType() );
        assertEquals( Scalars.GraphQLInt, type.getFieldDefinition( "size" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "mimeType" ).getType() );

        GraphQLFieldDefinition attachmentUrlField = type.getFieldDefinition( "attachmentUrl" );
        assertEquals( Scalars.GraphQLString, attachmentUrlField.getType() );

        assertEquals( 3, attachmentUrlField.getArguments().size() );
        assertEquals( Scalars.GraphQLBoolean, attachmentUrlField.getArgument( "download" ).getType() );
        assertEquals( ExtendedScalars.Json, attachmentUrlField.getArgument( "params" ).getType() );
        assertEquals( "UrlType", getNameForGraphQLTypeReference( attachmentUrlField.getArgument( "type" ).getType() ) );
    }

    private void verifyPublishInfo()
    {
        GraphQLObjectType type = context.getOutputType( "PublishInfo" );

        assertEquals( "Publish information.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 3, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "from" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "to" ).getType() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "first" ).getType() );
    }

    private void verifySiteConfigurator()
    {
        GraphQLObjectType type = context.getOutputType( "SiteConfigurator" );

        assertEquals( "Site configurator.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 2, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "applicationKey" ).getType() );
        assertEquals( ExtendedScalars.Json, type.getFieldDefinition( "configAsJson" ).getType() );
    }

    private void verifyMediaUploader()
    {
        GraphQLObjectType type = context.getOutputType( "MediaUploader" );

        assertEquals( "Media uploader.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 2, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "attachment" ).getType() );
        assertEquals( "MediaFocalPoint", getNameForGraphQLTypeReference( type.getFieldDefinition( "focalPoint" ).getType() ) );
    }

    private void verifyMediaFocalPoint()
    {
        GraphQLObjectType type = context.getOutputType( "MediaFocalPoint" );

        assertEquals( "Media focal point.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 2, fields.size() );
        assertEquals( Scalars.GraphQLFloat, type.getFieldDefinition( "x" ).getType() );
        assertEquals( Scalars.GraphQLFloat, type.getFieldDefinition( "y" ).getType() );
    }

    private void verifyGeoPoint()
    {
        GraphQLObjectType type = context.getOutputType( "GeoPoint" );

        assertEquals( "GeoPoint.", type.getDescription() );

        List<GraphQLFieldDefinition> fields = type.getFieldDefinitions();

        assertEquals( 3, fields.size() );
        assertEquals( Scalars.GraphQLString, type.getFieldDefinition( "value" ).getType() );
        assertEquals( Scalars.GraphQLFloat, type.getFieldDefinition( "latitude" ).getType() );
        assertEquals( Scalars.GraphQLFloat, type.getFieldDefinition( "longitude" ).getType() );
    }
}
