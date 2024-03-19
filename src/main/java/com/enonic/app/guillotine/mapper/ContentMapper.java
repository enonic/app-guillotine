package com.enonic.app.guillotine.mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentInheritType;
import com.enonic.xp.content.ContentPublishInfo;
import com.enonic.xp.content.ExtraData;
import com.enonic.xp.content.WorkflowCheckState;
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
        gen.value( "hasChildren", content.hasChildren() );
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
        serializeExtraData( gen, content.getAllExtraData() );
        serializePage( gen, content.getPage() );
        serializeAttachments( gen, content );
        serializePublishInfo( gen, content.getPublishInfo() );
        serializeWorkflowInfo( gen, content.getWorkflowInfo() );
        serializeInherit( gen, content.getInherit() );
    }

    private void serializeData( final MapGenerator gen, final PropertyTree value )
    {
        gen.map( "data" );
        gen.value( Constants.CONTENT_ID_FIELD, content.getId() );
        new PropertyTreeMapper( value, content.getId().toString() ).serialize( gen );
        gen.end();
    }

    private void serializePublishInfo( final MapGenerator gen, final ContentPublishInfo info )
    {
        gen.map( "publish" );
        if ( info != null )
        {
            gen.value( "from", info.getFrom() );
            gen.value( "to", info.getTo() );
            gen.value( "first", info.getFirst() );
        }
        gen.end();
    }

    private void serializeWorkflowInfo( final MapGenerator gen, final WorkflowInfo info )
    {
        gen.map( "workflow" );
        if ( info != null )
        {
            gen.value( "state", info.getState().toString() );
            gen.map( "checks" );
            for ( Map.Entry<String, WorkflowCheckState> e : info.getChecks().entrySet() )
            {
                gen.value( e.getKey(), e.getValue().toString() );
            }
            gen.end();
        }
        gen.end();
    }

    private void serializeExtraData( final MapGenerator gen, final Iterable<ExtraData> values )
    {
        gen.map( "x" );

        final ListMultimap<ApplicationKey, ExtraData> extradatasByModule = ArrayListMultimap.create();
        for ( ExtraData extraData : values )
        {
            extradatasByModule.put( extraData.getName().getApplicationKey(), extraData );
        }

        for ( final ApplicationKey applicationKey : extradatasByModule.keys() )
        {
            final List<ExtraData> extraDatas = extradatasByModule.get( applicationKey );
            if ( extraDatas.isEmpty() )
            {
                continue;
            }
            gen.map( extraDatas.get( 0 ).getApplicationPrefix() );
            for ( final ExtraData extraData : extraDatas )
            {
                gen.map( extraData.getName().getLocalName() );
				gen.value( Constants.CONTENT_ID_FIELD, content.getId().toString() );
                new PropertyTreeMapper( extraData.getData(), content.getId().toString() ).serialize( gen );
                gen.end();
            }
            gen.end();
        }
        gen.end();
    }

    private void serializePage( final MapGenerator gen, final Page value )
    {
        if ( value != null )
        {
            new PageMapper( value, content.getId() ).serialize( gen );
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

    @Override
    public void serialize( final MapGenerator gen )
    {
        serialize( gen, this.content );
    }
}

