package com.enonic.app.guillotine.graphql.commands;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FindContentsParams
{
    private final Integer start;

    private final Integer first;

    private final Object query;

    private final Object sort;

    private final Map<String, Object> aggregations;

    private final Map<String, Object> highlight;

    private final List<Map<String, Object>> filters;

    private final List<String> contentTypes;

    private FindContentsParams( Builder builder )
    {
        this.start = Objects.requireNonNullElse( builder.start, 0 );
        this.first = Objects.requireNonNullElse( builder.first, 10 );
        this.query = builder.query;
        this.sort = builder.sort;
        this.aggregations = builder.aggregations;
        this.highlight = builder.highlight;
        this.filters = builder.filters;
        this.contentTypes = builder.contentTypes;
    }

    public Integer getStart()
    {
        return start;
    }

    public Integer getFirst()
    {
        return first;
    }

    public Object getQuery()
    {
        return query;
    }

    public Object getSort()
    {
        return sort;
    }

    public Map<String, Object> getAggregations()
    {
        return aggregations;
    }

    public Map<String, Object> getHighlight()
    {
        return highlight;
    }

    public List<Map<String, Object>> getFilters()
    {
        return filters;
    }

    public List<String> getContentTypes()
    {
        return contentTypes;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static final class Builder
    {
        Integer start;

        Integer first;

        Object query;

        Object sort;

        Map<String, Object> aggregations;

        Map<String, Object> highlight;

        List<Map<String, Object>> filters;

        List<String> contentTypes;

        public Builder setStart( final Integer start )
        {
            this.start = start;
            return this;
        }

        public Builder setFirst( final Integer first )
        {
            this.first = first;
            return this;
        }

        public Builder setQuery( final Object query )
        {
            this.query = query;
            return this;
        }

        public Builder setSort( final Object sort )
        {
            this.sort = sort;
            return this;
        }

        public Builder setAggregations( final Map<String, Object> aggregations )
        {
            this.aggregations = aggregations;
            return this;
        }

        public Builder setHighlight( final Map<String, Object> highlight )
        {
            this.highlight = highlight;
            return this;
        }

        public Builder setFilters( final List<Map<String, Object>> filters )
        {
            this.filters = filters;
            return this;
        }

        public Builder setContentTypes( final List<String> contentTypes )
        {
            this.contentTypes = contentTypes;
            return this;
        }

        public FindContentsParams build()
        {
            return new FindContentsParams( this );
        }
    }
}
