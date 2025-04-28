package com.enonic.app.guillotine.graphql.helper;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.attachment.Attachments;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.Media;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;

public final class ContentDeserializer
{
    @SuppressWarnings("unchecked")
    public static Content convert( final Object jsApiResult )
    {
        if ( !( jsApiResult instanceof Map<?, ?> map ) )
        {
            return null;
        }

        if ( !map.containsKey( "type" ) )
        {
            return null;
        }

        final ContentTypeName type = ContentTypeName.from( (String) map.get( "type" ) );

        final Content.Builder<?> builder = type.getApplicationKey().getName().equals( "media" ) ? Media.create() : Content.create();
        builder.type( type );

        if ( map.containsKey( "_id" ) )
        {
            builder.id( ContentId.from( (String) map.get( "_id" ) ) );
        }
        if ( map.containsKey( "_name" ) )
        {
            builder.name( (String) map.get( "_name" ) );
        }
        if ( map.containsKey( "_path" ) )
        {
            builder.path( ContentPath.from( (String) map.get( "_path" ) ) );
        }
        if ( map.containsKey( "displayName" ) )
        {
            builder.displayName( (String) map.get( "displayName" ) );
        }
        if ( map.containsKey( "createdTime" ) )
        {
            builder.createdTime( Instant.parse( (String) map.get( "createdTime" ) ) );
        }
        if ( map.containsKey( "modifiedTime" ) )
        {
            builder.modifiedTime( Instant.parse( (String) map.get( "modifiedTime" ) ) );
        }
        if ( map.containsKey( "owner" ) )
        {
            builder.owner( PrincipalKey.from( (String) map.get( "owner" ) ) );
        }
        if ( map.containsKey( "creator" ) )
        {
            builder.creator( PrincipalKey.from( (String) map.get( "creator" ) ) );
        }
        if ( map.containsKey( "modifier" ) )
        {
            builder.modifier( PrincipalKey.from( (String) map.get( "modifier" ) ) );
        }
        if ( map.containsKey( "language" ) )
        {
            builder.language( Locale.forLanguageTag( (String) map.get( "language" ) ) );
        }

        if ( map.containsKey( "data" ) )
        {
            PropertyTree data = PropertyTree.fromMap( (Map<String, Object>) map.get( "data" ) );
            builder.data( data );
        }

        if ( map.containsKey( "attachments" ) )
        {
            Map<String, Object> attachmentAsMap = (Map<String, Object>) map.get( "attachments" );

            final Attachments.Builder attachments = Attachments.create();

            attachmentAsMap.values().forEach( attachment -> {
                if ( attachment instanceof Map )
                {
                    Map<String, Object> attachmentMap = (Map<String, Object>) attachment;

                    attachments.add( Attachment.create().label( (String) attachmentMap.get( "label" ) ).sha512(
                        (String) attachmentMap.get( "sha512" ) ).size( ( (Integer) attachmentMap.get( "size" ) ) ).name(
                        (String) attachmentMap.get( "name" ) ).mimeType( (String) attachmentMap.get( "mimeType" ) ).build() );
                }
            } );

            builder.attachments( attachments.build() );
        }

        return builder.build();
    }

}
