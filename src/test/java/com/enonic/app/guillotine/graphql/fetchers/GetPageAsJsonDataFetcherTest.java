package com.enonic.app.guillotine.graphql.fetchers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.descriptor.DescriptorKey;
import com.enonic.xp.page.GetDefaultPageTemplateParams;
import com.enonic.xp.page.PageTemplate;
import com.enonic.xp.page.PageTemplateService;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.site.Site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetPageAsJsonDataFetcherTest {
    private static final RepositoryId QUERY_REPO = RepositoryId.from("com.enonic.cms.sample-blog");

    private static final Branch QUERY_BRANCH = Branch.from("master");

    private static final String POST_ID = "9f1e2d3c-4b5a-4697-8877-66554433aabb";

    private ContentService contentService;

    private PageTemplateService pageTemplateService;

    private GetPageAsJsonDataFetcher instance;

    private DataFetchingEnvironment environment;

    @BeforeEach
    public void setUp() {
        contentService = mock(ContentService.class);
        pageTemplateService = mock(PageTemplateService.class);

        final ServiceFacade serviceFacade = mock(ServiceFacade.class);
        when(serviceFacade.getContentService()).thenReturn(contentService);
        when(serviceFacade.getPageTemplateService()).thenReturn(pageTemplateService);

        instance = new GetPageAsJsonDataFetcher(serviceFacade);

        final Map<String, Object> localContext = new HashMap<>();
        localContext.put(Constants.PROJECT_ARG, "sample-blog");
        localContext.put(Constants.BRANCH_ARG, QUERY_BRANCH.getValue());

        final Map<String, Object> source = new HashMap<>();
        source.put("_id", POST_ID);
        source.put("type", "superhero:post");
        source.put("page", new HashMap<>());

        environment = mock(DataFetchingEnvironment.class);
        when(environment.getLocalContext()).thenReturn(localContext);
        when(environment.getSource()).thenReturn(source);
        when(environment.getArgument("resolveTemplate")).thenReturn(true);
        when(environment.getArgument("resolveFragment")).thenReturn(false);
    }

    @Test
    public void defaultTemplateIsResolvedInTheQueryContext()
        throws Exception {
        final Map<String, Context> observedContexts = new HashMap<>();

        when(contentService.getNearestSite(ContentId.from(POST_ID))).thenAnswer(invocation -> {
            observedContexts.put("getNearestSite", ContextAccessor.current());
            return Site.create()
                .id(ContentId.from("7b6a3f1c-2d4e-4f5a-9b8c-1d2e3f4a5b6c"))
                .name("superhero")
                .type(ContentTypeName.site())
                .parentPath(ContentPath.ROOT)
                .data(new PropertyTree())
                .build();
        });

        when(pageTemplateService.getDefault(any(GetDefaultPageTemplateParams.class))).thenAnswer(invocation -> {
            observedContexts.put("getDefault", ContextAccessor.current());
            return PageTemplate.newPageTemplate()
                .id(ContentId.from("3c2b1a0f-9e8d-4c7b-a6f5-e4d3c2b1a0f9"))
                .name("post-show-2-columns")
                .parentPath(ContentPath.from("/superhero/_templates"))
                .type(ContentTypeName.pageTemplate())
                .controller(DescriptorKey.from("com.enonic.app.superhero:post-show-2-columns"))
                .data(new PropertyTree())
                .creator(PrincipalKey.ofAnonymous())
                .createdTime(Instant.parse("1975-01-08T00:00:00Z"))
                .build();
        });

        // the request itself carries no repository or branch, like a plain request to the api connector
        final Object result = ContextBuilder.create().build().callWith(() -> instance.get(environment));

        final Map<String, Object> page = CastHelper.cast(result);
        assertNotNull(page);
        assertEquals("com.enonic.app.superhero:post-show-2-columns", page.get("descriptor"));

        assertTrue(observedContexts.containsKey("getNearestSite"));
        assertTrue(observedContexts.containsKey("getDefault"));
        observedContexts.forEach((call, context) -> {
            assertEquals(QUERY_REPO, context.getRepositoryId(), call + " must run in the repository of the query");
            assertEquals(QUERY_BRANCH, context.getBranch(), call + " must run in the branch of the query");
        });
    }
}
