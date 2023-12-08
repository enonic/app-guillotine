package com.enonic.app.guillotine.mapper;

import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.index.IndexConfigDocument;
import com.enonic.xp.node.Node;
import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public class NodeMapper
    implements MapSerializable
{
    private final Node node;

    private final boolean useRawValues;


    public NodeMapper( final Node node )
    {
        this.node = node;
        this.useRawValues = false;
    }

    public NodeMapper( final Node node, final boolean useRawValues )
    {
        this.node = node;
        this.useRawValues = useRawValues;
    }

    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.value( "_id", node.id() );
        gen.value( "_name", node.name() );
        gen.value( "_path", node.path() );
        gen.value( "_childOrder", node.getChildOrder().toString() );
        serializeIndexConfigDocument( gen, node.getIndexConfigDocument() );
        gen.value( "_state", node.getNodeState().toString() );
        gen.value( "_nodeType", node.getNodeType().getName() );
        gen.value( "_versionKey", node.getNodeVersionId() );
        gen.value( "_manualOrderValue", node.getManualOrderValue() );
        gen.value( "_ts", node.getTimestamp() );
        serializeData( gen, node.data() );
    }

    private void serializeData( final MapGenerator gen, final PropertyTree value )
    {
        new PropertyTreeMapper( this.useRawValues, value ).serialize( gen );
    }

    private void serializeIndexConfigDocument( final MapGenerator gen, final IndexConfigDocument value )
    {
        gen.map( "_indexConfig" );
        new IndexConfigDocMapper( value ).serialize( gen );
        gen.end();
    }

}