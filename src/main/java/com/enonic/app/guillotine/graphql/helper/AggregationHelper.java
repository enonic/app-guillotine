package com.enonic.app.guillotine.graphql.helper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.enonic.app.guillotine.graphql.Constants;

public class AggregationHelper
{
    public static Map<String, Object> createAggregations( List<Map<String, Object>> inputAggregations )
    {
        Map<String, Object> holder = new LinkedHashMap<>();

        inputAggregations.forEach( inputAggregation -> createAggregation( holder, inputAggregation ) );

        return holder;
    }

    private static void createAggregation( Map<String, Object> holder, Map<String, Object> inputAggregation )
    {
        String inputAggregationName = inputAggregation.get( "name" ).toString();

        Map<String, Object> aggregationProjection = new HashMap<>();
        holder.put( inputAggregationName, aggregationProjection );

        Constants.SUPPORTED_AGGREGATIONS.forEach( aggregationName -> {
            if ( inputAggregation.get( aggregationName ) != null )
            {
                aggregationProjection.put( aggregationName, inputAggregation.get( aggregationName ) );
            }
        } );

        List<Map<String, Object>> subAggregations = CastHelper.cast( inputAggregation.get( "subAggregations" ) );
        if ( subAggregations != null && !subAggregations.isEmpty() )
        {
            Map<String, Object> subAggregationHolder = new HashMap<>();
            aggregationProjection.put( "aggregations", subAggregationHolder );

            subAggregations.forEach( subAggregation -> createAggregation( subAggregationHolder, subAggregation ) );
        }
    }
}
