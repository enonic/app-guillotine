exports.query = function (params) {
    const bean = __.newBean('com.enonic.app.guillotine.handler.MultiRepoQueryHandler');
    bean.setStart(params.start);
    bean.setCount(params.count);
    bean.setQuery(__.toScriptValue(params.query));
    bean.setSort(__.toScriptValue(params.sort));
    bean.setAggregations(__.toScriptValue(params.aggregations));
    bean.setHighlight(__.toScriptValue(params.highlight));
    bean.setSearchTargets(__.toScriptValue(params.searchTargets));
    if (params.explain === true) {
        bean.setExplain(true);
    }
    return __.toNativeObject(bean.execute());
};