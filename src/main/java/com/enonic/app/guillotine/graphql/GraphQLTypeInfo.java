package com.enonic.app.guillotine.graphql;

public final class GraphQLTypeInfo
{
    private final String typeName;

    private final boolean isList;

    public GraphQLTypeInfo( final String baseTypeName, final boolean isList )
    {
        this.typeName = baseTypeName;
        this.isList = isList;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public boolean isList()
    {
        return isList;
    }
}
