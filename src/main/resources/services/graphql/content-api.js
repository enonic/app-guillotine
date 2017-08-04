var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');
var graphQlConnectionLib = require('/lib/graphql-connection');
var genericTypesLib = require('./generic-types');
var typesApiLib = require('./types-api');

var getChildrenResultType = graphQlLib.createObjectType({
    name: 'GetChildrenResult',
    description: 'Get children result.',
    fields: {
        total: {
            type: graphQlLib.GraphQLInt
        },
        count: {
            type: graphQlLib.GraphQLInt
        },
        hits: {
            type: graphQlLib.list(genericTypesLib.contentType)
        }
    }
});

var getPermissionsResultType = graphQlLib.createObjectType({
    name: 'Permissions',
    description: 'Permissions.',
    fields: {
        inheritsPermissions: {
            type: graphQlLib.GraphQLBoolean
        },
        permissions: {
            type: graphQlLib.list(genericTypesLib.accessControlEntryType)
        }
    }
});

exports.contentApiType = graphQlLib.createObjectType({
    name: 'ContentApi',
    description: 'Content API',
    fields: {
        get: {
            type: genericTypesLib.contentType,
            args: {
                key: graphQlLib.GraphQLID
            },
            resolve: function (env) {
                return contentLib.get({
                    key: getKey(env)
                });
            }
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
                return contentLib.getChildren({
                    key: getKey(env),
                    start: env.args.offset,
                    count: env.args.first,
                    sort: env.args.sort
                }).hits;
            }
        },
        getChildrenAsConnection: {
            type: genericTypesLib.contentConnectionType,
            args: {
                key: graphQlLib.GraphQLID,
                after: graphQlLib.GraphQLString,
                first: graphQlLib.GraphQLInt,
                sort: graphQlLib.GraphQLString
            },
            resolve: function (env) {
                var start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;
                var getChildrenResult = contentLib.getChildren({
                    key: getKey(env),
                    start: start,
                    count: env.args.first,
                    sort: env.args.sort
                });
                return {
                    total: getChildrenResult.total,
                    start: start,
                    hits: getChildrenResult.hits
                };
            }
        },
        getPermissions: {
            type: getPermissionsResultType,
            args: {
                key: graphQlLib.GraphQLID
            },
            resolve: function (env) {
                return contentLib.getPermissions({
                    key: getKey(env)
                });
            }
        },
        getSite: {
            type: graphQlLib.reference('Site'),
            args: {
                key: graphQlLib.GraphQLID
            },
            resolve: function (env) {
                return contentLib.getSite({
                    key: getKey(env)
                });
            }
        },
        getSiteConfig: {
            type: graphQlLib.GraphQLString,
            args: {
                key: graphQlLib.GraphQLID,
                applicationKey: graphQlLib.nonNull(graphQlLib.GraphQLID)
            },
            resolve: function (env) {
                var config = contentLib.getSiteConfig({
                    key: getKey(env),
                    applicationKey: env.args.applicationKey
                });
                return config && JSON.stringify(config);
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
                    query: env.args.query,
                    start: env.args.offset,
                    count: env.args.first,
                    sort: env.args.sort
                }).hits;
            }
        },
        queryAsConnection: {
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
                    query: env.args.query,
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
            type: typesApiLib.typesApiType,
            resolve: function () {
               return {};
            }
        } 
    }
});

function getKey(env) {
    var key = env.args.key;
    if (!key) {
        var content = portalLib.getContent();
        if (content) {
            key = content._id
        } else {
            throw 'Missing field argument key';
        }
    }
    return key;
}