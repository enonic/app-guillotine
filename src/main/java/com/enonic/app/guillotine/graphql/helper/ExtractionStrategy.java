package com.enonic.app.guillotine.graphql.helper;

import java.util.List;
import java.util.Map;

interface ExtractionStrategy
{
    List<Map<String, Object>> extract( Object jsApiResult );
}
