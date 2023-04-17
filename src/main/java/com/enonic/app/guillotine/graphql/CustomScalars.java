package com.enonic.app.guillotine.graphql;

import graphql.schema.GraphQLScalarType;

public class CustomScalars
{
    public static final GraphQLScalarType LocalDateTime = LocalDateTimeScalar.newLocalDateTime();

    public static final GraphQLScalarType LocalTime = LocalTimeScalar.newLocalTime();

    private CustomScalars()
    {
    }
}
