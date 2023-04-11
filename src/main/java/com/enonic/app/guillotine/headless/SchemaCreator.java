package com.enonic.app.guillotine.headless;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SchemaCreator
{
    @SuppressWarnings("unchecked")
    public HeadlessSchemaParams createSchema()
    {
        List<HeadlessFieldDefinition> fieldDefinitions = new ArrayList<>();
        fieldDefinitions.add( new HeadlessFieldDefinition()
        {
            {
                setName( "get" );
                setDescription( "Get content by key" );
                setResolveFunction( environment -> {
                    Object source = environment.getSource();

                    if ( source instanceof Map )
                    {
                        return ( (Map<String, Object>) source ).get( "_id" );
                    }

                    return null;
                } );
            }
        } );

        HeadlessObjectParams queryParams = new HeadlessObjectParams();

        queryParams.setDescription( "Query" );
        queryParams.setTypeName( "Query" );
        queryParams.setFields( fieldDefinitions );

        HeadlessSchemaParams schemaParams = new HeadlessSchemaParams();

        schemaParams.setQuery( queryParams );

        return schemaParams;
    }
}
