package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Map;
import java.util.Objects;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.ParamsUrHelper;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.portal.url.AttachmentUrlGeneratorParams;
import com.enonic.xp.portal.url.AttachmentUrlParams;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.project.ProjectName;

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
        final String contentId = GuillotineLocalContextHelper.getContextProperty( environment, Constants.CONTENT_ID_FIELD );

        final Map<String, Object> attachmentAsMap = environment.getSource();

        final Boolean download = environment.getArgument( "download" );

        final String siteBaseUrl = GuillotineLocalContextHelper.getSiteBaseUrl( environment );
        final ProjectName projectName =
            ProjectName.from( GuillotineLocalContextHelper.getContextProperty( environment, Constants.PROJECT_ARG ) );
        final Branch branch = Branch.from( GuillotineLocalContextHelper.getContextProperty( environment, Constants.BRANCH_ARG ) );

//        final AttachmentUrlGeneratorParams.Builder urlParams =
//            AttachmentUrlGeneratorParams.create().setUrlType( environment.getArgument( "type" ) ).setDownload(
//                download != null && download ).setBaseUrl( siteBaseUrl ).setProjectName( () -> projectName ).setBranch(
//                () -> branch ).setName( attachmentAsMap.get( "name" ).toString() ).setContent( () -> {
//                return null;
//            } ).setBaseUrl( siteBaseUrl );

        final AttachmentUrlParams params =
            new AttachmentUrlParams().id( contentId ).name( attachmentAsMap.get( "name" ).toString() ).download(
                Objects.toString( download, "false" ) ).type( environment.getArgument( "type" ) ).baseUrl(
                GuillotineLocalContextHelper.getSiteBaseUrl( environment ) );

        ParamsUrHelper.resolveParams( params.getParams(), environment.getArgument( "params" ) );

        return portalUrlService.attachmentUrl( params );
    }
}
