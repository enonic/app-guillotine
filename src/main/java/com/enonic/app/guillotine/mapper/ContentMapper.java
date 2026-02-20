package com.enonic.app.guillotine.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentInheritType;
import com.enonic.xp.content.ContentPublishInfo;
import com.enonic.xp.content.Mixin;
import com.enonic.xp.content.Mixins;
import com.enonic.xp.content.WorkflowInfo;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.page.Page;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;
import com.enonic.xp.sortvalues.SortValuesProperty;

public final class ContentMapper
    implements MapSerializable
{
    private final Content content;

    private final SortValuesProperty sort;

    private final Float score;

    public ContentMapper( final Content content )
    {
        this( content, null, null );
    }

    public ContentMapper( final Content content, final SortValuesProperty sort, final Float score )
    {
        this.content = content;
        this.sort = sort;
        this.score = score;
    }

    private void serialize( final MapGenerator gen, final Content content )
    {
        gen.value( "_id", content.getId() );
        gen.value( "_name", content.getName() );
        gen.value( "_path", content.getPath() );
        gen.value( "_score", this.score );
        gen.value( "creator", content.getCreator() );
        gen.value( "modifier", content.getModifier() );
        gen.value( "createdTime", content.getCreatedTime() );
        gen.value( "modifiedTime", content.getModifiedTime() );
        gen.value( "owner", content.getOwner() );
        gen.value( "type", content.getType() );
        gen.value( "displayName", content.getDisplayName() );
        gen.value( "language", content.getLanguage() );
        gen.value( "valid", content.isValid() );
        gen.value( "originProject", content.getOriginProject() );
        gen.value( "variantOf", content.getVariantOf() );
        if ( content.getChildOrder() != null )
        {
            gen.value( "childOrder", content.getChildOrder().toString() );
        }
        if ( sort != null && sort.getValues() != null )
        {
            gen.array( "_sort" );
            for ( final Object sortValue : sort.getValues() )
            {
                gen.value( sortValue );
            }
            gen.end();
        }

        serializeData( gen, content.getData() );
        serializeMixins( gen, content.getMixins() );
        serializePage( gen, content.getPage() );
        serializeAttachments( gen, content );
        serializePublishInfo( gen, content.getPublishInfo() );
        serializeWorkflowInfo( gen, content.getWorkflowInfo() );
        serializeInherit( gen, content.getInherit() );
    }

    private void serializeData( final MapGenerator gen, final PropertyTree value )
    {
        gen.map( "data" );
        new PropertyTreeMapper( value ).serialize( gen );
        gen.end();
    }

    private void serializePublishInfo( final MapGenerator gen, final ContentPublishInfo info )
    {
        gen.map( "publish" );
        if ( info != null )
        {
            gen.value( "from", info.from() );
            gen.value( "to", info.to() );
            gen.value( "first", info.first() );
            gen.value( "time", info.time() );
        }
        gen.end();
    }

    private void serializeWorkflowInfo( final MapGenerator gen, final WorkflowInfo info )
    {
        gen.map( "workflow" );
        if ( info != null )
        {
            gen.value( "state", info.getState().toString() );
        }
        gen.end();
    }

    private void serializeMixins( final MapGenerator gen, final Mixins mixins )
    {
        gen.map( "x" );

        mixins.stream()
            .collect( Collectors.groupingBy( this::getApplicationPrefix, LinkedHashMap::new, Collectors.toList() ) )
            .forEach( ( appPrefix, appMixins ) -> {
                gen.map( appPrefix );
                for ( final Mixin mixin : appMixins )
                {
                    gen.map( mixin.getName().getLocalName() );
                    new PropertyTreeMapper( mixin.getData() ).serialize( gen );
                    gen.end();
                }
                gen.end();
            } );
        gen.end();
    }

    private void serializePage( final MapGenerator gen, final Page value )
    {
        if ( value != null )
        {
            new PageMapper( value ).serialize( gen );
        }
        else
        {
            gen.map( "page" );
            gen.end();
        }
    }

    private void serializeAttachments( final MapGenerator gen, final Content value )
    {
        gen.map( "attachments" );
        new AttachmentsMapper( value ).serialize( gen );
        gen.end();
    }

    private void serializeInherit( final MapGenerator gen, final Set<ContentInheritType> value )
    {
        if ( !value.isEmpty() )
        {
            gen.array( "inherit" );
            value.forEach( gen::value );
            gen.end();
        }
    }

    private String getApplicationPrefix( final Mixin mixin )
    {
        return mixin.getName().getApplicationKey().toString().replace( '.', '-' );
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        serialize( gen, this.content );
    }
}

