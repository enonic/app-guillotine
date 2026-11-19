package com.enonic.app.guillotine.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.BaseGraphQLIntegrationTest;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.event.Event;
import com.enonic.xp.event.EventPublisher;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.project.Projects;
import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.aggregation.TermsAggregationQuery;
import com.enonic.xp.query.expr.DslExpr;
import com.enonic.xp.script.ScriptExports;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.event.ScriptEventListener;
import com.enonic.xp.script.event.ScriptEventManager;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Calls {@code mcp/content/content.js} the way XP's MCP endpoint does: {@code execute(name, args, invocation)} with {@code args} a plain
 * {@code java.util.Map} and a third host object, and reads the result with {@code getMap()}. Runs on the default engine (Nashorn); a
 * subclass repeats it on GraalJS.
 */
public class McpControllerScriptTest
    extends BaseGraphQLIntegrationTest
{
    private static final String CONTROLLER = "/mcp/content/content.js";

    private final List<ContentQuery> searches = new ArrayList<>();

    private McpTools tools;

    private final List<ScriptEventListener> listeners = new ArrayList<>();

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();

        doReturn( "myproject" ).when( guillotineConfigService ).getMcpDefaultProject();
        final NodeService nodeService = mock( NodeService.class );
        when( nodeService.getByPath( ContentConstants.CONTENT_ROOT_PATH ) ).thenReturn(
            Node.create().name( "content" ).parentPath( NodePath.ROOT ).data( new PropertyTree() ).build() );
        when( projectService.list() ).thenReturn( Projects.empty() );
        when( serviceFacade.getNodeService() ).thenReturn( nodeService );
        when( serviceFacade.getProjectService() ).thenReturn( projectService );

        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> {
            searches.add( invocation.getArgument( 0 ) );
            return FindContentIdsByQueryResult.create().contents( ContentIds.empty() ).totalHits( 0 ).build();
        } );
        when( contentService.getByIds( any( GetContentByIdsParams.class ) ) ).thenReturn( Contents.empty() );

        tools = new McpTools( serviceFacade, guillotineConfigService, schemaProvider );

        final ScriptEventManager eventManager = mock( ScriptEventManager.class );
        doAnswer( invocation -> listeners.add( invocation.getArgument( 0 ) ) ).when( eventManager ).add( any() );
        addService( ScriptEventManager.class, eventManager );
        addService( EventPublisher.class, mock( EventPublisher.class ) );
    }

    @Test
    public void testControllerReusesTheSchemaUntilApplicationsChange()
    {
        final Map<String, Object> query = Map.of( "query", "{ guillotine { get(key: \"/nope\") { _path } } }" );
        final GraphQLSchema before = schemaProvider.getSchema();
        clearInvocations( applicationService );

        assertFalse( execute( "query", query ).containsKey( "isError" ) );
        assertFalse( execute( "query", query ).containsKey( "isError" ) );
        verify( applicationService, never() ).getInstalledApplications();
        assertSame( before, schemaProvider.getSchema() );

        assertFalse( listeners.isEmpty() );
        listeners.forEach( listener -> listener.onEvent(
            Event.create( "application" ).localOrigin( true ).value( "eventType", "STARTED" ).value( "key", "com.example.app" ).build() ) );

        assertFalse( execute( "query", query ).containsKey( "isError" ) );
        verify( applicationService, atLeastOnce() ).getInstalledApplications();
        final GraphQLSchema rebuilt = schemaProvider.getSchema();
        assertNotSame( before, rebuilt );

        clearInvocations( applicationService );
        assertFalse( execute( "query", query ).containsKey( "isError" ) );
        verify( applicationService, never() ).getInstalledApplications();
        assertSame( rebuilt, schemaProvider.getSchema() );
    }

    @Test
    public void testJavaMapArgsReachTheToolAsTheSameJsonArguments()
    {
        final Map<String, Object> direct = tools.run( "search", McpJson.write( searchArgs() ) );
        final Map<String, Object> viaController = execute( "search", searchArgs() );

        assertFalse( direct.containsKey( "isError" ), () -> McpJson.write( direct ) );
        assertEquals( 2, searches.size() );
        assertSameSearch( searches.get( 0 ), searches.get( 1 ) );
        assertEquals( normalized( direct ), normalized( viaController ) );
    }

    @Test
    public void testScriptObjectArgsReachTheToolAsTheSameJsonArguments()
    {
        final Map<String, Object> direct = tools.run( "search", McpJson.write( searchArgs() ) );
        final Map<String, Object> viaController = execute( "search", serializable( searchArgs() ) );

        assertEquals( 2, searches.size() );
        assertSameSearch( searches.get( 0 ), searches.get( 1 ) );
        assertEquals( normalized( direct ), normalized( viaController ) );
    }

    @Test
    public void testNestedObjectArgumentIsPassedThrough()
    {
        final Map<String, Object> args = new LinkedHashMap<>();
        args.put( "query", "query($n:Int){guillotine{queryDsl(query:{matchAll:{}},first:$n){_id}}}" );
        args.put( "variables", Map.of( "n", 3 ) );

        final Map<String, Object> result = execute( "query", args );

        assertFalse( result.containsKey( "isError" ), () -> McpJson.write( result ) );
        assertEquals( 1, searches.size() );
        assertEquals( 3, searches.get( 0 ).getSize() );
    }

    @Test
    public void testMissingArgsAreAnEmptyObject()
    {
        assertEquals( "type is required. Pass a content type name from list-types, such as \"app.key:article\".",
                      errorText( executeWithoutArgs( "describe-type" ) ) );
    }

    @Test
    public void testNullArrayItemInArgsIsRejectedNotDropped()
    {
        final Map<String, Object> args = new LinkedHashMap<>();
        args.put( "types", java.util.Arrays.asList( "article", null ) );

        final String error = errorText( execute( "search", args ) );
        assertTrue( error.startsWith( "Invalid type: null." ), error );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testNonObjectArgsAreAnErrorResult()
    {
        assertEquals( "Invalid arguments for search: expected a JSON object.", errorText( execute( "search", "news" ) ) );
    }

    @Test
    public void testResultKeepsNoNullsAndMatchesItsTextJson()
    {
        final Map<String, Object> result = execute( "list-projects", Map.of() );

        assertFalse( result.containsKey( "isError" ), () -> McpJson.write( result ) );
        final Object structured = result.get( "structuredContent" );
        assertNotNull( structured );
        assertFalse( McpJson.write( structured ).contains( "null" ) );
        final String text = text( result );
        assertEquals( McpJson.readObject( text.substring( text.indexOf( '\n' ) + 1 ) ), normalized( structured ) );
    }

    @Test
    public void testJsonStringifyOfArgsIsWhatTheEngineBreaks()
    {
        final ScriptValue result = runScript( "/mcp/probe/stringify.js" ).executeMethod( "execute", "search", Map.of( "text", "news" ),
                                                                                         new Object() );

        assertEquals( stringifiedJavaMap(), result.getMap().get( "json" ) );
    }

    @Test
    public void testQueryFieldResolvingToNullSurvivesTheScriptHop()
    {
        final Map<String, Object> result = execute( "query", Map.of( "query", "{ guillotine { get(key: \"/nope\") { _path } } }" ) );

        assertFalse( result.containsKey( "isError" ), () -> McpJson.write( result ) );
        final Map<?, ?> structured = (Map<?, ?>) result.get( "structuredContent" );
        final Map<?, ?> guillotine = (Map<?, ?>) ( (Map<?, ?>) structured.get( "data" ) ).get( "guillotine" );
        assertTrue( guillotine.containsKey( "get" ), guillotine::toString );
        assertNull( guillotine.get( "get" ) );
    }

    @Test
    public void testNullsSurviveTheScriptHopExceptAsArrayItems()
    {
        final Map<String, Object> result = runScript( "/mcp/probe/nulls.js" ).executeMethod( "execute" ).getMap();

        assertTrue( result.containsKey( "value" ) );
        assertNull( result.get( "value" ) );
        final Map<?, ?> nested = (Map<?, ?>) result.get( "nested" );
        assertTrue( nested.containsKey( "value" ) );
        assertNull( nested.get( "value" ) );
        assertEquals( List.of( 1, 2 ), ( (List<?>) result.get( "list" ) ).stream().map( item -> ( (Number) item ).intValue() ).toList() );
    }

    /**
     * What {@code JSON.stringify} makes of a {@code java.util.Map} argument: nothing on Nashorn.
     */
    protected String stringifiedJavaMap()
    {
        return null;
    }

    @Test
    public void testDescriptorDeclaresExactlyTheRegisteredToolsWithHyphenatedNames()
    {
        final List<String> declared = ToolDescriptor.tools().stream().map( tool -> (String) tool.get( "name" ) ).toList();
        final String unknown = errorText( tools.run( "nope", "{}" ) );

        assertEquals( "Unknown Guillotine MCP tool: nope. Available tools: " + String.join( ", ", declared ) + ".", unknown );
        assertEquals( List.of( "list-projects", "list-types", "search", "get", "describe-type", "query", "find-references",
                               "check-links" ), declared );
        declared.forEach( name -> assertTrue( name.matches( "[a-z]+(-[a-z]+)*" ), name ) );
    }

    @Test
    public void testEveryDeclaredOutputSchemaIsCheckedAgainstToolResults()
    {
        final List<String> withOutputSchema = ToolDescriptor.tools()
            .stream()
            .filter( tool -> tool.containsKey( "outputSchema" ) )
            .map( tool -> (String) tool.get( "name" ) )
            .toList();

        assertEquals( List.of( "list-projects", "list-types" ), withOutputSchema,
                      "the tools that declare an outputSchema changed: validate the new tool's results, including truncated ones, with " +
                          "ToolDescriptor.outputSchemaErrors, then update this list" );
    }

    private Map<String, Object> execute( final String name, final Object args )
    {
        final ScriptExports exports = runScript( CONTROLLER );
        final ScriptValue result = exports.executeMethod( "execute", name, args, new Object() );
        assertTrue( result != null && result.isObject() && !result.isArray() );
        return result.getMap();
    }

    private Map<String, Object> executeWithoutArgs( final String name )
    {
        return runScript( CONTROLLER ).executeMethod( "execute", name ).getMap();
    }

    private static Map<String, Object> searchArgs()
    {
        final Map<String, Object> range = new LinkedHashMap<>();
        range.put( "field", "data.rating" );
        range.put( "from", 1 );
        range.put( "to", 5.5 );
        final Map<String, Object> aggregate = new LinkedHashMap<>();
        aggregate.put( "by", "data.author" );
        aggregate.put( "size", 5 );

        final Map<String, Object> args = new LinkedHashMap<>();
        args.put( "text", "spring news" );
        args.put( "types", List.of( "article", "base:folder" ) );
        args.put( "path", "/articles" );
        args.put( "workflow", "READY" );
        args.put( "filters", List.of( range, Map.of( "field", "data.featured", "equals", true ),
                                      Map.of( "field", "language", "equals", "en" ) ) );
        args.put( "sort", "displayName DESC" );
        args.put( "aggregate", List.of( aggregate ) );
        args.put( "count", 7 );
        return args;
    }

    private static void assertSameSearch( final ContentQuery expected, final ContentQuery actual )
    {
        final String dsl = dsl( actual ).toString();
        assertEquals( dsl( expected ), dsl( actual ) );
        assertEquals( expected.getQueryExpr().getOrderList().toString(), actual.getQueryExpr().getOrderList().toString() );
        assertEquals( expected.getFrom(), actual.getFrom() );
        assertEquals( 7, actual.getSize() );
        assertEquals( aggregationNames( expected ), aggregationNames( actual ) );
        assertEquals( List.of( "a0:5" ), aggregationNames( actual ) );
        assertTrue( dsl.contains( "spring news" ) && dsl.contains( "5.5" ) && dsl.contains( "data.featured" ), dsl );
    }

    private static Map<String, Object> dsl( final ContentQuery query )
    {
        return ( (DslExpr) query.getQueryExpr().getConstraint() ).getExpression().toMap();
    }

    private static List<String> aggregationNames( final ContentQuery query )
    {
        final List<String> names = new ArrayList<>();
        for ( final AggregationQuery aggregation : query.getAggregationQueries() )
        {
            names.add( aggregation.getName() + ( aggregation instanceof TermsAggregationQuery terms ? ":" + terms.getSize() : "" ) );
        }
        return names;
    }

    private static Map<String, Object> normalized( final Object value )
    {
        return McpJson.readObject( McpJson.write( value ) );
    }

    private static String errorText( final Map<String, Object> result )
    {
        assertEquals( true, result.get( "isError" ) );
        return text( result );
    }

    private static MapSerializable serializable( final Map<String, Object> map )
    {
        return generator -> {
            map.forEach( ( key, value ) -> write( generator, key, value ) );
        };
    }

    private static void write( final MapGenerator generator, final String key, final Object value )
    {
        if ( value instanceof Map<?, ?> map )
        {
            if ( key == null )
            {
                generator.map();
            }
            else
            {
                generator.map( key );
            }
            map.forEach( ( k, v ) -> write( generator, (String) k, v ) );
            generator.end();
        }
        else if ( value instanceof List<?> list )
        {
            if ( key == null )
            {
                generator.array();
            }
            else
            {
                generator.array( key );
            }
            list.forEach( item -> write( generator, null, item ) );
            generator.end();
        }
        else if ( key == null )
        {
            generator.value( value );
        }
        else
        {
            generator.value( key, value );
        }
    }
}
