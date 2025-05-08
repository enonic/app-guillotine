package com.enonic.app.guillotine.graphql.fetchers;

interface ExtractionStrategy<T>
{
    T extract( Object jsApiResult );
}
