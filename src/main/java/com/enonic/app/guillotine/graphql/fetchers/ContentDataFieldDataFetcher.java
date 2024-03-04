package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.CastHelper;

public class ContentDataFieldDataFetcher
	implements DataFetcher<Object>
{
	@Override
	public Object get( final DataFetchingEnvironment environment )
		throws Exception
	{
		Map<String, Object> sourceAsMap = CastHelper.cast( environment.getSource() );
		Map<String, Object> data = CastHelper.cast( sourceAsMap.get( "data" ) );

		if ( data == null )
		{
			return null;
		}

		if ( Objects.equals( sourceAsMap.get( "type" ), "portal:site" ) || Objects.equals( sourceAsMap.get( "_path" ), "/content" ) )
		{
			data.remove( "siteConfig" );
		}

		return data;
	}
}
