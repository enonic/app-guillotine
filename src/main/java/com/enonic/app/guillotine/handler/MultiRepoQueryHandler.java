package com.enonic.app.guillotine.handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.enonic.app.guillotine.handler.params.QueryAggregationParams;
import com.enonic.app.guillotine.handler.params.QueryHighlightParams;
import com.enonic.app.guillotine.mapper.MultiRepoQueryResultMapper;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.lib.common.JsonToPropertyTreeTranslator;
import com.enonic.xp.node.FindNodesByMultiRepoQueryResult;
import com.enonic.xp.node.MultiRepoNodeQuery;
import com.enonic.xp.node.NodeQuery;
import com.enonic.xp.node.NodeService;
import com.enonic.xp.node.SearchTarget;
import com.enonic.xp.node.SearchTargets;
import com.enonic.xp.query.aggregation.AggregationQueries;
import com.enonic.xp.query.expr.ConstraintExpr;
import com.enonic.xp.query.expr.DslExpr;
import com.enonic.xp.query.expr.DslOrderExpr;
import com.enonic.xp.query.expr.OrderExpr;
import com.enonic.xp.query.expr.QueryExpr;
import com.enonic.xp.query.highlight.HighlightQuery;
import com.enonic.xp.query.parser.QueryParser;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.script.ScriptValue;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import com.enonic.xp.util.JsonHelper;

public class MultiRepoQueryHandler
    implements ScriptBean
{
    private Supplier<NodeService> nodeServiceSupplier;

    private Integer start;

    private Integer count;

    private ScriptValue query;

    private ScriptValue sort;

    private Map<String, Object> aggregations;

    private Map<String, Object> highlight;

    private ScriptValue searchTargets;

    private boolean explain;

    @Override
    public void initialize( final BeanContext context )
    {
        this.nodeServiceSupplier = context.getService( NodeService.class );
    }

    public Object execute()
    {
        MultiRepoNodeQuery query = new MultiRepoNodeQuery( createSearchTargets(), createNodeQuery() );

        FindNodesByMultiRepoQueryResult queryResult = nodeServiceSupplier.get().findByQuery( query );

        return new MultiRepoQueryResultMapper( queryResult );
    }

    private SearchTargets createSearchTargets()
    {
        final SearchTargets.Builder searchTargetBuilder = SearchTargets.create();

        if ( searchTargets.isArray() )
        {
            searchTargets.getArray().forEach( searchTarget -> {
                PropertyTree propertyTree = JsonToPropertyTreeTranslator.translate( JsonHelper.from( searchTarget.getMap() ) );

                searchTargetBuilder.add( SearchTarget.create().
                    repositoryId(RepositoryId.from( "com.enonic.cms." + propertyTree.getString( "project" ) ) ).
                    branch(Branch.from( propertyTree.getString( "branch" ) ) ).
                    principalKeys(ContextAccessor.current().getAuthInfo().getPrincipals() ).
                    build()
                );
            } );
        }

        return searchTargetBuilder.build();
    }

    private NodeQuery createNodeQuery()
    {
        final int start = Objects.requireNonNullElse( this.start, 0 );
        final int count = Objects.requireNonNullElse( this.count, 10 );

        final QueryExpr queryExpr = QueryExpr.from( buildConstraintExpr(), buildOrderExpr() );

        final AggregationQueries aggregations = new QueryAggregationParams().getAggregations( this.aggregations );

        final HighlightQuery highlight = new QueryHighlightParams().getHighlightQuery( this.highlight );

        return NodeQuery.create().from( start ).size( count ).addAggregationQueries( aggregations ).highlight( highlight ).query(
            queryExpr ).explain( this.explain ).build();
    }

    private ConstraintExpr buildConstraintExpr()
    {
        if ( query == null )
        {
            return QueryParser.parseCostraintExpression( "" );
        }
        else if ( query.isObject() )
        {
            return DslExpr.from( JsonToPropertyTreeTranslator.translate( JsonHelper.from( query.getMap() ) ) );
        }

        throw new IllegalArgumentException( "query must be a String or JSON object" );
    }

    private List<OrderExpr> buildOrderExpr()
    {
        if ( sort == null )
        {
            return List.of();
        }
        else if ( sort.isObject() )
        {

            return List.of( DslOrderExpr.from( JsonToPropertyTreeTranslator.translate( JsonHelper.from( sort.getMap() ) ) ) );

        }
        else if ( sort.isArray() )
        {
            return sort.getArray().stream().map(
                expr -> DslOrderExpr.from( JsonToPropertyTreeTranslator.translate( JsonHelper.from( expr.getMap() ) ) ) ).collect(
                Collectors.toList() );
        }
        else
        {
            throw new IllegalArgumentException( "sort must be a String, JSON object or array of JSON objects" );
        }
    }

    public void setStart( final Integer start )
    {
        this.start = start;
    }

    public void setCount( final Integer count )
    {
        this.count = count;
    }

    public void setQuery( final ScriptValue query )
    {
        this.query = query;
    }

    public void setSort( final ScriptValue sort )
    {
        this.sort = sort;
    }

    public void setAggregations( final ScriptValue aggregations )
    {
        this.aggregations = aggregations != null ? aggregations.getMap() : null;
    }

    public void setHighlight( final ScriptValue highlight )
    {
        this.highlight = highlight != null ? highlight.getMap() : null;
    }

    public void setExplain( final boolean explain )
    {
        this.explain = explain;
    }

    public void setSearchTargets( final ScriptValue searchTargets )
    {
        this.searchTargets = searchTargets;
    }
}
