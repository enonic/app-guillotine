package com.enonic.app.guillotine.graphql;

import java.util.List;

import graphql.schema.GraphQLType;

public class OutputObjectCreationCallbackParams
    extends BaseCreationCallbackParams
{
    private List<GraphQLType> interfaces;

    public List<GraphQLType> getInterfaces()
    {
        return interfaces;
    }

    public void setInterfaces( final List<GraphQLType> interfaces )
    {
        this.interfaces = interfaces;
    }
}
