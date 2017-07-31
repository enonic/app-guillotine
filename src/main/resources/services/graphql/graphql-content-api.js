var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');


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
    name: 'ContentApi',
    description: 'Content API',
    fields: {
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