package com.enonic.app.guillotine.graphql.helper;

import java.util.Collection;
import java.util.Map;

import com.enonic.app.guillotine.graphql.Constants;

public class DataFetcherHelper
{
    public static Object removeContentIdField( final Object object )
    {
        if ( object instanceof Map )
        {
            Map<String, Object> map = CastHelper.cast( object );
            map.remove( Constants.CONTENT_ID_FIELD );
            map.keySet().forEach( key -> removeContentIdField( map.get( key ) ) );
        }
        if ( object instanceof Collection )
        {
            Collection<?> collection = CastHelper.cast( object );
            for ( Object item : collection )
            {
                removeContentIdField( item );
            }
        }
        return object;
    }
}
