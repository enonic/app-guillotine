package com.enonic.app.guillotine.mcp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import graphql.Scalars;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.GuillotineConfig;
import com.enonic.app.guillotine.GuillotineConfigService;
import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.SchemaProvider;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.Project;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.project.Projects;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.ContentTypes;
import com.enonic.xp.schema.content.GetContentTypeParams;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.testing.ScriptTestSupport;

import static com.enonic.app.guillotine.mcp.McpResults.read;
import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static com.enonic.app.guillotine.mcp.McpResults.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Calls the group controllers the way XP's MCP endpoint does: {@code execute(name, args, invocation)} with {@code args} a plain script
 * object, and reads the result with {@code getMap()}. Runs on the default engine (Nashorn); a subclass repeats it on GraalJS.
 */
public class McpControllerScriptTest
    extends ScriptTestSupport
{
    private static final String CONTENT_CONTROLLER = "/mcp/content/content.js";

    private static final String GRAPHQL_CONTROLLER = "/mcp/graphql/graphql.js";

    private static final String REFERENCES_CONTROLLER = "/mcp/references/references.js";

    private static final AuthenticationInfo ALICE = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).displayName( "Alice" ).build() )
        .principals( RoleKeys.AUTHENTICATED )
        .build();

    private GuillotineConfigService configService;

    private ServiceFacade serviceFacade;

    private SchemaProvider schemaProvider;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        configService = spy( new GuillotineConfigService() );
        configService.activate( mock( GuillotineConfig.class, invocation -> invocation.getMethod().getDefaultValue() ) );

        serviceFacade = mock( ServiceFacade.class );
        when( serviceFacade.getProjectService() ).thenReturn( projectService );
        when( serviceFacade.getContentService() ).thenReturn( contentService );
        final NodeService nodeService = mock( NodeService.class );
        when( nodeService.getByPath( ContentConstants.CONTENT_ROOT_PATH ) ).thenReturn(
            Node.create().name( "content" ).parentPath( NodePath.ROOT ).data( new PropertyTree() ).build() );
        when( serviceFacade.getNodeService() ).thenReturn( nodeService );

        schemaProvider = mock( SchemaProvider.class );
        when( schemaProvider.executeToSpecification( anyString(), anyMap(), isNull(), any() ) ).thenAnswer( invocation -> {
            final Map<String, Object> echo = new LinkedHashMap<>();
            echo.put( "variables", invocation.getArgument( 1 ) );
            echo.put( "where", ContextAccessor.current().getRepositoryId() + "@" + ContextAccessor.current().getBranch() );
            echo.put( "missing", null );
            return Map.of( "data", echo );
        } );

        when( schemaProvider.getSchema() ).thenReturn( articleSchema() );
        final ContentTypeService contentTypeService = mock( ContentTypeService.class );
        when( contentTypeService.getByName( any( GetContentTypeParams.class ) ) ).thenReturn( ContentType.create()
                                                                                              .superType( ContentTypeName.structured() )
                                                                                              .name( "app:article" )
                                                                                              .title( "Article" )
                                                                                              .addFormItem( Input.create()
                                                                                                                .name( "title" )
                                                                                                                .label( "Title" )
                                                                                                                .inputType(
                                                                                                                    InputTypeName.TEXT_LINE )
                                                                                                                .build() )
                                                                                              .build() );
        final ContentTypes registered =
            ContentTypes.from( contentTypeService.getByName( GetContentTypeParams.from( ContentTypeName.from( "app:article" ) ) ) );
        when( contentTypeService.getAll() ).thenReturn( registered );
        when( serviceFacade.getContentTypeService() ).thenReturn( contentTypeService );

        when( projectService.list() ).thenReturn( Projects.from(
            List.of( Project.create().name( ProjectName.from( "blog" ) ).displayName( "Blog" ).parent( ProjectName.from( "base" ) ).build() ) ) );
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.empty() ).totalHits( 12 ).build() );

        addService( ServiceFacade.class, serviceFacade );
        addService( GuillotineConfigService.class, configService );
        addService( SchemaProvider.class, schemaProvider );
    }

    @Test
    public void testListProjectsThroughTheControllerIsTheToolResult()
    {
        final Map<String, Object> result = asAlice( () -> execute( CONTENT_CONTROLLER, "list-projects", Map.of() ) );

        final Map<String, Object> structured = structured( result );
        assertEquals( Map.of( "caller", Map.of( "anonymous", false, "key", "user:system:alice", "displayName", "Alice" ),
                              "defaultProject", "blog", "defaultBranch", "draft", "projects",
                              List.of( Map.of( "name", "blog", "displayName", "Blog", "parent", "base", "itemCount", 12 ) ) ),
                      structured );
        assertEquals( normalized( asAlice( () -> new McpTools( serviceFacade, schemaProvider, configService ).run( "content", "list-projects", "{}" ) ) ),
                      normalized( result ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "list-projects", structured ) );
    }

    @Test
    public void testListTypesAndDescribeTypeThroughTheControllerAreTheToolResults()
    {
        for ( final Map.Entry<String, Map<String, Object>> call : Map.of( "list-types", Map.<String, Object>of(), "describe-type",
                                                                          Map.<String, Object>of( "type", "app:article" ) ).entrySet() )
        {
            final Map<String, Object> result = asAlice( () -> execute( CONTENT_CONTROLLER, call.getKey(), call.getValue() ) );

            final Map<String, Object> structured = structured( result );
            assertEquals( "blog", structured.get( "project" ), call.getKey() );
            assertEquals( normalized( asAlice( () -> new McpTools( serviceFacade, schemaProvider, configService ).run( "content",
                                                                                                                    call.getKey(),
                                                                                                                    write(
                                                                                                                        call.getValue() ) ) ) ),
                          normalized( result ) );
            assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, call.getKey(), structured ) );
        }
        final Map<String, Object> described =
            structured( asAlice( () -> execute( CONTENT_CONTROLLER, "describe-type", Map.of( "type", "Article" ) ) ) );
        assertEquals( "app_Article", described.get( "graphqlType" ) );
        assertEquals( List.of( Map.of( "name", "title", "path", "data.title", "input", "TextLine" ) ), described.get( "fields" ) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSearchAndGetThroughTheContentControllerAreTheToolResults()
    {
        final Map<String, Object> args = new LinkedHashMap<>();
        args.put( "filters", List.of( Map.of( "field", "title", "equals", "x" ), Map.of( "field", "modifiedTime", "from", "2026-01-01" ) ) );
        args.put( "aggregate", List.of( Map.of( "by", "type", "size", 3 ) ) );
        args.put( "count", 0 );

        final Map<String, Object> result = asAlice( () -> execute( CONTENT_CONTROLLER, "search", args ) );

        final Map<String, Object> structured = structured( result );
        assertEquals( Map.of( "project", "blog", "branch", "draft", "total", 0, "hits", List.of(), "aggregations",
                              List.of( Map.of( "by", "type", "buckets", List.of() ) ) ), structured );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "search", structured ) );
        assertEquals( normalized( asAlice( () -> new McpTools( serviceFacade, schemaProvider, configService ).run( "content", "search",
                                                                                                                write( args ) ) ) ),
                      normalized( result ) );
        final ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass( Map.class );
        verify( schemaProvider, atLeastOnce() ).executeToSpecification( anyString(), variables.capture(), isNull(), any() );
        assertEquals( Map.of( "query", Map.of( "boolean", Map.of( "must", List.of(
                          Map.of( "term", Map.of( "field", "data.title", "value", Map.of( "string", "x" ) ) ),
                          Map.of( "range", Map.of( "field", "modifiedTime", "gte", Map.of( "instant", "2026-01-01T00:00:00Z" ) ) ) ) ) ),
                              "first", 0, "sort", List.of( Map.of( "field", "modifiedTime", "direction", "DESC" ),
                                                           Map.of( "field", "_path", "direction", "ASC" ) ), "aggregations",
                              List.of( Map.of( "name", "a0", "terms", Map.of( "field", "type", "size", 3, "order", "_count DESC" ) ) ) ),
                      normalized( variables.getAllValues().get( 0 ) ) );

        final Map<String, Object> get = Map.of( "key", "/articles/a", "fields", List.of( "title" ) );
        final Map<String, Object> missing = asAlice( () -> execute( CONTENT_CONTROLLER, "get", get ) );
        assertEquals( true, missing.get( "isError" ) );
        assertTrue( text( missing ).startsWith( "No content at \"/articles/a\" in project blog on branch draft" ), () -> text( missing ) );
        assertEquals( normalized( asAlice( () -> new McpTools( serviceFacade, schemaProvider, configService ).run( "content", "get",
                                                                                                                write( get ) ) ) ),
                      normalized( missing ) );
    }

    @Test
    public void testAnonymousCallerGetsTheRefusalAsAnErrorResultUntilTheOperatorOptsIn()
    {
        final Map<String, Object> refused = execute( CONTENT_CONTROLLER, "list-projects", Map.of() );

        assertEquals( true, refused.get( "isError" ) );
        assertTrue( text( refused ).contains( "mcp.allowAnonymous=true" ), () -> text( refused ) );

        doReturn( true ).when( configService ).isMcpAllowAnonymous();

        final Map<String, Object> structured = structured( execute( CONTENT_CONTROLLER, "list-projects", Map.of() ) );
        assertEquals( Map.of( "anonymous", true ), structured.get( "caller" ) );
        assertEquals( "master", structured.get( "defaultBranch" ) );
    }

    @Test
    public void testQueryThroughTheGraphqlControllerIsTheToolResult()
    {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put( "key", "a1" );
        nested.put( "filter", Map.of( "in", Arrays.asList( "x", "y" ), "n", 3 ) );
        final Map<String, Object> args = Map.of( "query", "query ($key: ID) { guillotine { get(key: $key) { _id } } }", "variables",
                                                 nested, "branch", "master" );

        final Map<String, Object> result = asAlice( () -> execute( GRAPHQL_CONTROLLER, "query", args ) );

        final Map<String, Object> data = new LinkedHashMap<>();
        data.put( "variables", nested );
        data.put( "where", "com.enonic.cms.blog@master" );
        data.put( "missing", null );
        assertEquals( Map.of( "project", "blog", "branch", "master", "data", data ), structured( result ) );
        assertEquals(
            normalized( asAlice( () -> new McpTools( serviceFacade, schemaProvider, configService ).run( "graphql", "query", write( args ) ) ) ),
            normalized( result ) );
    }

    @Test
    public void testQueryRefusesAnonymousCallersUntilTheOperatorOptsIn()
    {
        final Map<String, Object> args = Map.of( "query", "{ guillotine { getSite { _id } } }" );

        final Map<String, Object> refused = execute( GRAPHQL_CONTROLLER, "query", args );
        assertEquals( true, refused.get( "isError" ) );
        assertTrue( text( refused ).contains( "mcp.allowAnonymous=true" ), () -> text( refused ) );

        doReturn( true ).when( configService ).isMcpAllowAnonymous();

        assertEquals( "master", structured( execute( GRAPHQL_CONTROLLER, "query", args ) ).get( "branch" ) );
    }

    @Test
    public void testReferenceScansThroughTheReferencesControllerAreTheToolResults()
    {
        for ( final Map.Entry<String, Map<String, Object>> call : Map.of( "find-unreferenced",
                                                                          Map.<String, Object>of( "types", List.of( "media:image" ) ),
                                                                          "find-broken-references",
                                                                          Map.<String, Object>of( "path", "/landings" ) ).entrySet() )
        {
            final Map<String, Object> result = asAlice( () -> execute( REFERENCES_CONTROLLER, call.getKey(), call.getValue() ) );

            final Map<String, Object> structured = structured( result );
            assertEquals( "blog", structured.get( "project" ), call.getKey() );
            assertEquals( 12, ( (Number) structured.getOrDefault( "candidates", structured.get( "total" ) ) ).intValue(), call.getKey() );
            assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.REFERENCES, call.getKey(), structured ), call.getKey() );
            assertEquals( normalized( asAlice( () -> new McpTools( serviceFacade, schemaProvider, configService ).run( "references",
                                                                                                                    call.getKey(),
                                                                                                                    write(
                                                                                                                        call.getValue() ) ) ) ),
                          normalized( result ) );
        }
    }

    @Test
    public void testReferenceScansRefuseAnonymousCallersEvenWhenTheOperatorOptsIn()
    {
        doReturn( true ).when( configService ).isMcpAllowAnonymous();

        final Map<String, Object> refused = execute( REFERENCES_CONTROLLER, "find-broken-references", Map.of() );

        assertEquals( true, refused.get( "isError" ) );
        assertTrue( text( refused ).contains( "Anonymous callers cannot use the references tools" ), () -> text( refused ) );
    }

    @Test
    public void testQueryErrorThroughTheControllerIsAnErrorResult()
    {
        final Map<String, Object> result =
            asAlice( () -> execute( GRAPHQL_CONTROLLER, "query", Map.of( "query", "mutation { delete(key: \"a\") }" ) ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( text( result ).startsWith( "Only query operations are allowed" ), () -> text( result ) );
    }

    @Test
    public void testDisabledToolsAnswerWithAnErrorResult()
    {
        doReturn( false ).when( configService ).isMcpEnabled();

        final Map<String, Object> result = asAlice( () -> execute( CONTENT_CONTROLLER, "list-projects", Map.of() ) );

        assertEquals( true, result.get( "isError" ) );
        assertTrue( text( result ).startsWith( "Guillotine MCP tools are disabled by configuration." ), () -> text( result ) );
    }

    @Test
    public void testArgsArriveAsAPlainObjectThatStringifiesToTheirJson()
    {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put( "n", 3 );
        nested.put( "none", null );
        final Map<String, Object> args = new LinkedHashMap<>();
        args.put( "text", "news" );
        args.put( "rating", 5.5 );
        args.put( "types", Arrays.asList( "article", null ) );
        args.put( "variables", nested );

        final Map<String, Object> result = execute( "/mcp/probe/stringify.js", "search", args );

        assertEquals( read( write( args ) ), read( (String) result.get( "json" ) ) );
    }

    private Map<String, Object> execute( final String controller, final String name, final Map<String, ?> args )
    {
        final ScriptValue result = runScript( controller ).executeMethod( "execute", name, new McpArguments( args ), new Object() );
        assertTrue( result != null && result.isObject() && !result.isArray() );
        return result.getMap();
    }

    private static Map<String, Object> normalized( final Object value )
    {
        return read( write( value ) );
    }

    /**
     * A Guillotine-like schema with one content type, {@code app:article}, whose GraphQL type is {@code app_Article}.
     */
    private static GraphQLSchema articleSchema()
    {
        final GraphQLInterfaceType content = GraphQLInterfaceType.newInterface()
            .name( "Content" )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "_id" ).type( Scalars.GraphQLID ) )
            .build();
        final GraphQLObjectType data = GraphQLObjectType.newObject()
            .name( "app_Article_Data" )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "title" ).type( Scalars.GraphQLString ) )
            .build();
        final GraphQLObjectType article = GraphQLObjectType.newObject()
            .name( "app_Article" )
            .description( "Article - app:article" )
            .withInterface( content )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "_id" ).type( Scalars.GraphQLID ) )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "data" ).type( data ) )
            .build();
        final GraphQLObjectType query = GraphQLObjectType.newObject()
            .name( "Query" )
            .field( GraphQLFieldDefinition.newFieldDefinition().name( "article" ).type( article ) )
            .build();
        return GraphQLSchema.newSchema()
            .query( query )
            .codeRegistry( GraphQLCodeRegistry.newCodeRegistry().typeResolver( "Content", env -> article ).build() )
            .build();
    }

    private static <T> T asAlice( final Callable<T> callable )
    {
        return ContextBuilder.create().authInfo( ALICE ).build().callWith( callable );
    }
}
