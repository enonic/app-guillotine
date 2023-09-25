package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.repository.RepositoryId;

public class GetAttachmentUrlByNameDataFetcher
    implements DataFetcher<String>
{
    private final PortalUrlService portalUrlService;

    public GetAttachmentUrlByNameDataFetcher( final PortalUrlService portalUrlService )
    {
        this.portalUrlService = portalUrlService;
    }

    @Override
    public String get( final DataFetchingEnvironment environment )
        throws Exception
    {
        return GuillotineLocalContextHelper.executeInContext( environment, () -> doGet( environment ) );
    }

    private String doGet( final DataFetchingEnvironment environment )
    {
        PortalRequest portalRequest = PortalRequestAccessor.get();
        portalRequest.setRepositoryId(
            RepositoryId.from( GuillotineLocalContextHelper.getRepositoryId( environment, portalRequest.getRepositoryId() ) ) );
        portalRequest.setBranch( Branch.from( GuillotineLocalContextHelper.getBranch( environment, portalRequest.getBranch() ) ) );

        Map<String, Object> attachmentAsMap = environment.getSource();

        AttachmentUrlParams params = new AttachmentUrlParams().id( attachmentAsMap.get( Constants.CONTENT_ID_FIELD ).toString() ).name(
            attachmentAsMap.get( "name" ).toString() ).download( Objects.toString( environment.getArgument( "download" ), "false" ) ).type(
            environment.getArgument( "type" ) ).portalRequest( portalRequest );
        return portalUrlService.attachmentUrl( params );
    }
}
