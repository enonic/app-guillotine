package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.commands.GetContentCommand;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.auth.AuthenticationInfo;

public class GetContentPathDataFetcher
    implements DataFetcher<String>
{
    private final ContentService contentService;

    public GetContentPathDataFetcher( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Map<String, Object> contentAsMap = environment.getSource();
        String originalPath = contentAsMap.get( "_path" ).toString();

        if ( Objects.equals( "siteRelative", environment.getArgument( "type" ) ) )
        {
            String sitePath = adminContext().callWith( () -> GuillotineLocalContextHelper.executeInContext( environment, () -> {
                String siteKey = GuillotineLocalContextHelper.getSiteKey( environment );
                return Objects.requireNonNullElse( getSitePathBySiteKey( siteKey, environment ), originalPath );
            } ) );
            String normalizedPath = originalPath.replace( sitePath, "" );
            return normalizedPath.startsWith( "/" ) ? normalizedPath.substring( 1 ) : normalizedPath;
        }
        else
        {
            return originalPath;
        }
    }

    private String getSitePathBySiteKey( final String siteKey, DataFetchingEnvironment environment )
    {
        Map<String, Object> contentAsMap = new GetContentCommand( contentService ).execute( siteKey, environment );
        if ( contentAsMap == null )
        {
            return null;
        }
        return contentAsMap.get( "_path" ).toString();
    }

    public Context adminContext()
    {
        return ContextBuilder.from( ContextAccessor.current() ).authInfo(
            AuthenticationInfo.copyOf( ContextAccessor.current().getAuthInfo() ).principals( RoleKeys.ADMIN ).build() ).build();
    }
}
