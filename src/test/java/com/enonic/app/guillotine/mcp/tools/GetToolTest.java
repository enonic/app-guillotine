package com.enonic.app.guillotine.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

public class GetToolTest
    extends ContentStoreTestSupport
{
    private static final String ARTICLE_ID = uuid( 100 );

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();
        put( uuid( 1 ), "/persons", "ada-lovelace", PERSON, null, false );
        final PropertyTree data = new PropertyTree();
        data.addString( "title", "Hello" );
        data.addString( "author", uuid( 1 ) );
        data.addString( "body", "<h1>Hi &amp; welcome</h1><p>See <a href=\"content://" + uuid( 1 ) + "\">Ada</a> and " +
            "<a href=\"content://" + uuid( 99 ) + "\">gone</a> or <a href=\"https://example.com\">site</a>.</p>" +
            "<figure><img alt=\"pic\" src=\"image://" + uuid( 1 ) + "?scale=block(768,432)\"></figure>" );
        final PropertySet link = data.addSet( "links" );
        link.addString( "label", "see" );
        link.addString( "target", uuid( 99 ) );
        put( ARTICLE_ID, "/articles", "article-01", ARTICLE, data, true );
    }

    private GetTool tool()
    {
        return new GetTool( schemaProvider, contentService, serviceFacade.getContentTypeService(), guillotineConfigService, resolver );
    }

    private Map<String, Object> run( final Map<String, Object> args )
    {
        final Map<String, Object> structured = structured( as( ALICE, () -> tool().execute( args ) ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "get", structured ) );
        return structured;
    }

    private String invalid( final Map<String, Object> args )
    {
        return assertThrows( ToolError.class, () -> as( ALICE, () -> tool().execute( args ) ) ).userMessage();
    }

    @Test
    public void testItemByPathWithRichTextAsTextAndLinksAndReferencesLabelled()
    {
        final Map<String, Object> structured = run( args( "key", "/articles/article-01/" ) );

        final Map<String, Object> ada = Map.of( "displayName", "Ada lovelace", "_path", "/persons/ada-lovelace" );
        assertEquals( Map.of( "title", "Hello", "author", Map.of( "_id", uuid( 1 ), "displayName", "Ada lovelace", "_path",
                                                                  "/persons/ada-lovelace" ), "body",
                              Map.of( "text", "Hi & welcome\nSee Ada and gone or site.", "links",
                                      List.of( merge( Map.of( "type", "content", "id", uuid( 1 ), "text", "Ada" ), ada ),
                                               Map.of( "type", "content", "id", uuid( 99 ), "text", "gone", "missing", true ),
                                               Map.of( "type", "external", "url", "https://example.com", "text", "site" ),
                                               merge( Map.of( "type", "image", "id", uuid( 1 ), "text", "pic" ), ada ) ) ), "links",
                              Map.of( "label", "see", "target", Map.of( "_id", uuid( 99 ), "missing", true ) ) ),
                      structured.get( "data" ) );
        assertEquals( ARTICLE_ID, structured.get( "_id" ) );
        assertEquals( "/articles/article-01", structured.get( "_path" ) );
        assertEquals( ARTICLE, structured.get( "type" ) );
        assertEquals( "IN_PROGRESS", structured.get( "workflow" ) );
        contexts.forEach( SearchToolTest::assertCallerContext );
    }

    @Test
    public void testFieldsSelectDataFieldsByAnyOfTheirNames()
    {
        final Map<String, Object> structured = run( args( "key", ARTICLE_ID, "fields", List.of( "data.title", "links.target", "tags" ) ) );

        assertEquals( Map.of( "title", "Hello", "links.target", Map.of( "_id", uuid( 99 ), "missing", true ) ), structured.get( "data" ) );
        assertEquals( Map.of( "links", Map.of( "label", "see", "target", Map.of( "_id", uuid( 99 ), "missing", true ) ), "body",
                              Map.of( "text", "Hi & welcome\nSee Ada and gone or site.", "links", ( (Map<?, ?>) ( (Map<?, ?>) run(
                                  args( "key", ARTICLE_ID ) ).get( "data" ) ).get( "body" ) ).get( "links" ) ) ),
                      run( args( "key", ARTICLE_ID, "fields", List.of( "links", "body" ) ) ).get( "data" ) );
        assertTrue( invalid( args( "key", ARTICLE_ID, "fields", List.of( "nope" ) ) ).startsWith(
            "Invalid fields: \"nope\" is not a data field of " + ARTICLE + ". Data fields: data.title," ) );
    }

    @Test
    public void testUnknownOrInvalidKeySaysHowToFindContent()
    {
        assertEquals( "No content at \"/articles/nope\" in project myproject on branch draft that the caller can read. Pass an _id or " +
                          "_path from search, such as /articles/article-01; master holds published content only.",
                      invalid( args( "key", "/articles/nope" ) ) );
        assertTrue( invalid( args( "key", "articles/nope" ) ).startsWith( "Invalid key: \"articles/nope\" looks like a relative path." ) );
        assertTrue( invalid( args( "key", "/articles/../x" ) ).startsWith( "Invalid key: \"/articles/../x\" is not a content path." ) );
    }

    @Test
    public void testLargeDataIsCutWithinBudgetKeepingTheRest()
    {
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        final PropertyTree data = new PropertyTree();
        data.addString( "title", "Short" );
        data.addString( "body", "<p>" + "x".repeat( 5000 ) + "</p>" );
        put( uuid( 101 ), "/articles", "article-02", ARTICLE, data, false );

        final Map<String, Object> structured = run( args( "key", uuid( 101 ) ) );

        assertTrue( McpJson.length( structured ) <= 1000 );
        assertEquals( true, structured.get( "truncated" ) );
        assertEquals( "Short", ( (Map<?, ?>) structured.get( "data" ) ).get( "title" ) );
        assertTrue( ( (String) structured.get( "hint" ) ).endsWith( "Pass fields with the data fields to read; one field alone gets the " +
                                                                     "whole budget." ), (String) structured.get( "hint" ) );
    }

    private static Map<String, Object> merge( final Map<String, Object> left, final Map<String, Object> right )
    {
        final Map<String, Object> result = new LinkedHashMap<>( left );
        result.putAll( right );
        return result;
    }
}
