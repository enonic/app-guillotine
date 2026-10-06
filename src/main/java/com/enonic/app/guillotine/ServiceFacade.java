package com.enonic.app.guillotine;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.app.guillotine.graphql.ComponentDescriptorService;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.page.PageTemplateService;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.schema.content.CmsFormFragmentService;
import com.enonic.xp.schema.content.ContentTypeService;

@Component(immediate = true, service = ServiceFacade.class)
public class ServiceFacade
{
    private final ContentService contentService;

    private final ContentTypeService contentTypeService;

    private final ComponentDescriptorService componentDescriptorService;

    private final PortalUrlService portalUrlService;

    private final NodeService nodeService;

    private final CmsFormFragmentService cmsFormFragmentService;

    private final PageTemplateService pageTemplateService;

    @Activate
    public ServiceFacade( final @Reference ContentService contentService, final @Reference ContentTypeService contentTypeService,
                          final @Reference ComponentDescriptorService componentDescriptorService,
                          final @Reference PortalUrlService portalUrlService, final @Reference NodeService nodeService,
                          final @Reference CmsFormFragmentService cmsFormFragmentService,
                          final @Reference PageTemplateService pageTemplateService )
    {
        this.contentService = contentService;
        this.contentTypeService = contentTypeService;
        this.componentDescriptorService = componentDescriptorService;
        this.portalUrlService = portalUrlService;
        this.nodeService = nodeService;
        this.cmsFormFragmentService = cmsFormFragmentService;
        this.pageTemplateService = pageTemplateService;
    }

    public ContentService getContentService()
    {
        return contentService;
    }

    public ContentTypeService getContentTypeService()
    {
        return contentTypeService;
    }

    public ComponentDescriptorService getComponentDescriptorService()
    {
        return componentDescriptorService;
    }

    public PortalUrlService getPortalUrlService()
    {
        return portalUrlService;
    }

    public NodeService getNodeService()
    {
        return nodeService;
    }

    public CmsFormFragmentService getCmsFormFragmentService()
    {
        return cmsFormFragmentService;
    }

    public PageTemplateService getPageTemplateService()
    {
        return pageTemplateService;
    }
}
