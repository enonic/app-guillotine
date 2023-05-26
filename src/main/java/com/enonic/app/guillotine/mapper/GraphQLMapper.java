package com.enonic.app.guillotine.mapper;

import java.util.function.Function;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

import com.enonic.app.guillotine.graphql.scalars.CustomScalars;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class GraphQLMapper
    implements MapSerializable
{
    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.rawValue( "GraphQLString", Scalars.GraphQLString );
        gen.rawValue( "GraphQLInt", Scalars.GraphQLInt );
        gen.rawValue( "GraphQLID", Scalars.GraphQLID );
        gen.rawValue( "GraphQLBoolean", Scalars.GraphQLBoolean );
        gen.rawValue( "GraphQLFloat", Scalars.GraphQLFloat );
        gen.rawValue( "Json", ExtendedScalars.Json );
        gen.rawValue( "DateTime", ExtendedScalars.DateTime );
        gen.rawValue( "Date", ExtendedScalars.Date );
        gen.rawValue( "LocalTime", CustomScalars.LocalTime );
        gen.rawValue( "LocalDateTime", CustomScalars.LocalDateTime );
        gen.rawValue( "list", (Function<GraphQLType, GraphQLType>) GraphQLList::new );
        gen.rawValue( "nonNull", (Function<GraphQLType, GraphQLType>) GraphQLNonNull::new );
        gen.rawValue( "reference", (Function<String, GraphQLTypeReference>) GraphQLTypeReference::new );
    }
}
