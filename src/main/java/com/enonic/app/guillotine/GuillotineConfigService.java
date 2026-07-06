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

    private Set<String> allowedPageBaseUrls;

    private Set<String> allowedMediaBaseUrls;

    @Activate
    @Modified
    public void activate( final GuillotineConfig config )
    {
        this.queryPlaygroundUIMode = QueryPlaygroundUIMode.from( config.queryplayground_ui_mode() );
        this.modifyUnknownFieldMode = ModifyUnknownFieldMode.from( config.graphql_extensions_modifyUnknownField() );
        this.maxQueryTokens = config.maxQueryTokens();
        this.allowedPageBaseUrls = parseAllowedBaseUrls( config.allowedPageBaseUrls() );
        this.allowedMediaBaseUrls = parseAllowedBaseUrls( config.allowedMediaBaseUrls() );
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

    public boolean isPageBaseUrlAllowed( final String baseUrl )
    {
        return isAllowed( allowedPageBaseUrls, baseUrl );
    }

    public boolean isMediaBaseUrlAllowed( final String baseUrl )
    {
        return isAllowed( allowedMediaBaseUrls, baseUrl );
    }

    private static Set<String> parseAllowedBaseUrls( final String value )
    {
        return Arrays.stream( value.split( "," ) )
            .map( String::trim )
            .filter( entry -> !entry.isEmpty() )
            .map( GuillotineConfigService::removeTrailingSlash )
            .collect( Collectors.toUnmodifiableSet() );
    }

    private static boolean isAllowed( final Set<String> allowedBaseUrls, final String baseUrl )
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
