var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');

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