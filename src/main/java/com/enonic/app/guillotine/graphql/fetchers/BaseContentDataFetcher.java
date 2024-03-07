package com.enonic.app.guillotine.graphql.fetchers;

import java.util.regex.Pattern;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.site.Site;

public abstract class BaseContentDataFetcher
	implements DataFetcher<Object>
{
	private final static Pattern SITE_KEY_PATTERN = Pattern.compile( "\\$\\{site\\}" );

	protected final ContentService contentService;

	public BaseContentDataFetcher( final ContentService contentService )
	{
		this.contentService = contentService;
	}

	protected Content getContent( DataFetchingEnvironment environment, boolean returnRootContent )
	{
		String siteKey = GuillotineLocalContextHelper.getSiteKey( environment );

		String argumentKey = environment.getArgument( "key" );

		if ( argumentKey != null )
		{
			Site site = getSiteByKey( siteKey );
			String key = argumentKey.replaceAll( SITE_KEY_PATTERN.pattern(), site != null ? site.getPath().toString() : "" );
			return getContentByKey( key, returnRootContent, environment );
		}
		else
		{
			return getContentByKey( siteKey, returnRootContent, environment );
		}
	}

	private Site getSiteByKey( String siteKey )
	{
		if ( siteKey.equals( "/" ) )
		{
			return null;
		}
		return siteKey.startsWith( "/" )
			? contentService.findNearestSiteByPath( ContentPath.from( siteKey ) )
			: contentService.getNearestSite( ContentId.from( siteKey ) );
	}

	private Content getContentByKey( String key, boolean returnRootContent, DataFetchingEnvironment environment )
	{
		Content content = new GetContentCommand( contentService ).executeAndGetContent( key, environment );

		if ( content != null && "/".equals( content.getPath().toString() ) && !returnRootContent )
		{
			return null;
		}

		return content;
	}
}
