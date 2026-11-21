package com.enonic.app.guillotine.mcp.tools;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.enonic.app.guillotine.mcp.McpGroup;
import com.enonic.app.guillotine.mcp.ToolDescriptor;
import com.enonic.app.guillotine.mcp.shape.Cursors;
import com.enonic.app.guillotine.mcp.shape.McpJson;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ToolError;
import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.aggregation.Buckets;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentPublishInfo;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.context.Context;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static com.enonic.app.guillotine.mcp.McpResults.structured;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class SearchToolTest
    extends ContentStoreTestSupport
{
    private static final String NOTE = "myapplication:note";

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        final List<ContentType> result = new ArrayList<>( super.getCustomContentTypes() );
        result.add( 0, ContentType.create()
            .superType( ContentTypeName.structured() )
            .name( NOTE )
            .title( "Note" )
            .addFormItem( Input.create().name( "author" ).label( "Author" ).inputType( InputTypeName.TEXT_LINE ).build() )
            .addFormItem( Input.create().name( "language" ).label( "Language" ).inputType( InputTypeName.TEXT_LINE ).build() )
            .addFormItem( Input.create().name( "owner" ).label( "Owner" ).inputType( InputTypeName.TEXT_LINE ).build() )
            .build() );
        return result;
    }

    private SearchTool tool()
    {
        return new SearchTool( schemaProvider, contentService, serviceFacade.getContentTypeService(), guillotineConfigService, resolver );
    }

    private Map<String, Object> run( final Map<String, Object> args )
    {
        final Map<String, Object> structured = structured( as( ALICE, () -> tool().execute( args ) ) );
        assertEquals( List.of(), ToolDescriptor.outputSchemaErrors( McpGroup.CONTENT, "search", structured ) );
        return structured;
    }

    private String invalid( final Map<String, Object> args )
    {
        return assertThrows( ToolError.class, () -> as( ALICE, () -> tool().execute( args ) ) ).userMessage();
    }

    private void persons()
    {
        put( uuid( 1 ), "/persons", "ada-lovelace", PERSON, null, false );
        put( uuid( 2 ), "/persons", "alan-turing", PERSON, null, false );
    }

    private void articles( final int count )
    {
        for ( int i = 0; i < count; i++ )
        {
            final PropertyTree data = new PropertyTree();
            data.addString( "title", "Title " + i );
            data.addString( "author", uuid( 1 + i % 2 ) );
            final PropertySet link = data.addSet( "links" );
            link.addString( "label", "see" );
            link.addString( "target", uuid( 2 ) );
            link.addString( "target", uuid( 99 ) );
            data.addString( "body", "<p>Body " + i + "</p>" );
            put( uuid( 100 + i ), "/articles", "article-" + i, ARTICLE, data, false );
            searchable.add( uuid( 100 + i ) );
        }
    }

    @Test
    public void testArgumentsBecomeQueryDslVariables()
    {
        persons();
        articles( 3 );

        final Map<String, Object> structured = run(
            args( "text", "spring news", "types", List.of( "article", "base:folder" ), "path", "/articles/", "filters",
                  List.of( args( "field", "modifiedTime", "from", "2026-01-01" ), args( "field", "language", "equals", "en" ),
                           args( "field", "title", "equals", "Title 1" ), args( "field", "cover", "exists", false ),
                           args( "field", "data.tags", "exists", true ),
                           args( "field", "author", "references", "/persons/ada-lovelace" ), args( "references", uuid( 2 ) ) ),
                  "sort", "displayName desc, title", "aggregate", List.of( args( "by", "author", "size", 5 ),
                                                                         args( "by", "modifiedTime" ) ), "count",
                  2 ) );

        assertEquals( 3, structured.get( "total" ) );
        final ContentQuery query = searches.get( 0 );
        assertEquals( 0, query.getFrom() );
        assertEquals( 2, query.getSize() );
        final List<Map<String, Object>> must = List.of(
            Map.of( "fulltext",
                    Map.of( "fields", List.of( "displayName", "_name", "_allText" ), "query", "spring news", "operator", "AND" ) ),
            Map.of( "boolean", Map.of( "should", List.of( Map.of( "in", Map.of( "field", "type", "values", List.of( "base:folder" ) ) ),
                                                          Map.of( "like", Map.of( "field", "type", "value", "*:article" ) ) ) ) ),
            Map.of( "pathMatch", Map.of( "field", "_path", "path", "/content/articles", "minimumMatch", 2 ) ),
            Map.of( "range", Map.of( "field", "modifiedTime", "gte", "2026-01-01T00:00Z", "type", "dateTime" ) ),
            Map.of( "term", Map.of( "field", "language", "value", "en" ) ),
            Map.of( "term", Map.of( "field", "data.title", "value", "Title 1" ) ),
            Map.of( "exists", Map.of( "field", "data.tags" ) ),
            Map.of( "term", Map.of( "field", "data.author", "value", uuid( 1 ) ) ),
            Map.of( "term", Map.of( "field", "_references", "value", uuid( 2 ) ) ) );
        final List<Map<String, Object>> mustNot = List.of( Map.of( "exists", Map.of( "field", "data.cover" ) ) );
        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", must, "mustNot", mustNot ) ) ), dsl( query ) );
        assertEquals( List.of( "displayName DESC", "data.title ASC", "_path ASC" ), sort( query ) );
        assertEquals( Set.of( "a0", "a1" ), aggregationNames( query ) );
        assertCallerContext( searchContext.get() );
    }

    @Test
    public void testPathIsTheItemAndEverythingBelow()
    {
        folders( 1 );

        run( args( "path", "/folders" ) );

        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of(
            Map.of( "pathMatch", Map.of( "field", "_path", "path", "/content/folders", "minimumMatch", 2 ) ) ) ) ) ), dsl( searches.get( 0 ) ) );
        assertTrue( invalid( args( "path", "folders/*" ) ).startsWith( "Invalid path: \"folders/*\" is not a content path." ) );
    }

    @Test
    public void testMineIsTheCallerAsModifierNewestFirstInTheCallerContext()
    {
        folders( 1 );

        run( args( "mine", true ) );

        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of(
            Map.of( "term", Map.of( "field", "modifier", "value", "user:system:alice" ) ) ) ) ) ), dsl( searches.get( 0 ) ) );
        assertEquals( List.of( "modifiedTime DESC", "_path ASC" ), sort( searches.get( 0 ) ) );
        assertCallerContext( searchContext.get() );
    }

    @Test
    public void testMineNeedsASignedInCaller()
    {
        final ToolError error = assertThrows( ToolError.class, () -> as( AuthenticationInfo.unAuthenticated(),
                                                                         () -> tool().execute( args( "mine", true ) ) ) );

        assertEquals( "mine needs a signed-in caller, and this call is anonymous. Ask the user to sign in to Enonic XP, or omit mine.",
                      error.userMessage() );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testAnonymousCallersCannotFilterSortOrAggregateOnPrincipalFields()
    {
        final String refusal = " holds user keys, and this call is anonymous; filters, sorts and aggregations on creator, modifier " +
            "and owner need a signed-in caller. Use other fields (data.<name> for a data field of that name), or ask the user to sign in to Enonic XP.";
        final Map<Map<String, Object>, String> refused = new LinkedHashMap<>();
        refused.put( args( "filters", List.of( args( "field", "modifier", "equals", "user:system:su" ) ) ),
                     "Invalid filters[0].field: \"modifier\"" );
        refused.put( args( "filters", List.of( args( "field", "creator._orderby", "exists", true ) ) ),
                     "Invalid filters[0].field: \"creator._orderby\"" );
        refused.put( args( "filters", List.of( args( "references", "/folders", "field", "Owner" ) ) ),
                     "Invalid filters[0].field: \"Owner\"" );
        refused.put( args( "sort", "displayName, creator DESC" ), "Invalid sort: \"creator\"" );
        refused.put( args( "aggregate", List.of( args( "by", "\u0001modifier" ) ), "count", 0 ),
                     "Invalid aggregate[0].by: \"\\u0001modifier\"" );
        refused.put( args( "filters", List.of( args( "field", "\u001fowner", "exists", true ) ) ),
                     "Invalid filters[0].field: \"owner\"" );
        refused.put( args( "aggregate", List.of( args( "by", "modifier" ) ), "count", 0 ), "Invalid aggregate[0].by: \"modifier\"" );
        refused.put( args( "types", List.of( NOTE ), "aggregate", List.of( args( "by", "owner" ) ), "count", 0 ),
                     "Invalid aggregate[0].by: \"owner\"" );
        refused.forEach( ( arguments, message ) -> assertEquals(
            message + refusal, assertThrows( ToolError.class, () -> as( AuthenticationInfo.unAuthenticated(),
                                                                        () -> tool().execute( arguments ) ) ).userMessage(),
            arguments::toString ) );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testPrincipalFieldsWorkForSignedInCallersAndDataFieldsNamedLikeThemForAnyone()
    {
        run( args( "filters", List.of( args( "field", "modifier", "equals", "user:system:su" ) ), "sort", "owner DESC",
                   "aggregate", List.of( args( "by", "creator" ) ), "count", 0 ) );
        as( AuthenticationInfo.unAuthenticated(), () -> tool().execute(
            args( "types", List.of( NOTE ), "filters", List.of( args( "field", "data.owner", "equals", "modifier" ) ), "sort",
                  "data.owner", "aggregate", List.of( args( "by", "data.owner" ) ), "count", 0 ) ) );

        assertEquals( 2, searches.size() );
        assertEquals( List.of( "owner DESC", "_path ASC" ), sort( searches.get( 0 ) ) );
        assertEquals( List.of( "data.owner ASC", "_path ASC" ), sort( searches.get( 1 ) ) );
    }

    @Test
    public void testWorkflowFiltersAndHitsShowStatesOtherThanReady()
    {
        folders( 2 );

        final List<Map<String, Object>> hits = hits( run( args( "workflow", "IN_PROGRESS" ) ) );
        run( args( "workflow", "READY" ) );

        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of(
            Map.of( "term", Map.of( "field", "workflow.state", "value", "IN_PROGRESS" ) ) ) ) ) ), dsl( searches.get( 0 ) ) );
        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of( Map.of( "matchAll", Map.of() ) ), "mustNot", List.of(
            Map.of( "in", Map.of( "field", "workflow.state", "values", List.of( "IN_PROGRESS", "PENDING_APPROVAL", "REJECTED" ) ) ) ) ) ) ),
                      dsl( searches.get( 1 ) ) );
        assertEquals( Map.of( "_id", uuid( 500 ), "_path", "/folders/folder-0", "displayName", "Folder 0", "type", "base:folder",
                              "modifiedTime", "2026-01-01T00:00:00.000Z", "workflow", "IN_PROGRESS" ), hits.get( 0 ) );
        assertFalse( hits.get( 1 ).containsKey( "workflow" ) );
    }

    @Test
    public void testPagingWalksToTheEnd()
    {
        folders( 23 );

        final Set<Object> ids = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do
        {
            final Map<String, Object> args = args( "count", 7 );
            if ( cursor != null )
            {
                args.put( "cursor", cursor );
            }
            final Map<String, Object> structured = run( args );
            hits( structured ).forEach( hit -> ids.add( hit.get( "_id" ) ) );
            cursor = (String) structured.get( "nextCursor" );
            pages++;
        }
        while ( cursor != null && pages < 10 );

        assertEquals( 4, pages );
        assertEquals( 23, ids.size() );
        assertEquals( List.of( 0, 7, 14, 21 ), searches.stream().map( ContentQuery::getFrom ).toList() );
    }

    @Test
    public void testLargePageIsCutWithinBudgetAndTheCursorContinuesAfterTheLastHit()
    {
        doReturn( 1000 ).when( guillotineConfigService ).getMcpMaxResultChars();
        folders( 30 );

        final Map<String, Object> structured = run( args( "count", 30 ) );

        assertTrue( McpJson.length( structured ) <= 1000, () -> "length " + McpJson.length( structured ) );
        assertEquals( true, structured.get( "truncated" ) );
        final int returned = hits( structured ).size();
        assertTrue( returned > 0 && returned < 30, () -> "returned " + returned );
        run( args( "count", 30, "cursor", structured.get( "nextCursor" ) ) );
        assertEquals( returned, searches.get( 1 ).getFrom() );
    }

    @Test
    public void testTermsBucketsOfContentIdsCarryDisplayNameAndPathLargestFirst()
    {
        persons();
        articles( 1 );
        aggregations = Aggregations.create()
            .add( BucketAggregation.bucketAggregation( "a0" )
                      .buckets( Buckets.create()
                                    .add( Bucket.create().key( "not-an-id" ).docCount( 1 ).build() )
                                    .add( Bucket.create().key( uuid( 98 ) ).docCount( 2 ).build() )
                                    .add( Bucket.create().key( uuid( 1 ) ).docCount( 12 ).build() )
                                    .build() )
                      .build() )
            .build();

        final Map<String, Object> structured =
            run( args( "types", List.of( ARTICLE ), "aggregate", List.of( args( "by", "data.author" ) ), "count", 0 ) );

        assertEquals( List.of(), structured.get( "hits" ) );
        assertNull( structured.get( "nextCursor" ) );
        assertEquals( 0, searches.get( 0 ).getSize() );
        assertEquals( List.of( Map.of( "by", "data.author", "buckets", List.of(
                          Map.of( "key", uuid( 1 ), "count", 12, "displayName", "Ada lovelace", "_path", "/persons/ada-lovelace" ),
                          Map.of( "key", uuid( 98 ), "count", 2, "missing", true ), Map.of( "key", "not-an-id", "count", 1, "missing", true ) ) ) ),
                      structured.get( "aggregations" ) );
        contexts.forEach( SearchToolTest::assertCallerContext );
    }

    @Test
    public void testBucketsOfAReferenceFieldAreLabelledWhateverTheIdLooksLike()
    {
        folders( 1 );
        put( "imported-ada", "/persons", "ada-lovelace", PERSON, null, false );
        aggregations = Aggregations.create()
            .add( BucketAggregation.bucketAggregation( "a0" )
                      .buckets( Buckets.create().add( Bucket.create().key( "imported-ada" ).docCount( 3 ).build() ).build() )
                      .build() )
            .add( BucketAggregation.bucketAggregation( "a1" )
                      .buckets( Buckets.create().add( Bucket.create().key( "imported-ada" ).docCount( 3 ).build() ).build() )
                      .build() )
            .build();

        final List<?> result = (List<?>) run( args( "types", List.of( ARTICLE ), "aggregate",
                                                    List.of( args( "by", "author" ), args( "by", "data.title" ) ), "count", 0 ) ).get(
            "aggregations" );

        assertEquals( List.of( Map.of( "by", "data.author", "buckets", List.of(
                          Map.of( "key", "imported-ada", "count", 3, "displayName", "Ada lovelace", "_path", "/persons/ada-lovelace" ) ) ),
                               Map.of( "by", "data.title", "buckets", List.of( Map.of( "key", "imported-ada", "count", 3 ) ) ) ),
                      result );
    }

    @Test
    public void testLabelsAreLookedUpForAHundredIdsAndTheRestIsNoted()
    {
        folders( 1 );
        final Buckets.Builder buckets = Buckets.create();
        for ( int i = 0; i < ContentLookup.MAX_LABELS + 5; i++ )
        {
            buckets.add( Bucket.create().key( uuid( 1000 + i ) ).docCount( 1 ).build() );
        }
        aggregations = Aggregations.create().add( BucketAggregation.bucketAggregation( "a0" ).buckets( buckets.build() ).build() ).build();

        final Map<String, Object> structured = run( args( "aggregate", List.of( args( "by", "_references", "size", 100 ) ), "count", 0 ) );

        assertEquals( "Display names were looked up for the first 100 content ids only.", structured.get( "note" ) );
        final List<?> result = (List<?>) ( (Map<?, ?>) ( (List<?>) structured.get( "aggregations" ) ).get( 0 ) ).get( "buckets" );
        assertEquals( Map.of( "key", uuid( 1104 ), "count", 1 ), result.get( 104 ) );
    }

    @Test
    public void testFieldsReturnDataWithReferencesLabelled()
    {
        persons();
        articles( 2 );

        final List<Map<String, Object>> hits =
            hits( run( args( "types", List.of( "article" ), "fields", List.of( "title", "data.author", "links.target", "cover" ) ) ) );

        final Map<String, Object> alan = Map.of( "_id", uuid( 2 ), "displayName", "Alan turing", "_path", "/persons/alan-turing" );
        assertEquals( Map.of( "title", "Title 0", "author",
                              Map.of( "_id", uuid( 1 ), "displayName", "Ada lovelace", "_path", "/persons/ada-lovelace" ),
                              "links.target", List.of( alan, Map.of( "_id", uuid( 99 ), "missing", true ) ) ), hits.get( 0 ).get( "data" ) );
        assertEquals( alan, ( (Map<?, ?>) hits.get( 1 ).get( "data" ) ).get( "author" ) );
    }

    @Test
    public void testAFieldNameSharedByTypesIsAReferenceOnlyWhereItsTypeSaysSo()
    {
        persons();
        articles( 1 );
        final PropertyTree data = new PropertyTree();
        data.addString( "author", "John Smith" );
        put( uuid( 300 ), "/notes", "note-1", NOTE, data, false );
        searchable.add( uuid( 300 ) );

        final List<Map<String, Object>> hits = hits( run( args( "fields", List.of( "author" ) ) ) );
        run( args( "filters", List.of( args( "field", "author", "references", "/persons/ada-lovelace" ) ) ) );

        assertEquals( Map.of( "author", Map.of( "_id", uuid( 1 ), "displayName", "Ada lovelace", "_path", "/persons/ada-lovelace" ) ),
                      hits.get( 0 ).get( "data" ) );
        assertEquals( Map.of( "author", "John Smith" ), hits.get( 1 ).get( "data" ) );
        assertTrue( invalid( args( "types", List.of( NOTE ), "filters", List.of( args( "field", "author", "references", uuid( 1 ) ) ) ) )
                        .startsWith( "Invalid filters[0].field: data.author does not hold references." ) );
        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of( Map.of( "boolean", Map.of( "must", List.of(
                          Map.of( "term", Map.of( "field", "data.author", "value", uuid( 1 ) ) ),
                          Map.of( "in", Map.of( "field", "type", "values", List.of( ARTICLE ) ) ) ) ) ) ) ) ) ),
                      dsl( searches.get( 1 ) ) );
    }

    @Test
    public void testBucketsOfAFieldThatIsPlainTextInSomeTypeAreLabelledOnlyWhenTheyLookLikeIds()
    {
        persons();
        aggregations = Aggregations.create()
            .add( BucketAggregation.bucketAggregation( "a0" )
                      .buckets( Buckets.create()
                                    .add( Bucket.create().key( "John Smith" ).docCount( 2 ).build() )
                                    .add( Bucket.create().key( uuid( 1 ) ).docCount( 1 ).build() )
                                    .build() )
                      .build() )
            .build();

        final Map<?, ?> aggregation =
            (Map<?, ?>) ( (List<?>) run( args( "aggregate", List.of( args( "by", "author" ) ), "count", 0 ) ).get( "aggregations" ) ).get( 0 );

        assertEquals( List.of( Map.of( "key", "John Smith", "count", 2 ),
                               Map.of( "key", uuid( 1 ), "count", 1, "displayName", "Ada lovelace", "_path", "/persons/ada-lovelace" ) ),
                      aggregation.get( "buckets" ) );
    }

    @Test
    public void testDataFieldsOfAnInstalledTypeWithoutFieldsAreCheckedButUnknownTypesPassThrough()
    {
        folders( 1 );

        assertTrue( invalid( args( "types", List.of( "base:folder" ), "filters", List.of( args( "field", "data.titel", "exists", false ) ) ) )
                        .startsWith( "Invalid filters[0].field: \"data.titel\" is not a data field of base:folder. These types have no " +
                                         "data fields" ) );
        run( args( "types", List.of( "com.example.gone:thing" ), "filters", List.of( args( "field", "data.titel", "exists", true ) ) ) );
        assertTrue( invalid( args( "types", List.of( "com.example.gone:thing" ), "fields", List.of( "titel" ) ) ).contains(
            "No installed content type matches types" ) );
    }

    @Test
    public void testPagingBeyondTheWindowIsNoted()
    {
        for ( int i = 0; i < Cursors.MAX_WINDOW + 5; i++ )
        {
            searchable.add( uuid( 20000 + i ) );
        }

        assertEquals( "Paging stops at the first 10000 of 10005 matches; narrow the search to reach the rest.",
                      run( args( "count", 1 ) ).get( "note" ) );
    }

    @Test
    public void testASetInFieldsHasItsReferencesLabelled()
    {
        persons();
        articles( 1 );

        final Map<?, ?> data = (Map<?, ?>) hits( run( args( "types", List.of( ARTICLE ), "fields", List.of( "links" ) ) ) ).get( 0 )
            .get( "data" );

        assertEquals( Map.of( "links", Map.of( "label", "see", "target", List.of(
            Map.of( "_id", uuid( 2 ), "displayName", "Alan turing", "_path", "/persons/alan-turing" ),
            Map.of( "_id", uuid( 99 ), "missing", true ) ) ) ), data );
    }

    @Test
    public void testEqualsOnADateFieldIsThatDay()
    {
        folders( 1 );

        run( args( "filters", List.of( args( "field", "modifiedTime", "equals", "2026-01-31" ) ) ) );

        assertEquals( unwrap( Map.of( "boolean", Map.of( "must", List.of( Map.of( "range", Map.of( "field", "modifiedTime", "gte",
                                                                                                  "2026-01-31T00:00Z", "lt",
                                                                                                  "2026-02-01T00:00Z", "type",
                                                                                                  "dateTime" ) ) ) ) ) ),
                      dsl( searches.get( 0 ) ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "publish.from", "equals", "2026-01-31T12:00:00Z" ) ) ) ).startsWith(
            "Invalid filters[0].equals: publish.from is a date field; equals takes a day such as 2026-01-31." ) );
    }

    @Test
    public void testUnknownDataFieldNamesTheValidOnes()
    {
        final String message = invalid( args( "types", List.of( ARTICLE ), "fields", List.of( "titel" ) ) );

        assertEquals( "Invalid fields: \"titel\" is not a data field of " + ARTICLE + ". Data fields: data.title, data.author, " +
                          "data.tags, data.related, data.cover, data.body, data.links, data.links.label, data.links.target. Call " +
                          "describe-type for the fields of one type.", message );
        assertTrue( invalid( args( "filters", List.of( args( "field", "data.titel", "equals", "x" ) ) ) ).startsWith(
            "Invalid filters[0].field: \"data.titel\" is not a data field of any content type." ) );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testIndexFieldsPassThroughAndBareDataNamesResolve()
    {
        folders( 1 );

        run( args( "filters", List.of( args( "field", "x.com-example.seo.title", "equals", "a" ), args( "field", "body", "equals", "b" ) ),
                   "sort", "publish.from" ) );

        final List<?> must = (List<?>) ( (Map<?, ?>) dsl( searches.get( 0 ) ).get( "boolean" ) ).get( "must" );
        assertEquals( unwrap( List.of( Map.of( "term", Map.of( "field", "x.com-example.seo.title", "value", "a" ) ),
                                       Map.of( "term", Map.of( "field", "data.body", "value", "b" ) ) ) ), must );
        assertEquals( List.of( "publish.from ASC", "_path ASC" ), sort( searches.get( 0 ) ) );
    }

    @Test
    public void testReferencesFilterRejectsAMissingTargetAndAFieldThatHoldsNoReferences()
    {
        assertEquals( "Invalid filters[0].references: no content at \"/persons/nobody\" that the caller can read. Find the item with " +
                          "search and pass its _path or _id.",
                      invalid( args( "filters", List.of( args( "references", "/persons/nobody" ) ) ) ) );
        assertTrue( invalid( args( "types", List.of( ARTICLE ), "filters", List.of( args( "field", "title", "references", uuid( 1 ) ) ) ) )
                        .startsWith( "Invalid filters[0].field: data.title does not hold references." ) );
        assertTrue( searches.isEmpty() );
    }

    @Test
    public void testFilterTakesExactlyOneOperatorAndChecksDates()
    {
        assertTrue( invalid( args( "filters", List.of( args( "field", "title", "equals", "a", "exists", true ) ) ) ).startsWith(
            "Invalid filters[0]: pass exactly one of equals, from/to, exists or references." ) );
        assertTrue( invalid( args( "filters", List.of( args( "equals", "a" ) ) ) ).startsWith( "Invalid filters[0]: field is required." ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "modifiedTime", "from", "yesterday" ) ) ) ).startsWith(
            "Invalid filters[0].from: \"yesterday\" is not an ISO 8601 date, and modifiedTime is a date field." ) );
        assertTrue( invalid( args( "filters", List.of( args( "field", "publish.from", "to", "2026-02-30" ) ) ) ).startsWith(
            "Invalid filters[0].to: \"2026-02-30\" is not a valid date." ) );
        assertTrue( invalid( args( "sort", "title sideways" ) ).startsWith( "Invalid sort direction: \"sideways\"." ) );
    }

    @Test
    public void testIndexFieldsInFieldsDoNotFailTheCallAndPublishDatesComeWhenAsked()
    {
        persons();
        articles( 1 );
        final Content article = store.get( uuid( 100 ) );
        store.put( uuid( 100 ), Content.create( article )
            .publishInfo( ContentPublishInfo.create()
                              .from( Instant.parse( "2026-01-02T00:00:00Z" ) )
                              .to( Instant.parse( "2026-02-01T00:00:00Z" ) )
                              .build() )
            .build() );

        final Map<String, Object> structured = run(
            args( "types", List.of( "article" ), "fields", List.of( "displayName", "_path", "publish.to", "title", "creator", "language" ) ) );

        final Map<String, Object> hit = hits( structured ).get( 0 );
        assertEquals( Map.of( "from", "2026-01-02T00:00:00.000Z", "to", "2026-02-01T00:00:00.000Z" ), hit.get( "publish" ) );
        assertEquals( Map.of( "title", "Title 0" ), hit.get( "data" ) );
        assertEquals( "fields returns data fields and publish only, so creator, language were left out; filters and sort take index " +
                          "fields, and get returns createdTime and language. For the caller's own edits pass mine.", structured.get( "note" ) );

        final Map<String, Object> anonymous = structured( as( AuthenticationInfo.unAuthenticated(), () -> tool().execute(
            args( "types", List.of( "article" ), "fields", List.of( "creator" ) ) ) ) );
        assertEquals( "fields returns data fields and publish only, so creator was left out; filters and sort take index fields, and " +
                          "get returns createdTime and language. Anonymous callers cannot filter, sort or aggregate on creator, modifier " +
                          "or owner.", anonymous.get( "note" ) );

        final Map<String, Object> plain = hits( run( args( "types", List.of( "article" ), "fields", List.of( "publish", "_id" ) ) ) ).get( 0 );
        assertEquals( Map.of( "from", "2026-01-02T00:00:00.000Z", "to", "2026-02-01T00:00:00.000Z" ), plain.get( "publish" ) );
        assertFalse( plain.containsKey( "data" ) );
        assertFalse( hits( run( args( "types", List.of( "article" ) ) ) ).get( 0 ).containsKey( "publish" ) );
        assertTrue( invalid( args( "types", List.of( ARTICLE ), "fields", List.of( "data.publish" ) ) ).startsWith(
            "Invalid fields: \"data.publish\" is not a data field of " + ARTICLE + "." ) );
    }

    @Test
    public void testADataFieldNamedLikeAnIndexFieldIsReturnedAsData()
    {
        final PropertyTree data = new PropertyTree();
        data.addString( "language", "Norwegian" );
        put( uuid( 700 ), "/notes", "note-1", NOTE, data, false );
        searchable.add( uuid( 700 ) );

        final Map<String, Object> structured = run( args( "types", List.of( NOTE ), "fields", List.of( "language", "type" ) ) );

        assertEquals( Map.of( "language", "Norwegian" ), hits( structured ).get( 0 ).get( "data" ) );
        assertNull( structured.get( "note" ) );
    }

    @Test
    public void testValidIsAFilterField()
    {
        folders( 1 );

        run( args( "filters", List.of( args( "field", "valid", "equals", false ) ) ) );

        assertEquals( Map.of( "term", Map.of( "field", "valid", "value", false ) ),
                      ( (Map<?, ?>) dsl( searches.get( 0 ) ).get( "boolean" ) ).get( "must" ) );
    }

    @Test
    public void testUserValuesStayVariables()
    {
        folders( 1 );
        final String hostile = "\") { _id } } } query X { __schema { types { name } } } #{\"";

        final Map<String, Object> structured =
            run( args( "text", hostile, "filters", List.of( args( "field", "displayName", "equals", hostile ) ) ) );

        assertNull( structured.get( "errors" ) );
        final List<?> must = (List<?>) ( (Map<?, ?>) dsl( searches.get( 0 ) ).get( "boolean" ) ).get( "must" );
        assertEquals( hostile, ( (Map<?, ?>) ( (Map<?, ?>) must.get( 0 ) ).get( "fulltext" ) ).get( "query" ) );
        assertEquals( hostile, ( (Map<?, ?>) ( (Map<?, ?>) must.get( 1 ) ).get( "term" ) ).get( "value" ) );
    }

    @Test
    public void testEmptyResultSaysWhyWhenTheArgumentsShowIt()
    {
        final Map<String, Object> structured = run( args( "types", List.of( "com.example:nope", "nothing" ), "path", "/gone" ) );

        assertEquals( 0, structured.get( "total" ) );
        assertEquals( "No installed content type is named com.example:nope, nothing; call list-types for the type names. No content " +
                          "at path /gone on draft that the caller can read.", structured.get( "note" ) );

        when( resolver.resolve( any() ) ).thenReturn( new ProjectBranch( "myproject", "master" ) );
        assertEquals( "master holds published content, which is not in progress; search draft as a signed-in user.",
                      run( args( "workflow", "IN_PROGRESS" ) ).get( "note" ) );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> hits( final Map<String, Object> structured )
    {
        return (List<Map<String, Object>>) structured.get( "hits" );
    }

    static void assertCallerContext( final Context context )
    {
        assertEquals( "com.enonic.cms.myproject", context.getRepositoryId().toString() );
        assertEquals( ContentConstants.BRANCH_DRAFT, context.getBranch() );
        assertEquals( ALICE_KEY, context.getAuthInfo().getUser().getKey() );
        assertFalse( context.getAuthInfo().hasRole( RoleKeys.ADMIN ) );
    }
}
