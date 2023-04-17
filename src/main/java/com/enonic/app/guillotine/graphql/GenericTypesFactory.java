package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.fetchers.GetAttachmentUrlByNameDataFetcher;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;

import static com.enonic.app.guillotine.graphql.GraphQLHelper.newArgument;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.GraphQLHelper.outputField;

public class GenericTypesFactory
{
    private final GuillotineContext guillotineContext;

    private final ServiceFacade serviceFacade;

    public GenericTypesFactory( final GuillotineContext guillotineContext, final ServiceFacade serviceFacade )
    {
        this.guillotineContext = guillotineContext;
        this.serviceFacade = serviceFacade;
    }

    public void create()
    {
        new ConnectionTypeFactory( guillotineContext ).create();

        createGeoPointType();
        createMediaFocalPointType();
        createMediaUploaderType();
        createSiteConfiguratorType();
        createPublishInfoType();
        createAttachmentType();
        createIconType();
        createContentTypeType();
        createImageStyleType();
        createImageType();
        createMediaType();
        createLinkType();
        createRichTextType();
    }

    private void createGeoPointType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "value", Scalars.GraphQLString ) );
        fields.add( outputField( "latitude", Scalars.GraphQLFloat ) );
        fields.add( outputField( "longitude", Scalars.GraphQLFloat ) );

        GraphQLObjectType outputObject = newObject( "GeoPoint", "GeoPoint.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "latitude", env -> {
            String source = env.getSource();
            return source.split( ",", 2 )[0];
        } );

        guillotineContext.registerDataFetcher( outputObject.getName(), "longitude", env -> {
            String source = env.getSource();
            return source.split( ",", 2 )[1];
        } );
    }

    private void createMediaFocalPointType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "x", Scalars.GraphQLFloat ) );
        fields.add( outputField( "y", Scalars.GraphQLFloat ) );

        GraphQLObjectType outputObject = newObject( "MediaFocalPoint", "Media focal point.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );
    }

    private void createMediaUploaderType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "attachment", Scalars.GraphQLString ) );
        fields.add( outputField( "focalPoint", guillotineContext.getOutputType( "MediaFocalPoint" ) ) );

        GraphQLObjectType outputObject = newObject( "MediaUploader", "Media uploader.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );
    }

    private void createSiteConfiguratorType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "applicationKey", Scalars.GraphQLString ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType outputObject = newObject( "SiteConfigurator", "Site configurator.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "configAsJson", env -> {
            Map<String, Object> sourceAsMap = env.getSource();
            return sourceAsMap.get( "config" );
        } );
    }

    private void createPublishInfoType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "from", Scalars.GraphQLString ) );
        fields.add( outputField( "to", Scalars.GraphQLString ) );
        fields.add( outputField( "first", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( "PublishInfo", "Publish information.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );
    }

    private void createAttachmentType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "label", Scalars.GraphQLString ) );
        fields.add( outputField( "size", Scalars.GraphQLInt ) );
        fields.add( outputField( "mimeType", Scalars.GraphQLString ) );
        fields.add( outputField( "attachmentUrl", Scalars.GraphQLString, List.of( newArgument( "download", Scalars.GraphQLBoolean ),
                                                                                  newArgument( "type",
                                                                                               guillotineContext.getEnumType( "UrlType" ) ),
                                                                                  newArgument( "params", Scalars.GraphQLString ) ) ) );

        GraphQLObjectType outputObject = newObject( "Attachment", "Attachment.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "attachmentUrl",
                                               new GetAttachmentUrlByNameDataFetcher( serviceFacade.getPortalUrlService() ) );
    }

    private void createIconType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "mimeType", Scalars.GraphQLString ) );
        fields.add( outputField( "modifiedTime", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( "Icon", "Icon.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );
    }

    private void createContentTypeType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "displayName", Scalars.GraphQLString ) );
        fields.add( outputField( "description", Scalars.GraphQLString ) );
        fields.add( outputField( "superType", Scalars.GraphQLString ) );
        fields.add( outputField( "abstract", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "final", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "allowChildContent", Scalars.GraphQLBoolean ) );
        fields.add( outputField( "contentDisplayNameScript", Scalars.GraphQLString ) );
        fields.add( outputField( "icon", guillotineContext.getOutputType( "Icon" ) ) );
        fields.add( outputField( "form", new GraphQLList( new GraphQLTypeReference( "FormItem" ) ) ) );
        fields.add( outputField( "formAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType outputObject = newObject( "ContentType", "Content type.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "formAsJson", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "form" );
        } );
    }

    private void createImageStyleType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "aspectRatio", Scalars.GraphQLString ) );
        fields.add( outputField( "filter", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( "ImageStyle", "ImageStyle type.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );
    }

    private void createImageType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "image", new GraphQLTypeReference( "Content" ) ) );
        fields.add( outputField( "ref", Scalars.GraphQLString ) );
        fields.add( outputField( "style", guillotineContext.getOutputType( "ImageStyle" ) ) );

        GraphQLObjectType outputObject = newObject( "Image", "Image type.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "image", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return new GetContentCommand( serviceFacade.getContentService() ).execute( sourceAsMap.get( "imageId" ).toString() );
        } );

        guillotineContext.registerDataFetcher( outputObject.getName(), "ref", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "imageRef" );
        } );
    }

    private void createMediaType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "content", new GraphQLTypeReference( "Content" ) ) );
        fields.add( outputField( "intent", guillotineContext.getEnumType( "MediaIntentType" ) ) );

        GraphQLObjectType outputObject = newObject( "Media", "Media type.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "content", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            if ( sourceAsMap.containsKey( "contentId" ) )
            {
                return new GetContentCommand( serviceFacade.getContentService() ).execute( sourceAsMap.get( "contentId" ).toString() );
            }
            return null;
        } );
    }

    private void createLinkType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "ref", Scalars.GraphQLString ) );
        fields.add( outputField( "uri", Scalars.GraphQLString ) );
        fields.add( outputField( "media", guillotineContext.getOutputType( "Media" ) ) );
        fields.add( outputField( "content", new GraphQLTypeReference( "Content" ) ) );

        GraphQLObjectType outputObject = newObject( "Link", "Link type.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "ref", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "linkRef" );
        } );

        guillotineContext.registerDataFetcher( outputObject.getName(), "content", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            if ( sourceAsMap.get( "contentId" ) != null )
            {
                return new GetContentCommand( serviceFacade.getContentService() ).execute( sourceAsMap.get( "contentId" ).toString() );
            }
            return null;
        } );
    }

    private void createRichTextType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "raw", Scalars.GraphQLString ) );
        fields.add( outputField( "processedHtml", Scalars.GraphQLString ) );
        fields.add( outputField( "macrosAsJson", ExtendedScalars.Json ) );
        fields.add( outputField( "macros", new GraphQLList( guillotineContext.getOutputType( "Macro" ) ) ) );
        fields.add( outputField( "images", new GraphQLList( guillotineContext.getOutputType( "Image" ) ) ) );
        fields.add( outputField( "links", new GraphQLList( guillotineContext.getOutputType( "Link" ) ) ) );

        GraphQLObjectType outputObject = newObject( "RichText", "RichText type.", fields );
        guillotineContext.registerType( outputObject.getName(), outputObject );

        guillotineContext.registerDataFetcher( outputObject.getName(), "macros", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return sourceAsMap.get( "macrosAsJson" );
        } );
    }
}
