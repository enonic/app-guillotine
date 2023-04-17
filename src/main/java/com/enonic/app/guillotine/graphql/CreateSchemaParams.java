package com.enonic.app.guillotine.graphql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CreateSchemaParams
{
    public Builder newInstance()
    {
        return CreateSchemaParams.create();
    }

    private final List<String> applications;

    private final List<String> allowPaths;

    private CreateSchemaParams( Builder builder )
    {
        this.applications = builder.applications;
        this.allowPaths = builder.allowPaths;
    }

    public List<String> getApplications()
    {
        return applications;
    }

    public List<String> getAllowPaths()
    {
        return allowPaths;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static class Builder
    {
        List<String> applications;

        List<String> allowPaths;

        public Builder setApplications( final List<String> applications )
        {
            this.applications = Objects.requireNonNullElse( applications, new ArrayList<>() );
            return this;
        }

        public Builder setAllowPaths( final List<String> allowPaths )
        {
            this.allowPaths = allowPaths;
            return this;
        }

        public CreateSchemaParams build()
        {
            return new CreateSchemaParams( this );
        }
    }
}
