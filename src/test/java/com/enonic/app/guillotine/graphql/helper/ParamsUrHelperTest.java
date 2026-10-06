package com.enonic.app.guillotine.graphql.helper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParamsUrHelperTest
{
    @Test
    public void convertToMultimap()
    {
        final Map<String, Object> params = new LinkedHashMap<>();
        params.put( "a", "x" );
        params.put( "b", 1 );
        params.put( "c", List.of( "y", "z" ) );
        params.put( "d", null );
        params.put( "e", Arrays.asList( null, "w" ) );

        assertEquals( Map.of( "a", List.of( "x" ), "b", List.of( "1" ), "c", List.of( "y", "z" ), "d", List.of(), "e", List.of( "w" ) ),
                      ParamsUrHelper.convertToMultimap( params ) );
    }
}
