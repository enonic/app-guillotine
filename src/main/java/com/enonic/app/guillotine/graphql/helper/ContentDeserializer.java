package com.enonic.app.guillotine.graphql.helper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.enonic.xp.attachment.Attachment;
import com.enonic.xp.attachment.Attachments;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentInheritType;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentPublishInfo;
import com.enonic.xp.content.ExtraData;
import com.enonic.xp.content.ExtraDatas;
import com.enonic.xp.content.Media;
import com.enonic.xp.content.WorkflowCheckState;
import com.enonic.xp.content.WorkflowInfo;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.index.ChildOrder;
import com.enonic.xp.page.DescriptorKey;
import com.enonic.xp.page.Page;
import com.enonic.xp.page.PageRegions;
import com.enonic.xp.page.PageTemplateKey;
import com.enonic.xp.project.ProjectName;
import com.enonic.xp.region.Component;
import com.enonic.xp.region.FragmentComponent;
import com.enonic.xp.region.LayoutComponent;
import com.enonic.xp.region.LayoutRegions;
import com.enonic.xp.region.PartComponent;
import com.enonic.xp.region.Region;
import com.enonic.xp.region.TextComponent;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.schema.xdata.XDataName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.site.Site;

@SuppressWarnings("unchecked")
public final class ContentDeserializer
{
    @SuppressWarnings("unchecked")
    public static Content deserialize( final Object jsApiResult )
    {
        if ( !( jsApiResult instanceof Map ) )
        {
            return null;
        }

        final SafeMap contentAsSafeMap = new SafeMap( (Map<String, Object>) jsApiResult );

        final ContentTypeName type =
            contentAsSafeMap.withMapper( contentAsSafeMap.getString( "type" ), ContentTypeName::from ).orElse( null );

        if ( type == null )
        {
            return null;
        }

        final Content.Builder<?> builder =
            type.getApplicationKey().getName().equals( "media" ) ? Media.create() : ( type.isSite() ? Site.create() : Content.create() );

        builder.type( type );

        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "_id" ), ContentId::from ).ifPresent( builder::id );
        contentAsSafeMap.getString( "_name" ).ifPresent( builder::name );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "_path" ), ContentPath::from ).ifPresent( builder::path );
        contentAsSafeMap.getString( "displayName" ).ifPresent( builder::displayName );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "createdTime" ), Instant::parse ).ifPresent( builder::createdTime );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "modifiedTime" ), Instant::parse ).ifPresent( builder::modifiedTime );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "owner" ), PrincipalKey::from ).ifPresent( builder::owner );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "creator" ), PrincipalKey::from ).ifPresent( builder::creator );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "modifier" ), PrincipalKey::from ).ifPresent( builder::modifier );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "language" ), Locale::forLanguageTag ).ifPresent( builder::language );
        contentAsSafeMap.getBoolean( "valid" ).ifPresent( builder::valid );
        contentAsSafeMap.getBoolean( "hasChildren" ).ifPresent( builder::hasChildren );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "originProject" ), ProjectName::from ).ifPresent( builder::originProject );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "variantOf" ), ContentId::from ).ifPresent( builder::variantOf );
        contentAsSafeMap.withMapper( contentAsSafeMap.getString( "getChildOrder" ), ChildOrder::from ).ifPresent( builder::childOrder );

        contentAsSafeMap.withMapper( contentAsSafeMap.getMap( "data" ), PropertyTree::fromMap ).ifPresent( builder::data );

        deserializeAttachment( contentAsSafeMap, builder );
        deserializeWorkflow( contentAsSafeMap, builder );
        deserializePublishInfo( contentAsSafeMap, builder );
        deserializeInherit( contentAsSafeMap, builder );
        deserializeXData( contentAsSafeMap, builder );
        deserializePage( contentAsSafeMap, builder );

        return builder.build();
    }

    private static void deserializeAttachment( final SafeMap contentAsMap, final Content.Builder<?> builder )
    {
        contentAsMap.getMap( "attachments" ).ifPresent( attachmentsAsMap -> {
            final Attachments.Builder attachments = Attachments.create();

            attachmentsAsMap.values().forEach( attachment -> {
                if ( attachment instanceof Map )
                {
                    final SafeMap attachmentMap = new SafeMap( (Map<String, Object>) attachment );

                    final Attachment.Builder attachmentBuilder = Attachment.create();
                    attachmentMap.getString( "label" ).ifPresent( attachmentBuilder::label );
                    attachmentMap.getString( "sha512" ).ifPresent( attachmentBuilder::sha512 );
                    attachmentMap.withMapper( attachmentMap.getString( "size" ), Integer::parseInt ).ifPresent( attachmentBuilder::size );
                    attachmentMap.getString( "name" ).ifPresent( attachmentBuilder::name );
                    attachmentMap.getString( "mimeType" ).ifPresent( attachmentBuilder::mimeType );
                    attachments.add( attachmentBuilder.build() );
                }
            } );

            builder.attachments( attachments.build() );
        } );
    }

    private static void deserializeWorkflow( final SafeMap contentAsMap, final Content.Builder<?> builder )
    {
        contentAsMap.getMap( "workflow" ).ifPresent( workflowAsMap -> {
            final SafeMap workflowMap = new SafeMap( workflowAsMap );

            final WorkflowInfo.Builder workflowBuilder = WorkflowInfo.create();

            workflowMap.getString( "state" ).ifPresent( workflowBuilder::state );

            workflowMap.getMap( "checks" ).ifPresent( checksAsMap -> {
                final Map<String, WorkflowCheckState> workflowCheckStates = new HashMap<>();

                checksAsMap.forEach( ( checkName, value ) -> {
                    final String check = value.toString();
                    workflowCheckStates.put( checkName, WorkflowCheckState.valueOf( check ) );
                } );

                workflowBuilder.checks( workflowCheckStates );
            } );

            builder.workflowInfo( workflowBuilder.build() );
        } );
    }

    private static void deserializePublishInfo( final SafeMap contentAsMap, final Content.Builder<?> builder )
    {
        contentAsMap.getMap( "publish" ).ifPresent( publishAsMap -> {
            final SafeMap publishMap = new SafeMap( publishAsMap );

            final ContentPublishInfo.Builder publishBuilder = ContentPublishInfo.create();

            publishMap.withMapper( publishMap.getString( "from" ), Instant::parse ).ifPresent( publishBuilder::from );
            publishMap.withMapper( publishMap.getString( "to" ), Instant::parse ).ifPresent( publishBuilder::to );
            publishMap.withMapper( publishMap.getString( "first" ), Instant::parse ).ifPresent( publishBuilder::first );

            builder.publishInfo( publishBuilder.build() );
        } );
    }

    private static void deserializeInherit( final SafeMap contentAsMap, final Content.Builder<?> builder )
    {
        contentAsMap.getList( "inherit" ).ifPresent( inheritAsList -> {
            final Set<ContentInheritType> inherit =
                inheritAsList.stream().map( Objects::toString ).map( ContentInheritType::valueOf ).collect( Collectors.toSet() );
            builder.setInherit( inherit );
        } );
    }

    private static void deserializeXData( final SafeMap contentAsMap, final Content.Builder<?> builder )
    {
        contentAsMap.getMap( "x" ).ifPresent( extraDataAsMap -> {
            final ExtraDatas.Builder extraDataBuilder = ExtraDatas.create();

            extraDataAsMap.forEach( ( name, data ) -> {
                final XDataName xDataName = XDataName.from( name );
                final PropertyTree dataTree = PropertyTree.fromMap( (Map<String, Object>) data );
                extraDataBuilder.add( new ExtraData( xDataName, dataTree ) );
            } );

            builder.extraDatas( extraDataBuilder.build() );
        } );
    }

    private static void deserializePage( final SafeMap contentAsMap, final Content.Builder<?> builder )
    {
        final Page.Builder pageBuilder = Page.create();

        contentAsMap.getMap( "page" ).ifPresent( pageAsMap -> {
            final SafeMap pageMap = new SafeMap( pageAsMap );

            pageMap.withMapper( pageMap.getString( "template" ), PageTemplateKey::from ).ifPresent( pageBuilder::template );
            pageMap.withMapper( pageMap.getString( "descriptor" ), DescriptorKey::from ).ifPresent( pageBuilder::descriptor );
            pageMap.withMapper( pageMap.getMap( "config" ), PropertyTree::fromMap ).ifPresent( pageBuilder::config );

            final PageRegions.Builder regionsBuilder = PageRegions.create();
            deserializeRegions( pageMap.getMap( "regions" ) ).forEach( regionsBuilder::add );
            pageBuilder.regions( regionsBuilder.build() );
        } );

        contentAsMap.getMap( "fragment" ).ifPresent( componentAsMap -> pageBuilder.fragment( deserializeComponent( componentAsMap ) ) );

        builder.page( pageBuilder.build() );
    }

    private static List<Region> deserializeRegions( final Optional<Map<String, Object>> regions )
    {
        final List<Region> result = new ArrayList<>();

        regions.ifPresent( regionsAsMap -> regionsAsMap.forEach( ( regionName, regionData ) -> {
            final Region.Builder regionBuilder = Region.create();
            regionBuilder.name( regionName );

            final SafeMap regionDataMap = new SafeMap( (Map<String, Object>) regionData );

            regionDataMap.getList( "components" ).ifPresent( componentsAsList -> componentsAsList.forEach( componentData -> {
                if ( componentData instanceof Map )
                {
                    final Component component = deserializeComponent( (Map<String, Object>) componentData );
                    if ( component != null )
                    {
                        regionBuilder.add( component );
                    }
                }
            } ) );

            result.add( regionBuilder.build() );
        } ) );

        return result;
    }

    private static Component deserializeComponent( final Map<String, Object> componentData )
    {
        final SafeMap componentDataMap = new SafeMap( componentData );

        final String componentType = componentDataMap.getString( "type" ).orElse( null );

        if ( "text".equals( componentType ) )
        {
            final TextComponent.Builder textComponent = TextComponent.create();
            textComponent.text( componentDataMap.getString( "text" ).orElse( null ) );

            return textComponent.build();
        }

        if ( "part".equals( componentType ) )
        {
            final PartComponent.Builder partComponent = PartComponent.create();

            componentDataMap.withMapper( componentDataMap.getString( "descriptor" ), DescriptorKey::from ).ifPresent(
                partComponent::descriptor );
            componentDataMap.withMapper( componentDataMap.getMap( "config" ), PropertyTree::fromMap ).ifPresent( partComponent::config );

            return partComponent.build();
        }

        if ( "layout".equals( componentType ) )
        {
            final LayoutComponent.Builder layoutComponent = LayoutComponent.create();

            componentDataMap.withMapper( componentDataMap.getString( "descriptor" ), DescriptorKey::from ).ifPresent(
                layoutComponent::descriptor );
            componentDataMap.withMapper( componentDataMap.getMap( "config" ), PropertyTree::fromMap ).ifPresent( layoutComponent::config );

            final LayoutRegions.Builder layoutRegionsbuilder = LayoutRegions.create();
            deserializeRegions( componentDataMap.getMap( "regions" ) ).forEach( layoutRegionsbuilder::add );
            layoutComponent.regions( layoutRegionsbuilder.build() );

            return layoutComponent.build();
        }

        if ( "fragment".equals( componentType ) )
        {
            final FragmentComponent.Builder fragmentComponent = FragmentComponent.create();
            componentDataMap.withMapper( componentDataMap.getString( "fragment" ), ContentId::from ).ifPresent(
                fragmentComponent::fragment );
            return fragmentComponent.build();
        }

        return null;
    }

    private record SafeMap(Map<String, ?> map)
    {
        public <T> Optional<T> get( final String key, final Class<T> clazz )
        {
            final Object value = map.get( key );
            return clazz.isInstance( value ) ? Optional.of( clazz.cast( value ) ) : Optional.empty();
        }

        public Optional<Boolean> getBoolean( final String key )
        {
            return get( key, Boolean.class );
        }

        public Optional<String> getString( final String key )
        {
            return get( key, String.class );
        }

        public Optional<Map<String, Object>> getMap( String key )
        {
            return get( key, Map.class ).map( m -> (Map<String, Object>) m );
        }

        public Optional<List<Object>> getList( String key )
        {
            return get( key, List.class ).map( m -> (List<Object>) m );
        }

        public <T, R> Optional<R> withMapper( final Optional<T> opt, Function<T, R> mapper )
        {
            try
            {
                return opt.map( mapper );
            }
            catch ( Exception e )
            {
                return Optional.empty();
            }
        }
    }

}
