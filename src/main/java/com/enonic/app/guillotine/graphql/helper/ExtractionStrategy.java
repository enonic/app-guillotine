package com.enonic.app.guillotine.graphql.helper;

import java.util.List;

import com.enonic.xp.content.Content;

interface ExtractionStrategy
{
    List<Content> extract( Object jsApiResult );
}
