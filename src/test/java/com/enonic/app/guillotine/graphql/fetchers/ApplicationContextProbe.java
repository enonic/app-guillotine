package com.enonic.app.guillotine.graphql.fetchers;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

/**
 * Test probe, instantiated from a resolver via {@code __.newBean(...)}, that reports the application context seen while
 * a resolver runs:
 * <ul>
 *   <li>{@link #getScriptApplicationKey()} — {@code BeanContext.getApplicationKey()}, the executing script's application.
 *       This is the value {@code /lib/xp/i18n} uses to resolve message bundles.</li>
 *   <li>{@link #getRequestApplicationKey()} — the current {@code PortalRequest} application, i.e. the value that
 *       {@code DynamicDataFetcher} temporarily swaps to the resolver's application (#741).</li>
 * </ul>
 */
public final class ApplicationContextProbe
    implements ScriptBean
{
    private ApplicationKey scriptApplicationKey;

    @Override
    public void initialize( final BeanContext context )
    {
        this.scriptApplicationKey = context.getApplicationKey();
    }

    public String getScriptApplicationKey()
    {
        return scriptApplicationKey != null ? scriptApplicationKey.toString() : null;
    }

    public String getRequestApplicationKey()
    {
        final PortalRequest portalRequest = PortalRequestAccessor.get();
        return portalRequest != null && portalRequest.getApplicationKey() != null
            ? portalRequest.getApplicationKey().toString()
            : null;
    }
}
