package com.enonic.app.guillotine.graphql;

import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLSchema;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequestAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pins down the application context observed while an extension resolver runs, now that the {@code PortalRequest}
 * application swap (#741) has been removed from {@code DynamicDataFetcher}.
 * <p>
 * The incoming request is set to a <em>different</em> application ({@code com.enonic.web}) than the extension that
 * owns the resolver ({@code myapplication}). The resolver reports, via
 * {@code com.enonic.app.guillotine.graphql.fetchers.ApplicationContextProbe}:
 * <ul>
 *   <li>{@code script=...} — {@code BeanContext.getApplicationKey()} (what {@code /lib/xp/i18n} keys off);</li>
 *   <li>{@code request=...} — the current {@code PortalRequest} application;</li>
 *   <li>{@code app=...} — the {@code app.name} script global (built from the script's application, never from
 *       {@code PortalRequest}).</li>
 * </ul>
 * {@code script} and {@code app} resolve to the extension ({@code myapplication}) via the script execution context,
 * independent of {@code PortalRequest}. {@code request} stays the incoming application ({@code com.enonic.web}) — the
 * resolver no longer swaps it, so portal URL functions that omit an explicit {@code application} now default to the
 * request's application.
 */
public class ResolverApplicationContextGraphQLIntegrationTest
    extends BaseGraphQLIntegrationTest
{
    @Test
    public void resolverObservesExtensionApplicationContext()
    {
        PortalRequestAccessor.get().setApplicationKey( ApplicationKey.from( "com.enonic.web" ) );

        final GraphQLSchema schema = getBean().createSchema();

        final Map<String, Object> response = executeQuery( schema, "{ applicationContextProbe }" );

        assertFalse( response.containsKey( "errors" ), () -> "Unexpected errors: " + response.get( "errors" ) );

        final Map<String, Object> data = CastHelper.cast( response.get( "data" ) );

        // BeanContext and app.name are the extension app (via the script execution context), independent of the
        // request app; the request application is left untouched (no swap).
        assertEquals( "script=myapplication;request=com.enonic.web;app=myapplication", data.get( "applicationContextProbe" ) );

        assertEquals( "com.enonic.web", PortalRequestAccessor.get().getApplicationKey().toString() );
    }
}
