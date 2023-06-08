package com.enonic.app.guillotine.graphql.factory;

import java.util.ArrayList;
import java.util.List;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.fetchers.GetChildrenConnectionDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetChildrenDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetContentDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetPermissionsDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetSiteDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetTypeDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.GetTypesDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.QueryConnectionDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.QueryDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.QueryDslConnectionDataFetcher;
import com.enonic.app.guillotine.graphql.fetchers.QueryDslDataFetcher;

import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newArgument;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.newObject;
import static com.enonic.app.guillotine.graphql.helper.GraphQLHelper.outputField;

public class HeadlessCmsTypeFactory
{
    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    public HeadlessCmsTypeFactory( final GuillotineContext context, final ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;
    }

    public GraphQLObjectType create()
    {
        List<GraphQLFieldDefinition> fields = createHeadlessCMSFields();

        GraphQLObjectType headlessCms = newObject( context.uniqueName( "HeadlessCms" ), "Headless CMS", fields );

        context.registerType( headlessCms.getName(), headlessCms );

        context.registerDataFetcher( headlessCms.getName(), "get",
                                     new GetContentDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "getChildren",
                                     new GetChildrenDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "getChildrenConnection",
                                     new GetChildrenConnectionDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "getPermissions",
                                     new GetPermissionsDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "getSite",
                                     new GetSiteDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "query", new QueryDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "queryConnection",
                                     new QueryConnectionDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "queryDsl",
                                     new QueryDslDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "queryDslConnection",
                                     new QueryDslConnectionDataFetcher( context, serviceFacade.getContentService() ) );

        context.registerDataFetcher( headlessCms.getName(), "getType",
                                     new GetTypeDataFetcher( context, serviceFacade.getContentTypeService() ) );

        context.registerDataFetcher( headlessCms.getName(), "getTypes",
                                     new GetTypesDataFetcher( context, serviceFacade.getContentTypeService() ) );

        return headlessCms;
    }

    private List<GraphQLFieldDefinition> createHeadlessCMSFields()
    {
        GraphQLInterfaceType contentInterface = context.getInterfaceType( "Content" );

        List<GraphQLFieldDefinition> fields = new ArrayList<>();

        fields.add( outputField( "get", contentInterface, newArgument( "key", Scalars.GraphQLID ) ) );

        fields.add( outputField( "getChildren", new GraphQLList( contentInterface ),
                                 List.of( newArgument( "key", Scalars.GraphQLID ), newArgument( "offset", Scalars.GraphQLInt ),
                                          newArgument( "first", Scalars.GraphQLInt ), newArgument( "sort", Scalars.GraphQLString ) ) ) );

        fields.add( outputField( "getChildrenConnection", context.getOutputType( "ContentConnection" ),
                                 List.of( newArgument( "key", Scalars.GraphQLID ), newArgument( "after", Scalars.GraphQLString ),
                                          newArgument( "first", Scalars.GraphQLInt ), newArgument( "sort", Scalars.GraphQLString ) ) ) );

        fields.add( outputField( "getPermissions", context.getOutputType( "Permissions" ), newArgument( "key", Scalars.GraphQLID ) ) );

        fields.add( outputField( "getSite", new GraphQLTypeReference( "portal_Site" ) ) );

        fields.add( outputField( "query", new GraphQLList( contentInterface ),
                                 List.of( newArgument( "query", Scalars.GraphQLString ), newArgument( "offset", Scalars.GraphQLInt ),
                                          newArgument( "first", Scalars.GraphQLInt ), newArgument( "sort", Scalars.GraphQLString ),
                                          newArgument( "contentTypes", new GraphQLList( Scalars.GraphQLString ) ),
                                          newArgument( "filters", new GraphQLList( context.getInputType( "FilterInput" ) ) ) ) ) );

        fields.add( outputField( "queryConnection", context.getOutputType( "QueryContentConnection" ),
                                 List.of( newArgument( "query", new GraphQLNonNull( Scalars.GraphQLString ) ),
                                          newArgument( "after", Scalars.GraphQLString ), newArgument( "first", Scalars.GraphQLInt ),
                                          newArgument( "sort", Scalars.GraphQLString ),
                                          newArgument( "contentTypes", new GraphQLList( Scalars.GraphQLString ) ),
                                          newArgument( "aggregations", new GraphQLList( context.getInputType( "AggregationInput" ) ) ),
                                          newArgument( "filters", new GraphQLList( context.getInputType( "FilterInput" ) ) ) ) ) );

        fields.add( outputField( "queryDsl", new GraphQLList( contentInterface ),
                                 List.of( newArgument( "query", context.getInputType( "QueryDSLInput" ) ),
                                          newArgument( "offset", Scalars.GraphQLInt ), newArgument( "first", Scalars.GraphQLInt ),
                                          newArgument( "sort", new GraphQLList( context.getInputType( "SortDslInput" ) ) ) ) ) );

        fields.add( outputField( "queryDslConnection", context.getOutputType( "QueryDSLContentConnection" ),
                                 List.of( newArgument( "query", new GraphQLNonNull( context.getInputType( "QueryDSLInput" ) ) ),
                                          newArgument( "after", Scalars.GraphQLString ), newArgument( "first", Scalars.GraphQLInt ),
                                          newArgument( "aggregations", new GraphQLList( context.getInputType( "AggregationInput" ) ) ),
                                          newArgument( "highlight", context.getInputType( "HighlightInputType" ) ),
                                          newArgument( "sort", new GraphQLList( context.getInputType( "SortDslInput" ) ) ) ) ) );

        fields.add( outputField( "getType", context.getOutputType( "ContentType" ),
                                 newArgument( "name", new GraphQLNonNull( Scalars.GraphQLString ) ) ) );

        fields.add( outputField( "getTypes", new GraphQLList( context.getOutputType( "ContentType" ) ) ) );

        return fields;
    }
}
