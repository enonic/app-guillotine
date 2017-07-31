var contentLib = require('/lib/xp/content');
var graphQlLib = require('/lib/graphql');

exports.contentApiType = graphQlLib.createObjectType({
    name: 'ContentApi',
    description: 'Content API',
    fields: {
        getSite: {
            type: graphQlLib.reference('Site'),
            args: {
                key: graphQlLib.nonNull(graphQlLib.GraphQLID)
            },
            resolve: function(env) {
                return contentLib.getSite({
                    key: env.args.key
                });
            }
        }
    }    
});