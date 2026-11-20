package com.enonic.app.guillotine.mcp.shape;

import org.junit.jupiter.api.Test;

import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.exception.ForbiddenAccessException;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.security.IdProviderKey;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.auth.AuthenticationInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProjectBranchTest
{
    private static final AuthenticationInfo ALICE = AuthenticationInfo.create()
        .user( User.create().key( PrincipalKey.ofUser( IdProviderKey.system(), "alice" ) ).login( "alice" ).build() )
        .principals( RoleKeys.AUTHENTICATED )
        .build();

    @Test
    public void testDeniedReadBecomesAnErrorThatNamesTheBranchToUse()
    {
        final ForbiddenAccessException denied = new ForbiddenAccessException( ALICE.getUser() );

        assertEquals( "Access denied: the caller cannot read branch draft of project blog. Pass branch master to read published " +
                          "content, or ask an administrator for a role in project blog.",
                      asAlice( () -> assertThrows( ToolError.class, () -> new ProjectBranch( "blog", "draft" ).callWith( () -> {
                          throw denied;
                      } ) ) ).userMessage() );
        assertEquals( "Access denied: the caller cannot read branch master of project blog. Ask an administrator for read access to " +
                          "project blog.", asAlice( () -> new ProjectBranch( "blog", "master" ).accessDenied() ).userMessage() );
        assertEquals( "Access denied: the caller cannot read branch draft of project blog. Anonymous callers read only published " +
                          "content: omit branch or pass branch master.", ContextBuilder.copyOf( ContextAccessor.current() )
                          .authInfo( AuthenticationInfo.unAuthenticated() )
                          .build()
                          .callWith( () -> new ProjectBranch( "blog", "draft" ).accessDenied() )
                          .userMessage() );
    }

    @Test
    public void testCurrentIsTheProjectOfTheContextRepository()
    {
        assertEquals( new ProjectBranch( "blog", "master" ),
                      new ProjectBranch( "blog", "master" ).callWith( ProjectBranch::current ) );
        assertNull( ContextBuilder.copyOf( ContextAccessor.current() )
                        .repositoryId( RepositoryId.from( "system-repo" ) )
                        .build()
                        .callWith( ProjectBranch::current ) );
    }

    private static <T> T asAlice( final java.util.concurrent.Callable<T> callable )
    {
        return ContextBuilder.copyOf( ContextAccessor.current() ).authInfo( ALICE ).build().callWith( callable );
    }
}
