package com.enonic.app.guillotine.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The compact description of a content shared by the content tools: identity, type, timestamps, workflow and publishing.
 */
final class ContentSummary
{
    static final String BASE_FIELDS = "_id _path displayName type modifiedTime publish { first from to }";

    static final String PRINCIPAL_FIELDS = " modifier { value } owner { value }";

    private ContentSummary()
    {
    }

    static String fields( final boolean withPrincipals )
    {
        return withPrincipals ? BASE_FIELDS + PRINCIPAL_FIELDS : BASE_FIELDS;
    }

    static Map<String, Object> of( final Map<?, ?> node, final String workflow )
    {
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put( "_id", node.get( "_id" ) );
        summary.put( "_path", node.get( "_path" ) );
        summary.put( "displayName", node.get( "displayName" ) );
        summary.put( "type", node.get( "type" ) );
        summary.put( "modifiedTime", node.get( "modifiedTime" ) );
        summary.put( "modifier", principal( node.get( "modifier" ) ) );
        summary.put( "owner", principal( node.get( "owner" ) ) );
        summary.put( "workflow", workflow );
        if ( node.get( "publish" ) instanceof Map<?, ?> publish && publish.values().stream().anyMatch( value -> value != null ) )
        {
            summary.put( "publish", publish );
        }
        return summary;
    }

    private static Object principal( final Object value )
    {
        return value instanceof Map<?, ?> principal ? principal.get( "value" ) : null;
    }
}
