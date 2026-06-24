package com.enonic.app.guillotine;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.app.guillotine.graphql.SchemaProvider;

@Component(service = Function.class, property = {"function=guillotine.execute"})
public class GuillotineExecutor
    implements Function<Map<String, Object>, Map<String, Object>>
{
    private static final String QUERY_KEY = "query";

    private static final String VARIABLES_KEY = "variables";

    private final SchemaProvider schemaProvider;

    @Activate
    public GuillotineExecutor( @Reference final SchemaProvider schemaProvider )
    {
        this.schemaProvider = schemaProvider;
    }

    @Override
    public Map<String, Object> apply( final Map<String, Object> input )
    {
        final Object query = input.get( QUERY_KEY );
        if ( !( query instanceof String ) )
        {
            throw new IllegalArgumentException( "'query' is required and must be a String" );
        }

        final Object variables = input.get( VARIABLES_KEY );
        final Map<String, Object> variablesMap = variables instanceof Map ? castToMap( variables ) : Map.of();

        return schemaProvider.executeToSpecification( (String) query, variablesMap );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMap( final Object value )
    {
        return (Map<String, Object>) value;
    }
}
