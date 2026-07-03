package com.enonic.app.guillotine;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(immediate = true, service = GuillotineConfigService.class, configurationPid = "com.enonic.app.guillotine")
public class GuillotineConfigService
{
    private static final String ALLOW_ANY_BASE_URL = "*";

    private QueryPlaygroundUIMode queryPlaygroundUIMode;

    private ModifyUnknownFieldMode modifyUnknownFieldMode;

    private int maxQueryTokens;

    private String mediaBaseUrl;

    private Set<String> allowedBaseUrls;

    @Activate
    @Modified
    public void activate( final GuillotineConfig config )
    {
        this.queryPlaygroundUIMode = QueryPlaygroundUIMode.from( config.queryplayground_ui_mode() );
        this.modifyUnknownFieldMode = ModifyUnknownFieldMode.from( config.graphql_extensions_modifyUnknownField() );
        this.maxQueryTokens = config.maxQueryTokens();
        this.mediaBaseUrl = config.mediaBaseUrl();
        this.allowedBaseUrls = Arrays.stream( config.allowedBaseUrls().split( "," ) )
            .map( String::trim )
            .filter( value -> !value.isEmpty() )
            .map( GuillotineConfigService::removeTrailingSlash )
            .collect( Collectors.toUnmodifiableSet() );
    }

    public QueryPlaygroundUIMode getQueryPlaygroundUIMode()
    {
        return queryPlaygroundUIMode;
    }

    public ModifyUnknownFieldMode getModifyUnknownFieldMode()
    {
        return modifyUnknownFieldMode;
    }

    public int getMaxQueryTokens()
    {
        return maxQueryTokens;
    }

    public String getMediaBaseUrl()
    {
        return mediaBaseUrl;
    }

    public boolean isBaseUrlAllowed( final String baseUrl )
    {
        if ( baseUrl == null )
        {
            return false;
        }
        return allowedBaseUrls.contains( ALLOW_ANY_BASE_URL ) || allowedBaseUrls.contains( removeTrailingSlash( baseUrl.trim() ) );
    }

    private static String removeTrailingSlash( final String value )
    {
        return value.endsWith( "/" ) ? value.substring( 0, value.length() - 1 ) : value;
    }
}
