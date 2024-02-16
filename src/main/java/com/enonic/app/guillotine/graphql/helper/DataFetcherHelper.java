package com.enonic.app.guillotine.graphql.helper;

import java.util.Collection;
import java.util.Map;

import com.enonic.app.guillotine.graphql.Constants;

public class DataFetcherHelper
{
    public static Object removeField( final Object object, final String fieldName )
    {
        if ( object instanceof Map )
        {
            Map<String, Object> map = CastHelper.cast( object );
            map.remove( fieldName );
            map.keySet().forEach( key -> removeField( map.get( key ), fieldName ) );
        }
        if ( object instanceof Collection )
        {
            Collection<?> collection = CastHelper.cast( object );
            for ( Object item : collection )
            {
                removeField( item, fieldName );
            }
        }
        return object;
    }

    public static Object removeContentIdField( final Object object )
    {
        return removeField( object, Constants.CONTENT_ID_FIELD );
    }
}
