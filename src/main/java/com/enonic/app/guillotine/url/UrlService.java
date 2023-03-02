package com.enonic.app.guillotine.url;

public interface UrlService
{
    String imageUrl( ImageUrlParams params );

    String assetUrl( AssetUrlParams params );
}
