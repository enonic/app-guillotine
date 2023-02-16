package com.enonic.app.guillotine.handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import com.enonic.xp.app.Application;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.portal.script.PortalScriptService;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.ScriptExports;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class SchemaGetCreationCallbackHandler
    implements ScriptBean
{
    private static final Logger LOG = LoggerFactory.getLogger( SchemaGetCreationCallbackHandler.class );

    private static final String METHOD_NAME = "registerCreationCallbacks";

    private static final String SCRIPT_PATH = "extensions/extensions.js";

    private Supplier<ApplicationService> applicationService;

    private Supplier<PortalScriptService> scriptService;

    private Supplier<ResourceService> resourceService;

    @Override
    public void initialize( final BeanContext context )
    {
        this.applicationService = context.getService( ApplicationService.class );
        this.scriptService = context.getService( PortalScriptService.class );
        this.resourceService = context.getService( ResourceService.class );
    }

    public Object execute()
    {
        return new CreationCallbackMapper( doExecute() );
    }

    private List<Map<String, Object>> doExecute()
    {
        return applicationService.get().getInstalledApplications().stream().map( this::getCreationCallback ).filter(
            Objects::nonNull ).collect( Collectors.toList() );
    }

    private Map<String, Object> getCreationCallback( Application application )
    {
        ResourceKey resourceKey = ResourceKey.from( application.getKey(), SCRIPT_PATH );
        if ( resourceService.get().getResource( resourceKey ).exists() )
        {
            ScriptExports exports = scriptService.get().execute( resourceKey );
            if ( exports.hasMethod( METHOD_NAME ) )
            {
                try
                {
                    ScriptValue scriptValue = exports.executeMethod( METHOD_NAME );
                    return scriptValue.getMap();
                }
                catch ( Exception e )
                {
                    LOG.warn( "Schema extensions can not be extracted from {}", resourceKey, e );
                }
            }
        }
        return null;
    }

    public static class CreationCallbackMapper
        implements MapSerializable
    {

        private final List<Map<String, Object>> maps;

        public CreationCallbackMapper( final List<Map<String, Object>> maps )
        {
            this.maps = maps;
        }

        @Override
        public void serialize( final MapGenerator gen )
        {
            gen.map( "customCallbacks" );
            if ( maps != null )
            {
                maps.forEach( map -> {
                    for ( final Map.Entry<?, ?> entry : map.entrySet() )
                    {
                        gen.value( entry.getKey().toString(), entry.getValue() );
                    }
                } );
            }
            gen.end();
        }
    }

}
