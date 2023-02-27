const contentLib = require('/lib/xp/content');
const contextLib = require('/lib/xp/context');
const portalLib = require('/lib/xp/portal');
const graphQlConnectionLib = require('/lib/graphql-connection');
const graphQlLib = require('/lib/guillotine/graphql');
const multiRepoLib = require('/lib/guillotine/multiRepo');
const contentTypesLib = require('/lib/guillotine/dynamic/content-types');
const securityLib = require('/lib/guillotine/util/security');
const validationLib = require('/lib/guillotine/util/validation');
const wildcardLib = require('/lib/guillotine/util/wildcard');
const factoryUtil = require('/lib/guillotine/util/factory');
const getSiteLib = require('/lib/guillotine/util/site-helper');
const nodeTransformer = require('/lib/guillotine/util/node-transformer');
const contextUtilLib = require('/lib/guillotine/util/context-util');

function createContentApiType(context) {
    return graphQlLib.createObjectType(context, {
        name: context.uniqueName('HeadlessCms'),
        description: 'Headless CMS',
        fields: {
            get: {
                type: context.types.contentType,
                args: {
                    key: graphQlLib.GraphQLID
                },
                resolve: (env) => {
                    const node = contextUtilLib.executeInQueryContext(env.source, () => getContent(env, context, false));
                    addSystemPropertiesToNode(node, env.source);
                    return node;
                }
            },
            getChildren: {
                type: graphQlLib.list(context.types.contentType),
                args: {
                    key: graphQlLib.GraphQLID,
                    offset: graphQlLib.GraphQLInt,
                    first: graphQlLib.GraphQLInt,
                    sort: graphQlLib.GraphQLString
                },
                resolve: function (env) {
                    validationLib.validateArguments(env.args);
                    return contextUtilLib.executeInQueryContext(env.source, () => {
                        const parent = getContent(env, context, true);

                        if (parent) {
                            const hits = contentLib.getChildren({
                                key: parent._id,
                                start: env.args.offset,
                                count: env.args.first,
                                sort: env.args.sort
                            }).hits;
                            hits.forEach(node => addSystemPropertiesToNode(node, env.source));
                            return hits;
                        } else {
                            return [];
                        }
                    });
                }
            },
            getChildrenConnection: {
                type: context.types.contentConnectionType,
                args: {
                    key: graphQlLib.GraphQLID,
                    after: graphQlLib.GraphQLString,
                    first: graphQlLib.GraphQLInt,
                    sort: graphQlLib.GraphQLString
                },
                resolve: function (env) {
                    validationLib.validateArguments(env.args);
                    return contextUtilLib.executeInQueryContext(env.source, () => {
                        const parent = getContent(env, context, true);
                        if (parent) {
                            let start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;
                            let getChildrenResult = contentLib.getChildren({
                                key: parent._id,
                                start: start,
                                count: env.args.first,
                                sort: env.args.sort
                            });

                            let hits = getChildrenResult.hits;
                            hits.forEach(node => addSystemPropertiesToNode(node, env.source));

                            return {
                                total: getChildrenResult.total,
                                start: start,
                                hits: hits
                            };
                        } else {
                            let start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;
                            return {
                                total: 0,
                                start: start,
                                hits: 0
                            };
                        }
                    });
                }
            },
            getPermissions: {
                type: context.types.permissionsType,
                args: {
                    key: graphQlLib.GraphQLID
                },
                resolve: function (env) {
                    return contextUtilLib.executeInQueryContext(env.source, () => {
                        const content = getContent(env, context, false);
                        if (content) {
                            return contentLib.getPermissions({
                                key: content._id
                            });
                        } else {
                            return null;
                        }
                    });
                }
            },
            query: {
                type: graphQlLib.list(context.types.contentType),
                args: {
                    query: context.types.queryDslInputType,
                    offset: graphQlLib.GraphQLInt,
                    first: graphQlLib.GraphQLInt,
                    sort: graphQlLib.list(context.types.sortDslInputType),
                },
                resolve: function (env) {
                    validationLib.validateDslQuery(env);
                    const hits = contextUtilLib.executeInQueryContext(env.source,
                        () => contentLib.query(createQueryDslParams(env, env.args.offset, context)).hits);
                    hits.forEach(node => addSystemPropertiesToNode(node, env.source));
                    return hits;
                }
            },
            queryConnection: {
                type: context.types.queryDslContentConnectionType,
                args: {
                    query: graphQlLib.nonNull(context.types.queryDslInputType),
                    after: graphQlLib.GraphQLString,
                    first: graphQlLib.GraphQLInt,
                    aggregations: graphQlLib.list(context.types.aggregationInputType),
                    highlight: context.types.highlightInputType,
                    sort: graphQlLib.list(context.types.sortDslInputType),
                },
                resolve: function (env) {
                    validationLib.validateDslQuery(env);

                    const start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;

                    const queryResult = contextUtilLib.executeInQueryContext(env.source,
                        () => contentLib.query(createQueryDslParams(env, start, context)));

                    const hits = queryResult.hits;
                    hits.forEach(node => addSystemPropertiesToNode(node, env.source));
                    return {
                        total: queryResult.total,
                        start: start,
                        hits: hits,
                        aggregationsAsJson: queryResult.aggregations,
                        highlightAsJson: queryResult.highlight,
                    };
                }
            },
            multiRepoQuery: {
                type: context.types.multiRepoQueryContentConnectionType,
                args: {
                    query: graphQlLib.nonNull(context.types.queryDslInputType),
                    searchTargets: graphQlLib.nonNull(graphQlLib.list(context.types.searchTargetInputType)),
                    explain: graphQlLib.GraphQLBoolean,
                    after: graphQlLib.GraphQLString,
                    first: graphQlLib.GraphQLInt,
                    aggregations: graphQlLib.list(context.types.aggregationInputType),
                    highlight: context.types.highlightInputType,
                    sort: graphQlLib.list(context.types.sortDslInputType),
                },
                resolve: function (env) {
                    validationLib.validateDslQuery(env);

                    const start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;

                    const queryParams = {
                        start: start,
                        count: env.args.first,
                        query: securityLib.adaptDslQuery(factoryUtil.createDslQuery(env.args.query), context),
                        searchTargets: env.args.searchTargets,
                        explain: env.args.explain,
                    };

                    if (env.args.sort) {
                        queryParams.sort = factoryUtil.createDslSort(env.args.sort);
                    }
                    if (env.args.aggregations) {
                        const aggregations = {};
                        env.args.aggregations.forEach(aggregation => {
                            factoryUtil.createAggregation(aggregations, aggregation);
                        });
                        queryParams.aggregations = aggregations;
                    }
                    if (env.args.highlight) {
                        queryParams.highlight = factoryUtil.createHighlight(env.args.highlight);
                    }

                    const queryResult = multiRepoLib.query(queryParams);
                    const hits = queryResult.hits;
                    hits.forEach(node => addSystemPropertiesToNode(node, env.source));
                    return {
                        total: queryResult.total,
                        start: start,
                        hits: hits,
                        aggregationsAsJson: queryResult.aggregations,
                    };
                }
            },
            getType: {
                type: context.types.contentTypeType,
                args: {
                    name: graphQlLib.nonNull(graphQlLib.GraphQLString)
                },
                resolve: function (env) {
                    return contentTypesLib.getAllowedContentType(context, env.args.name);
                }
            },
            getTypes: {
                type: graphQlLib.list(context.types.contentTypeType),
                resolve: function (env) {
                    return contentTypesLib.getAllowedContentTypes(context);
                }
            }
        }
    });
}

function getContentByKey(key, context, returnRootContent) {
    const content = contentLib.get({
        key: key
    });
    if (content && content._path === '/' && returnRootContent === false) {
        return null;
    }
    return securityLib.filterForbiddenContent(content, context);
}

function getContent(env, context, returnRootContent) {
    if (env.args.key) {
        const site = context.isGlobalMode() && env.context && env.context['__siteKey']
                     ? getSiteLib.getSiteFromQueryContext(env.context)
                     : portalLib.getSite();

        const key = site ? wildcardLib.replaceSitePath(env.args.key, site._path) : env.args.key;

        return getContentByKey(key, context, returnRootContent);
    } else {
        if (context.isGlobalMode()) {
            if (env.context && env.context['__siteKey']) {
                return getContentByKey(env.context['__siteKey'], context, returnRootContent);
            } else if (returnRootContent === true) {
                return contextLib.run({}, () => contentLib.get({key: '/'}));
            }
        }
        return portalLib.getContent();
    }
}

function addSystemPropertiesToNode(node, searchTarget) {
    if (node) {
        node['__nodeId'] = node._id;
        node['__searchTarget'] = searchTarget;
        if (node.data) {
            nodeTransformer.addRecursiveNodeId(node.data, node._id, searchTarget);
        }
        if (node.attachments) {
            Object.keys(node.attachments).forEach(attachment => {
                nodeTransformer.addRecursiveNodeId(node.attachments[attachment], node._id, searchTarget)
            });
        }
    }
}

function createQueryDslParams(env, start, context) {
    const queryParams = {
        start: start,
        count: env.args.first,
    };
    if (env.args.query) {
        queryParams.query = securityLib.adaptDslQuery(factoryUtil.createDslQuery(env.args.query), context);
    }
    if (env.args.sort) {
        queryParams.sort = factoryUtil.createDslSort(env.args.sort);
    }
    if (env.args.aggregations) {
        const aggregations = {};
        env.args.aggregations.forEach(aggregation => {
            factoryUtil.createAggregation(aggregations, aggregation);
        });
        queryParams.aggregations = aggregations;
    }
    if (env.args.highlight) {
        queryParams.highlight = factoryUtil.createHighlight(env.args.highlight);
    }
    return queryParams;
}

exports.createContentApiType = createContentApiType;
