package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.fetchers.GetAttachmentUrlByNameDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetFieldAsJsonDataFetcher;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newArgument;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class GenericTypesFactory
{
    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    public GenericTypesFactory( final GuillotineContext context, final ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;
    }

    public void create()
    {
        new ConnectionTypeFactory( context ).create();

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

        GraphQLObjectType outputObject = newObject( context.uniqueName( "GeoPoint" ), "GeoPoint.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "latitude", env -> {
            String source = env.getSource();
            return source.split( ",", 2 )[0];
        } );

        context.registerDataFetcher( outputObject.getName(), "longitude", env -> {
            String source = env.getSource();
            return source.split( ",", 2 )[1];
        } );
    }

    private void createMediaFocalPointType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "x", Scalars.GraphQLFloat ) );
        fields.add( outputField( "y", Scalars.GraphQLFloat ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "MediaFocalPoint" ), "Media focal point.", fields );
        context.registerType( outputObject.getName(), outputObject );
    }

    private void createMediaUploaderType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "attachment", Scalars.GraphQLString ) );
        fields.add( outputField( "focalPoint", GraphQLTypeReference.typeRef( "MediaFocalPoint" ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "MediaUploader" ), "Media uploader.", fields );
        context.registerType( outputObject.getName(), outputObject );
    }

    private void createSiteConfiguratorType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "applicationKey", Scalars.GraphQLString ) );
        fields.add( outputField( "configAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "SiteConfigurator" ), "Site configurator.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "configAsJson", new GetFieldAsJsonDataFetcher( "config" ) );
    }

    private void createPublishInfoType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "from", Scalars.GraphQLString ) );
        fields.add( outputField( "to", Scalars.GraphQLString ) );
        fields.add( outputField( "first", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "PublishInfo" ), "Publish information.", fields );
        context.registerType( outputObject.getName(), outputObject );
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
                                                                                               GraphQLTypeReference.typeRef( "UrlType" ) ),
                                                                                  newArgument( "params", ExtendedScalars.Json ) ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "Attachment" ), "Attachment.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "attachmentUrl",
                                     new GetAttachmentUrlByNameDataFetcher( serviceFacade.getPortalUrlGeneratorService() ) );
    }

    private void createIconType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "mimeType", Scalars.GraphQLString ) );
        fields.add( outputField( "modifiedTime", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "Icon" ), "Icon.", fields );
        context.registerType( outputObject.getName(), outputObject );
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
        fields.add( outputField( "icon", GraphQLTypeReference.typeRef( "Icon" ) ) );
        fields.add( outputField( "form", new GraphQLList( GraphQLTypeReference.typeRef( "FormItem" ) ) ) );
        fields.add( outputField( "formAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "ContentType" ), "Content type.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "formAsJson", new GetFieldAsJsonDataFetcher( "form" ) );
    }

    private void createImageStyleType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "name", Scalars.GraphQLString ) );
        fields.add( outputField( "aspectRatio", Scalars.GraphQLString ) );
        fields.add( outputField( "filter", Scalars.GraphQLString ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "ImageStyle" ), "ImageStyle type.", fields );
        context.registerType( outputObject.getName(), outputObject );
    }

    private void createImageType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "image", GraphQLTypeReference.typeRef( "Content" ) ) );
        fields.add( outputField( "ref", Scalars.GraphQLString ) );
        fields.add( outputField( "style", GraphQLTypeReference.typeRef( "ImageStyle" ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "Image" ), "Image type.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "image", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            return new GetContentCommand( serviceFacade.getContentService() ).execute( sourceAsMap.get( "imageId" ).toString(), environment );
        } );

        context.registerDataFetcher( outputObject.getName(), "ref", new GetFieldAsJsonDataFetcher( "imageRef" ) );
    }

    private void createMediaType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "content", GraphQLTypeReference.typeRef( "Content" ) ) );
        fields.add( outputField( "intent", GraphQLTypeReference.typeRef( "MediaIntentType" ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "Media" ), "Media type.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "content", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            if ( sourceAsMap.containsKey( "contentId" ) )
            {
                return new GetContentCommand( serviceFacade.getContentService() ).execute( sourceAsMap.get( "contentId" ).toString(), environment );
            }
            return null;
        } );
    }

    private void createLinkType()
    {
        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "ref", Scalars.GraphQLString ) );
        fields.add( outputField( "uri", Scalars.GraphQLString ) );
        fields.add( outputField( "media", GraphQLTypeReference.typeRef( "Media" ) ) );
        fields.add( outputField( "content", GraphQLTypeReference.typeRef( "Content" ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "Link" ), "Link type.", fields );
        context.registerType( outputObject.getName(), outputObject );

        context.registerDataFetcher( outputObject.getName(), "ref", new GetFieldAsJsonDataFetcher( "linkRef" ) );

        context.registerDataFetcher( outputObject.getName(), "content", environment -> {
            Map<String, Object> sourceAsMap = environment.getSource();
            if ( sourceAsMap.get( "contentId" ) != null )
            {
                return new GetContentCommand( serviceFacade.getContentService() ).execute( sourceAsMap.get( "contentId" ).toString(), environment );
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
        fields.add( outputField( "macros", new GraphQLList( GraphQLTypeReference.typeRef( "Macro" ) ) ) );
        fields.add( outputField( "images", new GraphQLList( GraphQLTypeReference.typeRef( "Image" ) ) ) );
        fields.add( outputField( "links", new GraphQLList( GraphQLTypeReference.typeRef( "Link" ) ) ) );

        GraphQLObjectType outputObject = newObject( context.uniqueName( "RichText" ), "RichText type.", fields );
        context.registerType( outputObject.getName(), outputObject );

		context.registerDataFetcher( outputObject.getName(), "macros", new GetFieldAsJsonDataFetcher( "macrosAsJson" ) );
    }
}
