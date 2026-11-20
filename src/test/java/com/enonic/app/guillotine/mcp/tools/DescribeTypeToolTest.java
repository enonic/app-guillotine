package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;

import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.FormOptionSetOption;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;
import com.enonic.xp.util.GenericValue;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class DescribeTypeToolTest
    extends ArticleSchemaTestSupport
{
    private static final String PERSON = DATASET_APP + ":person";

    private static final String LANDING = DATASET_APP + ":landing";

    private static final String WIDE = DATASET_APP + ":wide";

    private final AtomicReference<String> queriedIn = new AtomicReference<>();

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();
        final ContentTypeService contentTypeService = serviceFacade.getContentTypeService();
        when( contentTypeService.getByName( any( GetContentTypeParams.class ) ) ).thenAnswer( invocation -> {
            final ContentTypeName name = ( (GetContentTypeParams) invocation.getArgument( 0 ) ).getContentTypeName();
            return contentTypeService.getAll().stream().filter( type -> type.getName().equals( name ) ).findFirst().orElse( null );
        } );
        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> {
            queriedIn.set( ContextAccessor.current().getRepositoryId() + "@" + ContextAccessor.current().getBranch() );
            return FindContentIdsByQueryResult.create().contents( ContentIds.empty() ).totalHits( 7 ).build();
        } );
    }

    @Override
    protected ResourceKey resolveScript( final ResourceKey resourceKey )
    {
        if ( ApplicationKey.from( DATASET_APP ).equals( resourceKey.getApplicationKey() ) )
        {
            return ResourceKey.from( resourceKey.getApplicationKey(), "/mcpextension" + resourceKey.getPath() );
        }
        return resourceKey;
    }

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        final List<ContentType> result = new ArrayList<>( super.getCustomContentTypes() );
        result.add( ContentType.create()
                        .superType( ContentTypeName.structured() )
                        .name( PERSON )
                        .title( "Person" )
                        .addFormItem( Input.create().name( "full-name" ).label( "Name" ).inputType( InputTypeName.TEXT_LINE ).build() )
                        .addFormItem( FormOptionSet.create()
                                          .name( "contact" )
                                          .label( "Contact" )
                                          .multiselection( Occurrences.create( 1, 1 ) )
                                          .addOptionSetOption( FormOptionSetOption.create()
                                                                   .name( "email" )
                                                                   .label( "E-mail" )
                                                                   .addFormItem( Input.create()
                                                                                     .name( "address" )
                                                                                     .label( "Address" )
                                                                                     .inputType( InputTypeName.TEXT_LINE )
                                                                                     .build() )
                                                                   .build() )
                                          .build() )
                        .build() );
        result.add( ContentType.create()
                        .superType( ContentTypeName.structured() )
                        .name( LANDING )
                        .title( "Landing" )
                        .addFormItem( Input.create().name( "body" ).label( "Body" ).inputType( InputTypeName.HTML_AREA ).build() )
                        .build() );
        result.add( ContentType.create().superType( ContentTypeName.structured() ).name( "myapplication:person" ).title( "Person" ).build() );
        final ContentType.Builder wide = ContentType.create().superType( ContentTypeName.structured() ).name( WIDE ).title( "Wide" );
        for ( int i = 0; i < 300; i++ )
        {
            wide.addFormItem( Input.create()
                                  .name( "field" + i )
                                  .label( "A long label for field number " + i )
                                  .helpText( "Help text that explains what goes into field number " + i + " of this wide type." )
                                  .inputType( InputTypeName.TEXT_LINE )
                                  .build() );
        }
        result.add( wide.build() );
        return result;
    }

    private DescribeTypeTool tool()
    {
        return new DescribeTypeTool( schemaProvider, serviceFacade.getContentTypeService(), guillotineConfigService, resolver );
    }

    private Map<String, Object> describe( final String type )
    {
        return structured( createAdminContext().callWith( () -> tool().execute( Map.of( "type", type ) ) ) );
    }

    @Test
    public void testArticleFieldsCarryIndexPathLabelHelpAndTargets()
    {
        final Map<String, Object> structured = describe( ARTICLE );

        assertEquals( "myproject", structured.get( "project" ) );
        assertEquals( "draft", structured.get( "branch" ) );
        assertEquals( ARTICLE, structured.get( "contentType" ) );
        assertEquals( "Article", structured.get( "title" ) );
        assertEquals( ARTICLE_TYPE, structured.get( "graphqlType" ) );
        assertEquals( 7, structured.get( "count" ) );
        assertEquals( "com.enonic.cms.myproject@draft", queriedIn.get() );
        assertEquals( List.of( Map.of( "name", "title", "path", "data.title", "input", "TextLine", "required", true ),
                               Map.of( "name", "author", "path", "data.author", "label", "Written by", "help",
                                       "The person who wrote the article.", "input", "ContentSelector", "targets",
                                       List.of( DATASET_APP + ":person" ) ),
                               Map.of( "name", "tags", "path", "data.tags", "input", "TextLine", "repeated", true ),
                               Map.of( "name", "related", "path", "data.related", "input", "ContentSelector", "repeated", true ),
                               Map.of( "name", "cover", "path", "data.cover", "input", "ImageSelector", "targets",
                                       List.of( "media:image", "media:vector" ) ),
                               Map.of( "name", "body", "path", "data.body", "input", "HtmlArea" ),
                               Map.of( "name", "links", "path", "data.links", "input", "ItemSet", "repeated", true ),
                               Map.of( "name", "links.label", "path", "data.links.label", "input", "TextLine" ),
                               Map.of( "name", "links.target", "path", "data.links.target", "input", "ContentSelector", "repeated",
                                       true ) ), structured.get( "fields" ) );
        assertEquals( DescribeTypeTool.INDEX_FIELDS, structured.get( "indexFields" ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "describe-type", structured ) );
    }

    @Test
    public void testSdlNamesTheContentFieldsOnceAndPrintsTheTypesGeneratedForTheType()
    {
        final String sdl = (String) describe( ARTICLE ).get( "sdl" );

        assertTrue( sdl.startsWith( "type " + ARTICLE_TYPE + " implements Content {\n  # Content fields: _id _name _path " ), sdl );
        assertFalse( sdl.contains( "\n  _path: " ), sdl );
        assertTrue( sdl.contains( "\n  data: " + ARTICLE_TYPE + "_Data\n" ), sdl );
        assertTrue( sdl.contains( "type " + ARTICLE_TYPE + "_Data {\n  title: String\n  author: Content\n" +
                                      "  tags(offset: Int, first: Int): [String]\n" ), sdl );
        assertTrue( sdl.contains( "type " + ARTICLE_TYPE + "_Links {\n  label: String\n  target(offset: Int, first: Int): [Content]\n}" ),
                    sdl );
        assertTrue( sdl.contains( "\n# RichText: " ), sdl );
    }

    @Test
    public void testOptionSetsGetTheirSelectionPathAndOptionFields()
    {
        final Map<String, Object> structured = describe( PERSON );

        assertEquals( List.of( Map.of( "name", "full_name", "path", "data.full-name", "label", "Name", "input", "TextLine" ),
                               Map.of( "name", "contact._selected", "path", "data.contact._selected", "input", "OptionSet" ),
                               Map.of( "name", "contact.email.address", "path", "data.contact.email.address", "input", "TextLine" ) ),
                      structured.get( "fields" ) );
        assertTrue( ( (String) structured.get( "sdl" ) ).contains( "enum " ), (String) structured.get( "sdl" ) );
    }

    @Test
    public void testTypeIsFoundByGraphQlNameOrUniqueLocalName()
    {
        assertEquals( ARTICLE, describe( ARTICLE_TYPE ).get( "contentType" ) );
        assertEquals( ARTICLE, describe( "Article" ).get( "contentType" ) );
    }

    @Test
    public void testUnknownTypeSuggestsSimilarNamesAndListTypes()
    {
        final ToolError error = assertThrows( ToolError.class, () -> createAdminContext().callWith(
            () -> tool().execute( Map.of( "type", "other.app:article" ) ) ) );

        assertEquals( "Unknown content type 'other.app:article'. Similar: " + ARTICLE +
                          ". Call list-types for the content types in use and pass one of their names.", error.userMessage() );
        assertThrows( ToolError.class, () -> tool().execute( Map.of( "type", " " ) ) );
    }

    @Test
    public void testAmbiguousLocalNameAsksForTheFullName()
    {
        final ToolError error = assertThrows( ToolError.class, () -> tool().execute( Map.of( "type", "person" ) ) );

        assertEquals( "Type 'person' is ambiguous: " + PERSON + ", myapplication:person. Pass the full content type name.",
                      error.userMessage() );
    }

    @Test
    public void testExamplesRunThroughTheQueryToolWithinItsLimits()
    {
        final QueryTool query = new QueryTool( schemaProvider, guillotineConfigService, resolver );
        final Parser parser = new Parser();
        for ( final String type : List.of( ARTICLE, PERSON, LANDING, WIDE, "base:folder", "media:image" ) )
        {
            @SuppressWarnings("unchecked") final List<Map<String, Object>> examples =
                (List<Map<String, Object>>) describe( type ).get( "examples" );
            assertEquals( 3, examples.size(), type );
            for ( final Map<String, Object> example : examples )
            {
                final String document = (String) example.get( "query" );
                final Document parsed = parser.parseDocument( document );
                final List<String> notes =
                    QueryLimits.check( parsed, parsed.getDefinitionsOfType( OperationDefinition.class ).get( 0 ), Map.of(),
                                       schemaProvider::getSchema );
                assertEquals( List.of(), notes, document );

                final Map<String, Object> result =
                    structured( createAdminContext().callWith( () -> query.execute( Map.of( "query", document ) ) ) );
                assertNull( result.get( "errors" ), document );
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExamplesFilterAndAggregateOnAReferenceWhenTheTypeHasOne()
    {
        final List<Map<String, Object>> article = (List<Map<String, Object>>) describe( ARTICLE ).get( "examples" );
        assertEquals( "{ guillotine { queryDslConnection(query: {term: {field: \"type\", value: {string: \"" + ARTICLE + "\"}}}, " +
                          "sort: {field: \"modifiedTime\", direction: DESC}, first: 5) { totalCount edges { node { _id _path displayName " +
                          "modifiedTime ... on " + ARTICLE_TYPE + " { data { title author { _path displayName } tags } } } } " +
                          "pageInfo { endCursor hasNext } } } }", article.get( 0 ).get( "query" ) );
        assertTrue( ( (String) article.get( 1 ).get( "query" ) ).contains( "{exists: {field: \"data.author\"}}" ) );
        assertTrue( ( (String) article.get( 1 ).get( "use" ) ).contains( "term with value {string: \"<its _id>\"} instead of exists" ) );
        assertTrue( ( (String) article.get( 2 ).get( "query" ) ).contains(
            "terms: {field: \"data.author\", order: \"_count DESC\", size: 10}" ) );

        final List<Map<String, Object>> landing = (List<Map<String, Object>>) describe( LANDING ).get( "examples" );
        assertFalse( ( (String) landing.get( 0 ).get( "query" ) ).contains( "... on" ) );
        assertTrue( ( (String) landing.get( 1 ).get( "query" ) ).contains( "{exists: {field: \"data.body\"}}" ) );
        assertTrue( ( (String) landing.get( 2 ).get( "query" ) ).contains( "terms: {field: \"_parentPath\"" ) );

        final List<Map<String, Object>> image = (List<Map<String, Object>>) describe( "media:image" ).get( "examples" );
        assertTrue( ( (String) image.get( 0 ).get( "query" ) ).contains( "{ data { altText artist copyright } }" ),
                    (String) image.get( 0 ).get( "query" ) );
        assertTrue( ( (String) image.get( 2 ).get( "query" ) ).contains( "terms: {field: \"data.artist\"" ),
                    (String) image.get( 2 ).get( "query" ) );

        final List<Map<String, Object>> folder = (List<Map<String, Object>>) describe( "base:folder" ).get( "examples" );
        assertTrue( ( (String) folder.get( 1 ).get( "query" ) ).contains( "{exists: {field: \"publish.first\"}}" ) );
    }

    @Test
    public void testFieldsAddedByAnotherApplicationAreMarked()
    {
        final Map<String, Object> structured = describe( ARTICLE );

        assertEquals( List.of( "wordCount", "guillotine.latestArticles" ), structured.get( "appFields" ) );
        assertEquals( DescribeTypeTool.APP_DEFINED_NOTE, structured.get( "appFieldsNote" ) );
        assertTrue( ( (String) structured.get( "sdl" ) ).contains( "\n  wordCount: Int" + TypeSdl.APP_DEFINED + "\n" ),
                    (String) structured.get( "sdl" ) );
        assertNull( describe( PERSON ).get( "appFields" ) );
    }

    @Test
    public void testTypicalTypeStaysCompact()
    {
        final Map<String, Object> structured = describe( ARTICLE );
        final int chars = McpJson.length( structured );

        assertTrue( chars < 3600, () -> "describe-type article is " + chars + " characters" );
    }

    @Test
    public void testWideTypeIsCutWithinTheBudget()
    {
        final Map<String, Object> structured = describe( WIDE );

        assertTrue( McpJson.length( structured ) <= guillotineConfigService.getMcpMaxResultChars() );
        assertEquals( true, structured.get( "truncated" ) );
        assertTrue( ( (String) structured.get( "sdl" ) ).endsWith( "\n# cut to 4000 characters" ) );
        assertTrue( ( (String) structured.get( "hint" ) ).endsWith(
            "Fields left out may be missing from the SDL too. For every data field name, run query with " +
                "{ __type(name: \"com_example_mcpdataset_Wide_Data\") { fields { name } } }." ), (String) structured.get( "hint" ) );
        assertTrue( ( (String) structured.get( "sdl" ) ).length() <= DescribeTypeTool.MAX_SDL_CHARS );
        assertEquals( 3, ( (List<?>) structured.get( "examples" ) ).size() );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "describe-type", structured ) );
    }

    @Test
    public void testBudgetTooSmallForAnyDescriptionIsAnErrorForTheAdministrator()
    {
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();

        final ToolError error = assertThrows( ToolError.class, () -> describe( ARTICLE ) );

        assertEquals( "The description of " + ARTICLE + " does not fit mcp.maxResultChars (1000). Do not retry; ask the administrator " +
                          "to raise mcp.maxResultChars in com.enonic.app.guillotine.cfg.", error.userMessage() );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUploadersAreNotReferencesInTheExamples()
    {
        final List<Map<String, Object>> document = (List<Map<String, Object>>) describe( "media:document" ).get( "examples" );

        assertFalse( ( (String) document.get( 1 ).get( "query" ) ).contains( "data.media" ), (String) document.get( 1 ).get( "query" ) );
        assertFalse( ( (String) document.get( 1 ).get( "use" ) ).contains( "_id" ), (String) document.get( 1 ).get( "use" ) );
    }

    @Test
    public void testSelectorTargetsAreQualifiedWithTheApplication()
    {
        final Input input = Input.create()
            .name( "pick" )
            .label( "Pick" )
            .inputType( InputTypeName.CONTENT_SELECTOR )
            .inputTypeProperty( "allowContentType", GenericValue.newList()
                .add( GenericValue.stringValue( "person" ) )
                .add( GenericValue.stringValue( "${app}:*" ) )
                .add( GenericValue.stringValue( "base:folder" ) )
                .build() )
            .build();

        assertEquals( List.of( DATASET_APP + ":person", DATASET_APP + ":*", "base:folder" ), TypeFields.targets( input, DATASET_APP ) );
    }

    @Test
    public void testSdlShrinksWithASmallBudget()
    {
        doReturn( 4000 ).when( guillotineConfigService ).getMcpMaxResultChars();

        assertTrue( ( (String) describe( ARTICLE ).get( "sdl" ) ).length() <= 1000 );
    }
}
