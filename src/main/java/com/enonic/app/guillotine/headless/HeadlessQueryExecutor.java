package com.enonic.app.guillotine.headless;

import java.util.Map;

public interface HeadlessQueryExecutor
{
    Object execute( String schemaId, String query, Map<String, Object> variables );
}
