package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.ContentTypeDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.FormItemDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetAsJsonWithoutContentIdDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetAttachmentUrlByIdDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetAttachmentsDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetComponentsDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentChildrenConnectionDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentChildrenDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentDataDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentFieldDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentParentDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentReferencesDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentSiteDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetImageUrlDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetPageAsJsonDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetPageTemplateDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetPageUrlDataFetcher;
import com.enonic.app.guillotine.graphql.helper.FormItemTypesHelper;
import com.enonic.app.guillotine.graphql.helper.NamingHelper;
import com.enonic.app.guillotine.graphql.helper.StringNormalizer;
import com.enonic.app.guillotine.graphql.typeresolvers.ContentTypeResolver;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newArgument;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newInterface;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class ContentTypesFactory
{
    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    private final FormItemTypesFactory formItemTypesFactory;

    public ContentTypesFactory( final GuillotineContext context, final ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;

        this.formItemTypesFactory = new FormItemTypesFactory( context, serviceFacade );
    }

    public void create()
    {
        GraphQLInterfaceType contentInterface = createContentInterface();
        context.registerType( contentInterface.getName(), contentInterface );

        new ConnectionTypeFactory( context ).createConnectionType( contentInterface.getName() );
        createQueryDslContentConnectionType();

        GraphQLObjectType untypedContent = newObject( context.uniqueName( "UntypedContent" ), "UntypedContent", List.of( contentInterface ),
                                                      getGenericContentFields( "UntypedContent" ) );
        context.registerContentType( untypedContent.getName(), untypedContent );

        Pattern allowedContentTypesPattern = generateAllowedContentTypeRegexp();

        List<ContentType> contentTypes = serviceFacade.getContentTypeService().getAll().stream().filter(
            contentType -> allowedContentTypesPattern.matcher( contentType.getName().toString() ).find() ).collect( Collectors.toList() );

        contentTypes.forEach( contentType -> createContentObjectType( contentType, contentInterface ) );
    }

    private void createQueryDslContentConnectionType()
    {
        GraphQLObjectType edgeType = context.getOutputType( "ContentEdge" );

        List<GraphQLFieldDefinition> fields = new ArrayList<>();
        fields.add( outputField( "aggregationsAsJson", ExtendedScalars.Json ) );
        fields.add( outputField( "highlightAsJson", ExtendedScalars.Json ) );

        GraphQLObjectType outputObject = new ConnectionTypeFactory( context ).createConnectionType( "QueryDSLContent", edgeType, fields );

        context.registerType( outputObject.getName(), outputObject );
    }

    private GraphQLInterfaceType createContentInterface()
    {
        GraphQLInterfaceType result = newInterface( context.uniqueName( "Content" ), "Content.", getGenericContentFields( "Content" ) );

        context.registerTypeResolver( result.getName(), new ContentTypeResolver( context ) );

        return result;
    }

    private Pattern generateAllowedContentTypeRegexp()
    {
        String applicationKeys =
            context.getApplications().stream().map( applicationKey -> "|" + applicationKey.replaceAll( "\\\\.", "\\." ) ).collect(
                Collectors.joining() );

        return Pattern.compile( "^(?:base|media|portal" + applicationKeys + "):" );
    }

    private void createContentObjectType( ContentType contentType, GraphQLInterfaceType contentInterface )
    {
        String typeName = generateContentTypeName( contentType.getName() );
        String typeDescription = contentType.getDisplayName() + " - " + contentType.getName();

        List<GraphQLFieldDefinition> fields = new ArrayList<>( getGenericContentFields( typeName ) );

        if ( contentType.getName().toString().startsWith( "media:" ) )
        {
            GraphQLFieldDefinition mediaUrlField = createMediaUrlField();

            fields.add( mediaUrlField );
            context.registerDataFetcher( typeName, mediaUrlField.getName(),
                                         new GetAttachmentUrlByIdDataFetcher( serviceFacade.getPortalUrlService() ) );

            if ( contentType.getName().toString().equals( "media:image" ) )
            {
                GraphQLFieldDefinition imageUrlField = createImageUrlField();

                fields.add( imageUrlField );
                context.registerDataFetcher( typeName, imageUrlField.getName(),
                                             new GetImageUrlDataFetcher( serviceFacade.getPortalUrlService() ) );
            }
        }

        List<FormItem> formItems = FormItemTypesHelper.getFilteredFormItems( contentType.getForm().getFormItems() );
        if ( !formItems.isEmpty() )
        {
            GraphQLObjectType dataObject = generateContentDataType( typeName, typeDescription, formItems );
            fields.add( outputField( "data", dataObject ) );

            context.registerType( dataObject.getName(), dataObject );
            context.registerDataFetcher( typeName, "data", new GetContentDataDataFetcher() );
        }

        GraphQLObjectType contentObject = newObject( context.uniqueName( typeName ), typeDescription, List.of( contentInterface ), fields );

        context.registerContentType( contentType.getName().toString(), contentObject );
    }

    private GraphQLFieldDefinition createMediaUrlField()
    {
        List<GraphQLArgument> arguments = new ArrayList<>();

        arguments.add( newArgument( "download", Scalars.GraphQLBoolean ) );
        arguments.add( newArgument( "type", GraphQLTypeReference.typeRef( "UrlType" ) ) );
        arguments.add( newArgument( "params", ExtendedScalars.Json ) );

        return outputField( "mediaUrl", Scalars.GraphQLString, arguments );
    }

    private GraphQLFieldDefinition createImageUrlField()
    {
        List<GraphQLArgument> arguments = new ArrayList<>();

        arguments.add( newArgument( "scale", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        arguments.add( newArgument( "quality", Scalars.GraphQLInt ) );
        arguments.add( newArgument( "type", GraphQLTypeReference.typeRef( "UrlType" ) ) );
        arguments.add( newArgument( "background", Scalars.GraphQLString ) );
        arguments.add( newArgument( "format", Scalars.GraphQLString ) );
        arguments.add( newArgument( "filter", Scalars.GraphQLString ) );
        arguments.add( newArgument( "params", Scalars.GraphQLString ) );

        return outputField( "imageUrl", Scalars.GraphQLString, arguments );
    }

    private GraphQLObjectType generateContentDataType( String parentTypeName, String parentDescription, List<FormItem> formItems )
    {
        String typeName = parentTypeName + "_Data";
        String description = parentDescription + " data";

        List<GraphQLFieldDefinition> fields = formItems.stream().map( formItem -> {
            String fieldName = StringNormalizer.create( formItem.getName() );

            GraphQLOutputType formItemObject = (GraphQLOutputType) formItemTypesFactory.generateFormItemObject( parentTypeName, formItem );

            GraphQLFieldDefinition field =
                outputField( fieldName, formItemObject, formItemTypesFactory.generateFormItemArguments( formItem ) );

            context.registerDataFetcher( typeName, fieldName, new FormItemDataFetcher( formItem, serviceFacade, context ) );

            return field;
        } ).collect( Collectors.toList() );

        return newObject( context.uniqueName( typeName ), description, fields );
    }


    private String generateContentTypeName( ContentTypeName name )
    {
        String applicationKey = StringNormalizer.create( name.getApplicationKey().toString() );
        String localName = StringNormalizer.create( name.getLocalName() );
        return applicationKey + "_" + NamingHelper.camelCase( localName );
    }


    private List<GraphQLFieldDefinition> getGenericContentFields( String contentType )
    {
        List<GraphQLFieldDefinition> result = new ArrayList<>();

        result.add( outputField( "_id", new GraphQLNonNull( Scalars.GraphQLID ) ) );
        result.add( outputField( "_name", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        result.add( outputField( "_path", new GraphQLNonNull( Scalars.GraphQLString ) ) );
        result.add( outputField( "_references", new GraphQLList( GraphQLTypeReference.typeRef( "Content" ) ) ) );
        result.add( outputField( "_score", Scalars.GraphQLFloat ) );
        result.add( outputField( "creator", GraphQLTypeReference.typeRef( "PrincipalKey" ) ) );
        result.add( outputField( "modifier", GraphQLTypeReference.typeRef( "PrincipalKey" ) ) );
        result.add( outputField( "createdTime", ExtendedScalars.DateTime ) );
        result.add( outputField( "modifiedTime", ExtendedScalars.DateTime ) );
        result.add( outputField( "owner", GraphQLTypeReference.typeRef( "PrincipalKey" ) ) );
        result.add( outputField( "type", Scalars.GraphQLString ) );
        result.add( outputField( "contentType", GraphQLTypeReference.typeRef( "ContentType" ) ) );
        result.add( outputField( "displayName", Scalars.GraphQLString ) );
        result.add( outputField( "hasChildren", Scalars.GraphQLBoolean ) );
        result.add( outputField( "language", Scalars.GraphQLString ) );
        result.add( outputField( "valid", Scalars.GraphQLBoolean ) );
        result.add( outputField( "dataAsJson", ExtendedScalars.Json ) );
        result.add( outputField( "x", GraphQLTypeReference.typeRef( "ExtraData" ) ) );
        result.add( outputField( "xAsJson", ExtendedScalars.Json ) );
        result.add( outputField( "pageAsJson", ExtendedScalars.Json, List.of( newArgument( "resolveTemplate", Scalars.GraphQLBoolean ),
                                                                              newArgument( "resolveFragment",
                                                                                           Scalars.GraphQLBoolean ) ) ) );
        result.add( outputField( "pageTemplate", GraphQLTypeReference.typeRef( "Content" ) ) );
        result.add( outputField( "components", new GraphQLList( GraphQLTypeReference.typeRef( "Component" ) ),
                                 List.of( newArgument( "resolveTemplate", Scalars.GraphQLBoolean ),
                                          newArgument( "resolveFragment", Scalars.GraphQLBoolean ) ) ) );
        result.add( outputField( "attachments", new GraphQLList( GraphQLTypeReference.typeRef( "Attachment" ) ) ) );
        result.add( outputField( "publish", GraphQLTypeReference.typeRef( "PublishInfo" ) ) );
        result.add( outputField( "pageUrl", Scalars.GraphQLString, List.of( newArgument( "params", ExtendedScalars.Json ) ) ) );
        result.add( outputField( "site", GraphQLTypeReference.typeRef( "portal_Site" ) ) );
        result.add( outputField( "parent", GraphQLTypeReference.typeRef( "Content" ) ) );
        result.add( outputField( "children", new GraphQLList( GraphQLTypeReference.typeRef( "Content" ) ),
                                 List.of( newArgument( "offset", Scalars.GraphQLInt ), newArgument( "first", Scalars.GraphQLInt ),
                                          newArgument( "sort", Scalars.GraphQLString ) ) ) );
        result.add( outputField( "childrenConnection", GraphQLTypeReference.typeRef( "ContentConnection" ),
                                 List.of( newArgument( "after", Scalars.GraphQLString ), newArgument( "first", Scalars.GraphQLInt ),
                                          newArgument( "sort", Scalars.GraphQLString ) ) ) );
        result.add( outputField( "permissions", GraphQLTypeReference.typeRef( "Permissions" ) ) );

        context.registerDataFetcher( contentType, "contentType",
                                     new ContentTypeDataFetcher( serviceFacade.getMixinService(), serviceFacade.getContentTypeService() ) );

        context.registerDataFetcher( contentType, "_references", new GetContentReferencesDataFetcher( serviceFacade.getContentService() ) );

        context.registerDataFetcher( contentType, "creator", new GetContentFieldDataFetcher( "creator" ) );

        context.registerDataFetcher( contentType, "modifier", new GetContentFieldDataFetcher( "modifier" ) );

        context.registerDataFetcher( contentType, "attachments", new GetAttachmentsDataFetcher() );

        context.registerDataFetcher( contentType, "parent", new GetContentParentDataFetcher( serviceFacade.getContentService() ) );

        context.registerDataFetcher( contentType, "owner", new GetContentFieldDataFetcher( "owner" ) );

        context.registerDataFetcher( contentType, "dataAsJson", new GetAsJsonWithoutContentIdDataFetcher( "data" ) );

        context.registerDataFetcher( contentType, "xAsJson", new GetAsJsonWithoutContentIdDataFetcher( "x" ) );

        context.registerDataFetcher( contentType, "pageAsJson", new GetPageAsJsonDataFetcher( serviceFacade ) );

        context.registerDataFetcher( contentType, "pageTemplate", new GetPageTemplateDataFetcher( serviceFacade ) );

        context.registerDataFetcher( contentType, "components", new GetComponentsDataFetcher( serviceFacade ) );

        context.registerDataFetcher( contentType, "permissions", new GetContentFieldDataFetcher( "permissions" ) );

        context.registerDataFetcher( contentType, "pageUrl", new GetPageUrlDataFetcher( serviceFacade.getPortalUrlService() ) );

        context.registerDataFetcher( contentType, "site", new GetContentSiteDataFetcher( serviceFacade.getContentService() ) );

        context.registerDataFetcher( contentType, "children", new GetContentChildrenDataFetcher( serviceFacade.getContentService() ) );

        context.registerDataFetcher( contentType, "childrenConnection",
                                     new GetContentChildrenConnectionDataFetcher( serviceFacade.getContentService() ) );

        return result;
    }
}
