package com.enonic.app.guillotine.graphql.fetchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;

import com.enonic.app.guillotine.graphql.Constants;
import com.enonic.app.guillotine.graphql.GuillotineSerializer;
import com.enonic.app.guillotine.graphql.helper.GraphQLTypeUnwrapper;
import com.enonic.app.guillotine.graphql.helper.GuillotineLocalContextHelper;
import com.enonic.app.guillotine.graphql.helper.SchemaAwareContentExtractor;
import com.enonic.app.guillotine.graphql.transformer.ContextualFieldResolver;
import com.enonic.app.guillotine.mapper.DataFetchingEnvironmentMapper;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.script.ScriptValue;

public class DynamicDataFetcher
    implements DataFetcher<Object>
{
    private final ScriptValue resolveFunction;

    private final ApplicationKey applicationKey;

    private final SchemaAwareContentExtractor contentExtractor;

    public DynamicDataFetcher( final ContentService contentService, final ContextualFieldResolver fieldResolver )
    {
        this.contentExtractor = new SchemaAwareContentExtractor( contentService );

        this.resolveFunction = fieldResolver.getResolveFunction();
        this.applicationKey = fieldResolver.getApplicationKey();
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        final PortalRequest portalRequest = PortalRequestAccessor.get();
        final ApplicationKey oldApplicationKey = portalRequest.getApplicationKey();

        portalRequest.setApplicationKey( applicationKey );

        PortalRequestAccessor.set( portalRequest );
        try
        {
            final Object rawValue =
                GuillotineSerializer.serialize( resolveFunction.call( new DataFetchingEnvironmentMapper( environment ) ) );

            if ( isContentType( environment.getFieldType() ) )
            {
                final List<Content> contents = contentExtractor.extract( rawValue, environment );

                final Map<String, Content> contentsWithAttachments = new HashMap<>();
                contents.forEach( content -> {
                    if ( !content.getAttachments().isEmpty() )
                    {
                        contentsWithAttachments.put( content.getId().toString(), content );
                    }
                } );

                final Map<String, Object> newLocalContext = GuillotineLocalContextHelper.newLocalContext( environment );
                if ( !contentsWithAttachments.isEmpty() )
                {
                    newLocalContext.put( Constants.CONTENTS_WITH_ATTACHMENTS_FIELD, contentsWithAttachments );
                }

                if ( rawValue instanceof DataFetcherResult<?> dataFetcherResult )
                {
                    return dataFetcherResult.transform( result -> result.localContext( newLocalContext ) );
                }
                else
                {
                    return DataFetcherResult.newResult().data( rawValue ).localContext( newLocalContext ).build();
                }
            }
            else
            {
                return rawValue;
            }
        }
        finally
        {
            portalRequest.setApplicationKey( oldApplicationKey );
            PortalRequestAccessor.set( portalRequest );
        }
    }

    private boolean isContentType( final GraphQLType type )
    {
        return GraphQLTypeUnwrapper.unwrapType( type ).getName().endsWith( "Content" );
    }
}
