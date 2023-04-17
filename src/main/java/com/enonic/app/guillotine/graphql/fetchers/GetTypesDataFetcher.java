package com.enonic.app.guillotine.graphql.fetchers;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import com.enonic.app.guillotine.graphql.GuillotineContext;
import com.enonic.app.guillotine.graphql.GuillotineMapGenerator;
import com.enonic.xp.lib.content.mapper.ContentTypeMapper;
import com.enonic.xp.schema.content.ContentTypeService;

public class GetTypesDataFetcher
    implements DataFetcher<Object>
{
    private final GuillotineContext context;

    private final ContentTypeService contentTypeService;

    public GetTypesDataFetcher( final GuillotineContext context, final ContentTypeService contentTypeService )
    {
        this.context = context;
        this.contentTypeService = contentTypeService;
    }

    @Override
    public Object get( final DataFetchingEnvironment environment )
        throws Exception
    {
        Pattern allowedContentTypesPattern = generateAllowedContentTypeRegexp();

        return contentTypeService.getAll().stream().filter(
            contentType -> allowedContentTypesPattern.matcher( contentType.getName().toString() ).find() ).map( contentType -> {
            GuillotineMapGenerator generator = new GuillotineMapGenerator();
            new ContentTypeMapper( contentType ).serialize( generator );
            return generator.getRoot();
        } ).collect( Collectors.toList() );
    }

    private Pattern generateAllowedContentTypeRegexp()
    {
        String applicationKeys =
            context.getApplications().stream().map( applicationKey -> "|" + applicationKey.replaceAll( "\\\\.", "\\." ) ).collect(
                Collectors.joining() );

        return Pattern.compile( "^(?:base|media|portal" + applicationKeys + "):" );
    }
}
