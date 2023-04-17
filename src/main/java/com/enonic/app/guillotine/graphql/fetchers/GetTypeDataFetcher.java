package com.enonic.app.guillotine.graphql.fetchers;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.GuillotineMapGenerator;
import com.enonic.xp.lib.content.mapper.ContentTypeMapper;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeService;
import com.enonic.xp.schema.content.GetContentTypeParams;

public class GetTypeDataFetcher
    implements DataFetcher<Object>
{
    private final GuillotineContext context;

    private final ContentTypeService contentTypeService;

    public GetTypeDataFetcher( final GuillotineContext context, final ContentTypeService contentTypeService )
    {
        this.context = context;
        this.contentTypeService = contentTypeService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        String typeName = Objects.requireNonNull( environment.getArgument( "name" ) );

        Pattern allowedContentTypesPattern = generateAllowedContentTypeRegexp();

        if ( allowedContentTypesPattern.matcher( typeName ).find() )
        {
            GetContentTypeParams params = new GetContentTypeParams();
            params.contentTypeName( typeName );
            ContentType contentType = contentTypeService.getByName( params );

            if ( contentType != null )
            {
                GuillotineMapGenerator generator = new GuillotineMapGenerator();
                new ContentTypeMapper( contentType ).serialize( generator );
                return generator.getRoot();
            }
        }
        return null;
    }

    private Pattern generateAllowedContentTypeRegexp()
    {
        String applicationKeys =
            context.getApplications().stream().map( applicationKey -> "|" + applicationKey.replaceAll( "\\\\.", "\\." ) ).collect(
                Collectors.joining() );

        return Pattern.compile( "^(?:base|media|portal" + applicationKeys + "):" );
    }
}
