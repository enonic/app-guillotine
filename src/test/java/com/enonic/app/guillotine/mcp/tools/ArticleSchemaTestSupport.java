package com.enonic.app.guillotine.mcp.tools;

import java.util.ArrayList;
import java.util.List;

import com.enonic.app.guillotine.graphql.BaseGraphQLIntegrationTest;
import com.enonic.app.guillotine.mcp.shape.ProjectBranch;
import com.enonic.app.guillotine.mcp.shape.ProjectBranchResolver;
import com.enonic.xp.app.Application;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentId;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.util.GenericValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Guillotine schema with an article content type like the one of the MCP test dataset ({@code com.example.mcpdataset:article}),
 * and a resolver that always answers project {@code myproject}, branch {@code draft}.
 */
public abstract class ArticleSchemaTestSupport
    extends BaseGraphQLIntegrationTest
{
    static final String DATASET_APP = "com.example.mcpdataset";

    static final String ARTICLE = DATASET_APP + ":article";

    static final String ARTICLE_TYPE = "com_example_mcpdataset_Article";

    protected ProjectBranchResolver resolver;

    @Override
    protected void initialize()
        throws Exception
    {
        super.initialize();
        resolver = mock( ProjectBranchResolver.class );
        when( resolver.resolve( any() ) ).thenReturn( new ProjectBranch( "myproject", "draft" ) );
    }

    @Override
    protected List<Application> createApplications()
    {
        return List.of( createApplication( "myapplication" ), createApplication( DATASET_APP ) );
    }

    @Override
    protected List<ContentType> getCustomContentTypes()
    {
        final List<ContentType> result = new ArrayList<>();
        result.add( ContentType.create()
                        .superType( ContentTypeName.structured() )
                        .name( ARTICLE )
                        .title( "Article" )
                        .addFormItem( input( "title", InputTypeName.TEXT_LINE, 1, 1 ) )
                        .addFormItem( Input.create()
                                          .name( "author" )
                                          .label( "Written by" )
                                          .helpText( "The person who wrote the article." )
                                          .inputType( InputTypeName.CONTENT_SELECTOR )
                                          .occurrences( 0, 1 )
                                          .inputTypeProperty( "allowContentType", GenericValue.newList()
                                              .add( GenericValue.stringValue( "${app}:person" ) )
                                              .build() )
                                          .build() )
                        .addFormItem( input( "tags", InputTypeName.TEXT_LINE, 0, 0 ) )
                        .addFormItem( input( "related", InputTypeName.CONTENT_SELECTOR, 0, 0 ) )
                        .addFormItem( input( "cover", InputTypeName.IMAGE_SELECTOR, 0, 1 ) )
                        .addFormItem( input( "body", InputTypeName.HTML_AREA, 0, 1 ) )
                        .addFormItem( FormItemSet.create()
                                          .name( "links" )
                                          .label( "links" )
                                          .occurrences( 0, 0 )
                                          .addFormItem( input( "label", InputTypeName.TEXT_LINE, 0, 1 ) )
                                          .addFormItem( input( "target", InputTypeName.CONTENT_SELECTOR, 0, 0 ) )
                                          .build() )
                        .build() );
        return result;
    }

    protected static Content article( final String id, final String displayName )
    {
        final PropertyTree data = new PropertyTree();
        data.addString( "title", displayName );
        data.addString( "tags", "news" );
        data.addString( "tags", "mcp" );
        return Content.create()
            .id( ContentId.from( id ) )
            .name( id )
            .displayName( displayName )
            .parentPath( ContentPath.from( "/articles" ) )
            .type( ContentTypeName.from( ARTICLE ) )
            .data( data )
            .creator( PrincipalKey.ofAnonymous() )
            .build();
    }

    private static Input input( final String name, final InputTypeName type, final int min, final int max )
    {
        return Input.create().name( name ).label( name ).inputType( type ).occurrences( min, max ).build();
    }
}
