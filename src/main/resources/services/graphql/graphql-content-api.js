var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');
var graphQlContentObjectTypesLib = require('./graphql-content-object-types');

var getChildrenResultType = graphQlLib.createObjectType({
    name: 'GetChildrenResult',
    description: 'Get children result.',
    fields: {
        total: {
            type: graphQlLib.GraphQLInt,
            resolve: function (env) {
                return env.source.total;
            }
        },
        count: {
            type: graphQlLib.GraphQLInt,
            resolve: function (env) {
                return env.source.count;
            }
        },
        hits: {
            type: graphQlLib.list(graphQlContentObjectTypesLib.contentType),
            resolve: function (env) {
                return env.source.hits;
            }
        }
    }
});
var permisionType = graphQlLib.createEnumType({
    name: 'Permission',
    description: 'Permission.',
    values: {
        'READ': 'READ',
        'CREATE': 'CREATE',
        'MODIFY': 'MODIFY',
        'DELETE': 'DELETE',
        'PUBLISH': 'PUBLISH',
        'READ_PERMISSIONS': 'READ_PERMISSIONS',
        'WRITE_PERMISSIONS': 'WRITE_PERMISSIONS'
    }
});

var accessControlEntryType = graphQlLib.createObjectType({
    name: 'AccessControlEntry',
    description: 'Access control entry.',
    fields: {
        principal: {
            type: graphQlLib.reference('PrincipalKey'),
            resolve: function (env) {
                return env.source.principal;
            }
        },
        allow: {
            type: graphQlLib.list(permisionType),
            resolve: function (env) {
                return env.source.allow;
            }
        },
        deny: {
            type: graphQlLib.list(permisionType),
            resolve: function (env) {
                return env.source.deny;
            }
        }
    }
});
var getPermissionsResultType = graphQlLib.createObjectType({
    name: 'GetPermissionsResult',
    description: 'Get permissions result.',
    fields: {
        inheritsPermissions: {
            type: graphQlLib.GraphQLBoolean,
            resolve: function (env) {
                return env.source.inheritsPermissions;
            }
        },
        permissions: {
            type: graphQlLib.list(accessControlEntryType),
            resolve: function (env) {
                return env.source.permissions;
            }
        }
    }
});

exports.contentApiType = graphQlLib.createObjectType({
    name: 'Contents',
    description: 'Contents API',
    fields: {
        get: {
            type: graphQlContentObjectTypesLib.contentType,
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
            type: getChildrenResultType,
            args: {
                key: graphQlLib.GraphQLID,
                start: graphQlLib.GraphQLInt,
                count: graphQlLib.GraphQLInt,
                sort: graphQlLib.GraphQLString
            },
            resolve: function (env) {
                return contentLib.getChildren({
                    key: getKey(env),
                    start: env.args.start,
                    count: env.args.count,
                    sort: env.args.sort
                });
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