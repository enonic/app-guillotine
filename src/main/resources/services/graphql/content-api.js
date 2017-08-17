var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');
var graphQlConnectionLib = require('/lib/graphql-connection');
var namingLib = require('/lib/headless-cms/naming');
var genericTypesLib = require('./generic-types');
var typesApiLib = require('./types-api');

exports.createContentApiType = function () {
    return graphQlLib.createObjectType({
        name: namingLib.uniqueName('ContentApi'),
        description: 'Content API',
        fields: {
            get: {
                type: genericTypesLib.contentType,
                args: {
                    key: graphQlLib.GraphQLID
                },
                resolve: getContent
            },
            getChildren: {
                type: graphQlLib.list(genericTypesLib.contentType),
                args: {
                    key: graphQlLib.GraphQLID,
                    offset: graphQlLib.GraphQLInt,
                    first: graphQlLib.GraphQLInt,
                    sort: graphQlLib.GraphQLString
                },
                resolve: function (env) {
                    var parent = getContent(env);
                    if (parent) {
                        return contentLib.getChildren({
                            key: parent._id,
                            start: env.args.offset,
                            count: env.args.first,
                            sort: env.args.sort
                        }).hits;
                    } else {
                        return [];
                    }
                }
            },
            getChildrenConnection: {
                type: genericTypesLib.contentConnectionType,
                args: {
                    key: graphQlLib.GraphQLID,
                    after: graphQlLib.GraphQLString,
                    first: graphQlLib.GraphQLInt,
                    sort: graphQlLib.GraphQLString
                },
                resolve: function (env) {
                    var parent = getContent(env);
                    if (parent) {
                        var start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;
                        var getChildrenResult = contentLib.getChildren({
                            key: parent._id,
                            start: start,
                            count: env.args.first,
                            sort: env.args.sort
                        });
                        return {
                            total: getChildrenResult.total,
                            start: start,
                            hits: getChildrenResult.hits
                        };
                    } else {
                        var start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;
                        return {
                            total: 0,
                            start: start,
                            hits: 0
                        };
                    }
                    
                }
            },
            getPermissions: {
                type: createPermissionsType(),
                args: {
                    key: graphQlLib.GraphQLID
                },
                resolve: function (env) {
                    var content = getContent(env);
                    if (content) {
                        return contentLib.getPermissions({
                            key: content._id
                        });
                    } else {
                        return null;
                    }
                }
            },
            getSite: {
                type: graphQlLib.reference('Site'),
                resolve: function (env) {
                    return portalLib.getSite();
                }
            },
            query: {
                type: graphQlLib.list(genericTypesLib.contentType),
                args: {
                    query: graphQlLib.nonNull(graphQlLib.GraphQLString),
                    offset: graphQlLib.GraphQLInt,
                    first: graphQlLib.GraphQLInt,
                    sort: graphQlLib.GraphQLString,
                },
                resolve: function (env) {
                    return contentLib.query({
                        query: adaptQuery(env.args.query),
                        start: env.args.offset,
                        count: env.args.first,
                        sort: env.args.sort
                    }).hits;
                }
            },
            queryConnection: {
                type: genericTypesLib.contentConnectionType,
                args: {
                    query: graphQlLib.nonNull(graphQlLib.GraphQLString),
                    after: graphQlLib.GraphQLString,
                    first: graphQlLib.GraphQLInt,
                    sort: graphQlLib.GraphQLString
                },
                resolve: function (env) {
                    var start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;
                    var queryResult = contentLib.query({
                        query: adaptQuery(env.args.query),
                        start: start,
                        count: env.args.first,
                        sort: env.args.sort
                    });
                    return {
                        total: queryResult.total,
                        start: start,
                        hits: queryResult.hits
                    };
                }
            },
            types: {
                type: typesApiLib.createTypesApiType(),
                resolve: function () {
                    return {};
                }
            }
        }
    });
};

function createPermissionsType() {
    return graphQlLib.createObjectType({
        name: namingLib.uniqueName('Permissions'),
        description: 'Permissions.',
        fields: {
            inheritsPermissions: {
                type: graphQlLib.GraphQLBoolean
            },
            permissions: {
                type: graphQlLib.list(genericTypesLib.accessControlEntryType)
            }
        }
    })
};

function getContent(env) {
    if (env.args.key) {
        var content = contentLib.get({
            key: env.args.key
        });
        return content && filterForbiddenContent(content);
    } else {
        return portalLib.getContent();   
    }
}

function filterForbiddenContent(content) {
    var sitePath = portalLib.getSite()._path;
    return content._path === sitePath || content._path.indexOf(sitePath + '/') === 0 ? content : null;
}

function adaptQuery(query) {
    var sitePath = portalLib.getSite()._path;
    return '(_path = "/content' + sitePath + '" OR _path LIKE "/content' + sitePath + '/*") AND (' + query + ')';
}