package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;

import static com.enonic.app.guillotine.mcp.McpResults.roundTrip;
import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static com.enonic.app.guillotine.mcp.McpResults.text;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DescribeTypeToolTest
    extends ArticleSchemaTestSupport
{
    private static final String COLLECTION = DATASET_APP + ":collection";

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        final List<ContentType> result = new ArrayList<>( super.getCustomContentTypes() );
        result.add( ContentType.create()
                        .superType( ContentTypeName.structured() )
                        .name( COLLECTION )
                        .addFormItem( input( "related", InputTypeName.CONTENT_SELECTOR ) )
                        .addFormItem( input( "gallery", InputTypeName.IMAGE_SELECTOR ) )
                        .addFormItem( input( "tags", InputTypeName.TEXT_LINE ) )
                        .addFormItem( FormItemSet.create()
                                          .name( "sections" )
                                          .label( "sections" )
                                          .occurrences( 0, 0 )
                                          .addFormItem( input( "heading", InputTypeName.TEXT_LINE ) )
                                          .addFormItem( input( "links", InputTypeName.CONTENT_SELECTOR ) )
                                          .build() )
                        .build() );
        return result;
    }

    private static Input input( final String name, final InputTypeName type )
    {
        return Input.create().name( name ).label( name ).inputType( type ).occurrences( 0, 0 ).build();
    }

    private DescribeTypeTool tool()
    {
        return new DescribeTypeTool( schemaProvider, guillotineConfigService );
    }

    @Test
    public void testDescribeContentType()
    {
        final Map<String, Object> result = tool().execute( Map.of( "type", ARTICLE ) );

        final Map<String, Object> structured = structured( roundTrip( result ) );
        assertEquals( ARTICLE, structured.get( "contentType" ) );
        assertEquals( "com_example_mcpdataset_Article", structured.get( "graphqlType" ) );
        assertEquals( "com_example_mcpdataset_Article_Data", structured.get( "dataType" ) );
        assertEquals( List.of( Map.of( "name", "title", "type", "String" ), Map.of( "name", "author", "type", "Content" ),
                               Map.of( "name", "tags", "type", "[String]" ), Map.of( "name", "cover", "type", "Content" ),
                               Map.of( "name", "body", "type", "RichText" ) ), structured.get( "dataFields" ) );

        final String sdl = (String) structured.get( "sdl" );
        assertTrue( sdl.startsWith( "#Article - com.example.mcpdataset:article\ntype com_example_mcpdataset_Article implements Content" ),
                    sdl );
        assertTrue( sdl.contains( "type com_example_mcpdataset_Article_Data {" ), sdl );
        assertTrue( sdl.contains( "type RichText {" ), sdl );
        assertTrue( sdl.length() <= DescribeTypeTool.MAX_SDL_CHARS );
        assertTrue( CastHelper.<List<String>>cast( structured.get( "sharedTypes" ) ).contains( "Content" ) );
        assertFalse( sdl.contains( "interface Content" ) );

        assertEquals( Map.of( "type", ARTICLE, "first", 10 ), structured.get( "exampleVariables" ) );
        assertTrue( ( (String) structured.get( "exampleQuery" ) ).contains( "... on com_example_mcpdataset_Article {" ) );
        assertNull( structured.get( "project" ) );
        verifyNoInteractions( resolver );
        assertTrue( text( result ).startsWith(
            "Content type com.example.mcpdataset:article is GraphQL type com_example_mcpdataset_Article; SDL of " ) );
    }

    @Test
    public void testExampleQueryRunsThroughQueryTool()
    {
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            FindContentIdsByQueryResult.create().contents( ContentIds.from( "a1" ) ).totalHits( 1 ).build() );
        when( contentService.getByIds( any() ) ).thenReturn( Contents.from( article( "a1", "First" ) ) );

        final Map<String, Object> described = structured( tool().execute( Map.of( "type", ARTICLE ) ) );
        final Map<String, Object> result = createAdminContext().callWith(
            () -> new QueryTool( schemaProvider, guillotineConfigService, resolver ).execute(
                Map.of( "query", described.get( "exampleQuery" ), "variables", described.get( "exampleVariables" ) ) ) );

        final Map<String, Object> structured = structured( roundTrip( result ) );
        assertNull( structured.get( "errors" ), () -> McpJson.write( structured.get( "errors" ) ) );
        final Map<String, Object> connection =
            CastHelper.cast( CastHelper.<Map<String, Object>>cast( CastHelper.<Map<String, Object>>cast( structured.get( "data" ) )
                                                                       .get( "guillotine" ) ).get( "queryDslConnection" ) );
        assertEquals( 1, connection.get( "totalCount" ) );
        final List<Map<String, Object>> edges = CastHelper.cast( connection.get( "edges" ) );
        final Map<String, Object> node = CastHelper.cast( edges.get( 0 ).get( "node" ) );
        final Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put( "title", "First" );
        data.put( "author", null );
        data.put( "tags", List.of( "news", "mcp" ) );
        data.put( "cover", null );
        data.put( "body", null );
        assertEquals( data, node.get( "data" ) );
    }

    @Test
    public void testShortNameWhenUnambiguous()
    {
        assertEquals( ARTICLE, structured( tool().execute( Map.of( "type", "article" ) ) ).get( "contentType" ) );
    }

    @Test
    public void testShortNameAmbiguous()
    {
        final ToolError error = assertThrows( ToolError.class, () -> tool().execute( Map.of( "type", "landing" ) ) );

        assertEquals( "Type 'landing' is ambiguous: com.example.mcpdataset:landing, myapplication:landing. Pass the full content type name.",
                      error.userMessage() );
    }

    @Test
    public void testGraphQlTypeName()
    {
        final Map<String, Object> structured = structured( tool().execute( Map.of( "type", "com_example_mcpdataset_Article_Data" ) ) );

        assertEquals( "com_example_mcpdataset_Article_Data", structured.get( "graphqlType" ) );
        assertNull( structured.get( "contentType" ) );
        assertNull( structured.get( "exampleQuery" ) );
        assertTrue( ( (String) structured.get( "sdl" ) ).startsWith( "#Article - com.example.mcpdataset:article data\n" ) );

        assertEquals( ARTICLE, structured( tool().execute( Map.of( "type", "com_example_mcpdataset_Article" ) ) ).get( "contentType" ) );
    }

    @Test
    public void testUnknownTypeSuggestsListTypes()
    {
        final ToolError error = assertThrows( ToolError.class, () -> tool().execute( Map.of( "type", "com.example.mcpdataset:artcle" ) ) );
        assertEquals( "Unknown type 'com.example.mcpdataset:artcle'. Call list-types to see the available content types, then pass one " +
                          "as \"app.key:name\".", error.userMessage() );

        final ToolError similar = assertThrows( ToolError.class, () -> tool().execute( Map.of( "type", "other:article" ) ) );
        assertEquals( "Unknown type 'other:article'. Similar content types: com.example.mcpdataset:article. Call list-types to see the " +
                          "available content types, then pass one as \"app.key:name\".", similar.userMessage() );

        assertThrows( ToolError.class, () -> tool().execute( Map.of( "type", "__Schema" ) ) );
        assertEquals( "type is required. Pass a content type name from list-types, such as \"app.key:article\".",
                      assertThrows( ToolError.class, () -> tool().execute( Map.of() ) ).userMessage() );
    }

    @Test
    public void testProjectAndBranchAreNeitherResolvedNorEchoed()
    {
        final Map<String, Object> structured =
            structured( tool().execute( Map.of( "type", ARTICLE, "project", "myproject", "branch", "draft" ) ) );

        assertFalse( structured.containsKey( "project" ) );
        assertFalse( structured.containsKey( "branch" ) );
        verifyNoInteractions( resolver );
    }

    @Test
    public void testSdlIsLimitedBySmallBudget()
    {
        final String fullSdl = (String) structured( tool().execute( Map.of( "type", ARTICLE ) ) ).get( "sdl" );
        when( guillotineConfigService.getMcpMaxResultChars() ).thenReturn( 1000 );

        final Map<String, Object> result = tool().execute( Map.of( "type", ARTICLE ) );

        assertTrue( McpJson.length( structured( result ) ) <= 1000, () -> "result is " + McpJson.length( structured( result ) ) + " chars" );
        final Map<String, Object> structured = structured( roundTrip( result ) );
        assertEquals( true, structured.get( "truncated" ) );
        final String hint = (String) structured.get( "hint" );
        assertTrue( hint.startsWith( "Result truncated to fit 1000 characters" ), hint );
        assertTrue( hint.endsWith( ". Call describe-type on the nested types separately." ), hint );
        final List<String> omitted = CastHelper.cast( structured.get( "omittedTypes" ) );
        assertTrue( omitted.contains( "RichText" ), () -> String.valueOf( omitted ) );
        final Object sdl = structured.get( "sdl" );
        assertTrue( sdl instanceof Map<?, ?> marker ? Boolean.TRUE.equals( marker.get( "truncated" ) )
                        : ( (String) sdl ).length() < fullSdl.length() && !( (String) sdl ).contains( "type RichText {" ),
                    () -> String.valueOf( sdl ) );
    }

    @Test
    public void testEveryExampleQueryIsWithinTheQueryLimits()
    {
        final GraphQLSchema schema = schemaProvider.getSchema();
        final List<String> checked = new ArrayList<>();
        for ( final GraphQLObjectType type : schema.getImplementations( (GraphQLInterfaceType) schema.getType( "Content" ) ) )
        {
            final Map<String, Object> structured = structured( roundTrip( tool().execute( Map.of( "type", type.getName() ) ) ) );
            if ( !( structured.get( "exampleQuery" ) instanceof String query ) )
            {
                continue;
            }
            final Document document = new Parser().parseDocument( query );
            assertDoesNotThrow( () -> QueryLimits.check( document, document.getDefinitionsOfType( OperationDefinition.class ).get( 0 ),
                                                         CastHelper.cast( structured.get( "exampleVariables" ) ),
                                                         guillotineConfigService.getMcpMaxComplexity(), false, () -> schema ),
                                query );
            checked.add( String.valueOf( structured.get( "contentType" ) ) );
        }
        assertTrue( checked.containsAll( List.of( ARTICLE, COLLECTION ) ), checked::toString );

        final String collection = (String) structured( tool().execute( Map.of( "type", COLLECTION ) ) ).get( "exampleQuery" );
        assertTrue( collection.contains( "related(first: 3) {" ), collection );
        assertTrue( collection.contains( "sections(first: 3) {" ), collection );
        assertFalse( collection.contains( "tags(" ), collection );
        final int first = (Integer) ( (Map<?, ?>) structured( tool().execute( Map.of( "type", COLLECTION ) ) ).get( "exampleVariables" ) )
            .get( "first" );
        assertTrue( first >= 1 && first < 10, String.valueOf( first ) );
    }

    @Test
    public void testLargeTypeIsCutToTheSdlLimit()
    {
        when( guillotineConfigService.getMcpMaxResultChars() ).thenReturn( 1000 );

        final String sdl = (String) structured( roundTrip( tool().execute( Map.of( "type", "HeadlessCms" ) ) ) ).get( "sdl" );

        assertTrue( sdl.length() <= 500, () -> sdl.length() + " chars:\n" + sdl );
        assertTrue( sdl.startsWith( "type HeadlessCms {" ) || sdl.contains( "\ntype HeadlessCms {" ), sdl );
        assertTrue( sdl.endsWith( "\n  # more fields left out to stay within 500 characters\n}" ), sdl );
    }

    @Test
    public void testCutKeepsTheTypeHeaderEvenWhenTheLimitIsSmaller()
    {
        final String sdl =
            DescribeTypeTool.sdl( (GraphQLNamedType) schemaProvider.getSchema().getType( "HeadlessCms" ), Set.of(), 20, 2 ).text();

        assertTrue( sdl.endsWith( "type HeadlessCms {\n  # more fields left out to stay within 20 characters\n}" ), sdl );
    }

    @Test
    public void testMeasureArticleDescription()
    {
        final Map<String, Object> result = tool().execute( Map.of( "type", ARTICLE ) );
        final Map<String, Object> structured = structured( result );

        System.out.println( "P6 measure (offline): describe-type " + ARTICLE + " full result chars=" + McpJson.write( result ).length() +
                                ", structuredContent chars=" + McpJson.write( structured ).length() + ", sdl chars=" +
                                ( (String) structured.get( "sdl" ) ).length() + ", exampleQuery chars=" +
                                ( (String) structured.get( "exampleQuery" ) ).length() );
        System.out.println( "P6 sdl:\n" + structured.get( "sdl" ) );
        System.out.println( "P6 exampleQuery:\n" + structured.get( "exampleQuery" ) );
        System.out.println( "P6 omittedTypes=" + structured.get( "omittedTypes" ) + " sharedTypes=" + structured.get( "sharedTypes" ) );

        assertNull( structured.get( "truncated" ) );
        assertTrue( McpJson.length( result ) < 8000, () -> "describe-type result is " + McpJson.length( result ) + " chars" );
        assertTrue( McpJson.length( structured ) < 2200, () -> "structuredContent is " + McpJson.length( structured ) + " chars" );
        assertTrue( ( (String) structured.get( "sdl" ) ).length() < 800 );
        assertEquals( List.of( "Macro", "Image", "Link" ), structured.get( "omittedTypes" ) );
        assertEquals( List.of( "Content", "JSON" ), structured.get( "sharedTypes" ) );
    }
}
