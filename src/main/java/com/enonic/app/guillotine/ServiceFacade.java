package com.enonic.app.guillotine;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.enonic.app.guillotine.graphql.ComponentDescriptorService;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.macro.MacroDescriptorService;
import com.enonic.xp.macro.MacroService;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.page.PageTemplateService;
import com.enonic.xp.portal.url.PortalUrlGeneratorService;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.mixin.MixinService;
import com.enonic.xp.style.StyleDescriptorService;

@Component(immediate = true, service = ServiceFacade.class)
public class ServiceFacade
{
    private final ContentService contentService;

    private final ContentTypeService contentTypeService;

    private final ComponentDescriptorService componentDescriptorService;

    private final PortalUrlService portalUrlService;

    private final NodeService nodeService;

    private final MixinService mixinService;

    private final MacroService macroService;

    private final MacroDescriptorService macroDescriptorService;

    private final PageTemplateService pageTemplateService;

    private final StyleDescriptorService styleDescriptorService;

    private final PortalUrlGeneratorService portalUrlGeneratorService;

    @Activate
    public ServiceFacade( final @Reference ContentService contentService, final @Reference ContentTypeService contentTypeService,
                          final @Reference ComponentDescriptorService componentDescriptorService,
                          final @Reference PortalUrlService portalUrlService, final @Reference NodeService nodeService,
                          final @Reference MixinService mixinService, final @Reference MacroService macroService,
                          final @Reference MacroDescriptorService macroDescriptorService,
                          final @Reference PageTemplateService pageTemplateService,
                          final @Reference StyleDescriptorService styleDescriptorService,
                          final @Reference PortalUrlGeneratorService portalUrlGeneratorService )
    {
        this.contentService = contentService;
        this.contentTypeService = contentTypeService;
        this.componentDescriptorService = componentDescriptorService;
        this.portalUrlService = portalUrlService;
        this.nodeService = nodeService;
        this.mixinService = mixinService;
        this.macroService = macroService;
        this.macroDescriptorService = macroDescriptorService;
        this.pageTemplateService = pageTemplateService;
        this.styleDescriptorService = styleDescriptorService;
        this.portalUrlGeneratorService = portalUrlGeneratorService;
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

    public MixinService getMixinService()
    {
        return mixinService;
    }

    public MacroService getMacroService()
    {
        return macroService;
    }

    public MacroDescriptorService getMacroDescriptorService()
    {
        return macroDescriptorService;
    }

    public PageTemplateService getPageTemplateService()
    {
        return pageTemplateService;
    }

    public StyleDescriptorService getStyleDescriptorService()
    {
        return styleDescriptorService;
    }

    public PortalUrlGeneratorService getPortalUrlGeneratorService()
    {
        return portalUrlGeneratorService;
    }
}
