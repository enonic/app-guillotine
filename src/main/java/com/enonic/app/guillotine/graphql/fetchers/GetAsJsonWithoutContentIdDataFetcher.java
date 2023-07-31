package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.CastHelper;

public class GetAsJsonWithoutContentIdDataFetcher
    implements DataFetcher<Object>
{

    private final String fieldName;

    public GetAsJsonWithoutContentIdDataFetcher( final String fieldName )
    {
        this.fieldName = fieldName;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> sourceAsMap = new HashMap<>( environment.getSource() );
        Object data = CastHelper.cast( sourceAsMap.get( fieldName ) );
        removeContentIdField( data );
        return data;
    }

    private void removeContentIdField( final Object object )
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
            collection.forEach( this::removeContentIdField );
        }
    }

}
