package com.enonic.app.guillotine.url;

public interface UrlService
{
    String imageUrl( ImageUrlParams params );

    String attachmentUrl( AttachmentUrlParams params );

    String pageUrl( PageUrlParams params );
}
