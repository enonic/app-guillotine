package com.enonic.app.guillotine.graphql.commands;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.enonic.app.guillotine.graphql.helper.CastHelper;
import com.enonic.app.guillotine.mapper.GuillotineMapGenerator;
import com.enonic.app.guillotine.mapper.JsonToFilterMapper;
import com.enonic.app.guillotine.mapper.JsonToPropertyTreeTranslator;
import com.enonic.app.guillotine.mapper.QueryMapper;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.Contents;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.content.GetContentByIdsParams;
import com.enonic.xp.query.aggregation.AggregationQuery;
import com.enonic.xp.query.expr.ConstraintExpr;
import com.enonic.xp.query.expr.DslExpr;
import com.enonic.xp.query.expr.DslOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.query.expr.QueryExpr;
import com.enonic.xp.query.filter.Filters;
import com.enonic.xp.query.highlight.HighlightQuery;
import com.enonic.xp.query.parser.QueryParser;
import com.enonic.xp.schema.content.ContentTypeNames;
import com.enonic.xp.util.JsonHelper;

public class FindContentsCommand
{
    private final FindContentsParams params;

    private final ContentService contentService;

    public FindContentsCommand( final FindContentsParams params, final ContentService contentService )
    {
        this.params = params;
        this.contentService = contentService;
    }

    public Map<String, Object> execute()
    {
        FindContentIdsByQueryResult findQueryResult = doExecute();

        Contents contents = !findQueryResult.getContentIds().isEmpty() ? this.contentService.getByIds(
            new GetContentByIdsParams( findQueryResult.getContentIds() ) ) : Contents.empty();

        return map( contents, findQueryResult );
    }

    public Object executeFromJS()
    {
        FindContentIdsByQueryResult findQueryResult = doExecute();

        Contents contents = !findQueryResult.getContentIds().isEmpty() ? this.contentService.getByIds(
            new GetContentByIdsParams( findQueryResult.getContentIds() ) ) : Contents.empty();

        return new QueryMapper( contents, findQueryResult );
    }

    private FindContentIdsByQueryResult doExecute()
    {
        HighlightQuery highlight = new QueryHighlightParams().getHighlightQuery( params.getHighlight() );

        Filters filters = JsonToFilterMapper.create( params.getFilters() );

        Set<AggregationQuery> aggregations = new QueryAggregationParams().getAggregations( params.getAggregations() );

        ContentQuery.Builder queryBuilder = ContentQuery.create().from( params.getStart() ).size( params.getFirst() ).queryExpr(
            QueryExpr.from( buildConstraintExpr(), buildOrderExpr() ) ).highlight( highlight ).aggregationQueries(
            aggregations ).addContentTypeNames( getContentTypeNames() );

        filters.forEach( queryBuilder::queryFilter );

        return contentService.find( queryBuilder.build() );
    }

    private Map<String, Object> map( Contents contents, final FindContentIdsByQueryResult findQueryResult )
    {
        GuillotineMapGenerator generator = new GuillotineMapGenerator();
        new QueryMapper( contents, findQueryResult ).serialize( generator );
        return CastHelper.cast( generator.getRoot() );
    }

    private ContentTypeNames getContentTypeNames()
    {
        if ( params.getContentTypes() == null )
        {
            return ContentTypeNames.empty();
        }
        return ContentTypeNames.from( params.getContentTypes() );
    }

    private ConstraintExpr buildConstraintExpr()
    {
        final Object query = params.getQuery();
        if ( query == null )
        {
            return QueryParser.parseCostraintExpression( "" );
        }
        else if ( query instanceof String )
        {
            return QueryParser.parseCostraintExpression( query.toString() );
        }
        else if ( query instanceof Map )
        {
            Map<String, Object> settings = CastHelper.cast( query );
            return DslExpr.from( JsonToPropertyTreeTranslator.translate( JsonHelper.from( settings ) ) );
        }

        throw new IllegalArgumentException( "query must be a String or JSON object" );
    }

    private List<OrderExpr> buildOrderExpr()
    {
        Object sort = params.getSort();
        if ( sort == null )
        {
            return List.of();
        }
        else if ( sort instanceof String )
        {
            return QueryParser.parseOrderExpressions( sort.toString() );
        }
        else if ( sort instanceof Map )
        {
            Map<String, Object> settings = CastHelper.cast( sort );
            return List.of( DslOrderExpr.from( JsonToPropertyTreeTranslator.translate( JsonHelper.from( settings ) ) ) );
        }
        else if ( sort instanceof Collection )
        {
            return ( (Collection<?>) sort ).stream().map( expr -> {
                Map<String, Object> exprAsMap = CastHelper.cast( expr );
                return DslOrderExpr.from( JsonToPropertyTreeTranslator.translate( JsonHelper.from( exprAsMap ) ) );
            } ).collect( Collectors.toList() );
        }

        throw new IllegalArgumentException( "sort must be a String, JSON object or array of JSON objects" );
    }
}
