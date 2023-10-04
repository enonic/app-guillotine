package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentNotFoundException;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.security.PrincipalKey;
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
                return Objects.toString( getSitePathBySiteKey( siteKey ), originalPath );
            } ) );
            String normalizedPath = originalPath.replace( sitePath, "" );
            return normalizedPath.startsWith( "/" ) ? normalizedPath.substring( 1 ) : normalizedPath;
        }
        else
        {
            return originalPath;
        }
    }

    private ContentPath getSitePathBySiteKey( final String siteKey )
    {
        if ( siteKey.isEmpty() )
        {
            return null;
        }
        try
        {
            if ( siteKey.startsWith( "/" ) )
            {
                return contentService.getByPath( ContentPath.from( siteKey ) ).getPath();
            }
            else
            {
                return contentService.getById( ContentId.from( siteKey ) ).getPath();
            }
        }
        catch ( ContentNotFoundException e )
        {
            return null;
        }
    }

    public Context adminContext()
    {
        AuthenticationInfo authenticationInfo = AuthenticationInfo.copyOf( ContextAccessor.current().getAuthInfo() ).principals(
            PrincipalKey.ofRole( "system.admin" ) ).build();
        return ContextBuilder.from( ContextAccessor.current() ).authInfo( authenticationInfo ).build();
    }
}
